/**
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2025 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */
package io.hexaglue.plugin.jpa.analysis;

import io.hexaglue.plugin.jpa.heuristics.JpaPropertyHeuristics;
import io.hexaglue.plugin.jpa.heuristics.PropertyHeuristicsDetector;
import io.hexaglue.plugin.jpa.model.PropertyModel;
import io.hexaglue.plugin.jpa.util.NamingUtils;
import io.hexaglue.plugin.jpa.validation.TypeCompatibilityValidator;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.options.PropertyMetadataHelper;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves JPA property metadata from domain properties.
 *
 * <p>This resolver analyzes domain properties and determines their JPA mapping:</p>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Uses hierarchical resolution (highest to lowest priority):</p>
 * <ol>
 *   <li><strong>Annotations</strong>: @Column, @Size, @NotNull from domain model</li>
 *   <li><strong>YAML configuration</strong>: Property-specific config in hexaglue.yaml</li>
 *   <li><strong>Heuristics</strong>: Name-based and type-based inference</li>
 *   <li><strong>Defaults</strong>: Safe JPA defaults</li>
 * </ol>
 *
 * <h2>Property Classification</h2>
 * <ul>
 *   <li><strong>Primitive</strong>: int, long, boolean → NOT NULL</li>
 *   <li><strong>String</strong>: VARCHAR(255) by default, @Lob for text fields</li>
 *   <li><strong>Enum</strong>: @Enumerated(STRING)</li>
 *   <li><strong>Temporal</strong>: LocalDate, Instant, etc.</li>
 *   <li><strong>Value Object</strong>: May need @Embedded or @Converter</li>
 * </ul>
 *
 * <h2>Heuristics Examples</h2>
 * <ul>
 *   <li>Property "email" → unique=true, length=320</li>
 *   <li>Property "description" → @Lob (large text)</li>
 *   <li>Property "code" → length=50, unique=true</li>
 *   <li>Primitive types → nullable=false</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class PropertyTypeResolver {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    private final GenerationContextSpec context;
    private final OptionsView.PluginOptionsView pluginOptions;
    private final String domainTypeQualifiedName;
    private final PropertyHeuristicsDetector heuristicsDetector;
    private final TypeCompatibilityValidator typeCompatibilityValidator;

    public PropertyTypeResolver(
            GenerationContextSpec context,
            OptionsView.PluginOptionsView pluginOptions,
            String domainTypeQualifiedName) {
        this(context, pluginOptions, domainTypeQualifiedName, new JpaPropertyHeuristics());
    }

    /**
     * Constructor with custom heuristics detector (for testing).
     *
     * @param context generation context
     * @param pluginOptions plugin options
     * @param domainTypeQualifiedName qualified name of domain type
     * @param heuristicsDetector heuristics detector
     */
    public PropertyTypeResolver(
            GenerationContextSpec context,
            OptionsView.PluginOptionsView pluginOptions,
            String domainTypeQualifiedName,
            PropertyHeuristicsDetector heuristicsDetector) {
        this.context = Objects.requireNonNull(context, "context");
        this.pluginOptions = Objects.requireNonNull(pluginOptions, "pluginOptions");
        this.domainTypeQualifiedName = Objects.requireNonNull(domainTypeQualifiedName, "domainTypeQualifiedName");
        this.heuristicsDetector = Objects.requireNonNull(heuristicsDetector, "heuristicsDetector");
        this.typeCompatibilityValidator = new TypeCompatibilityValidator(PLUGIN_ID);
    }

    /**
     * Resolves a domain property to a JPA property model.
     *
     * <p>Unwraps single-field Value Objects (like Email → String) to their inner type
     * for JPA persistence, similar to how IDs are unwrapped.</p>
     *
     * @param property domain property to analyze
     * @return JPA property model with all metadata
     */
    public PropertyModel resolve(DomainPropertyView property) {
        Objects.requireNonNull(property, "property");

        String name = property.name();
        TypeRef type = property.type();
        String columnName = NamingUtils.toColumnName(name);

        // Check if this is a single-field VO that should be unwrapped
        TypeRef unwrappedType = unwrapSingleFieldValueObject(type);

        // Validate type compatibility with JPA (after unwrapping)
        validateTypeCompatibility(unwrappedType, name);

        // Resolve metadata using hierarchical strategy
        Integer length = resolveLength(property);
        boolean nullable = resolveNullability(property);
        boolean unique = resolveUniqueness(property);
        boolean lob = isLobField(property);
        boolean enumerated = isEnumType(unwrappedType); // Use unwrapped type for enum check
        boolean temporal = isTemporalType(unwrappedType); // Use unwrapped type for temporal check
        boolean embedded = isEmbeddedValueObject(type); // Use original type for embedded check

        return new PropertyModel(
                name, unwrappedType, columnName, length, nullable, unique, lob, enumerated, temporal, embedded);
    }

    /**
     * Unwraps single-field Value Objects to their inner type.
     *
     * <p>For example: Email(String value) → String</p>
     *
     * @param type property type reference
     * @return unwrapped type if single-field VO, otherwise original type
     */
    private TypeRef unwrapSingleFieldValueObject(TypeRef type) {
        String typeName = type.render();

        // Look up in IR
        Optional<DomainTypeView> domainTypeOpt = context.model().domain().findType(typeName);

        if (domainTypeOpt.isEmpty()) {
            return type; // Not a domain type, return as-is
        }

        DomainTypeView domainType = domainTypeOpt.get();

        // Only unwrap single-field VOs (RECORDs or IDENTIFIERs with exactly 1 property)
        if ((domainType.kind() == DomainTypeKind.RECORD || domainType.kind() == DomainTypeKind.IDENTIFIER)
                && domainType.properties().size() == 1) {
            // Return the inner type
            return domainType.properties().get(0).type();
        }

        return type; // Not a single-field VO, return as-is
    }

    /**
     * Checks if a type is a multi-field Value Object that should be embedded.
     *
     * <p>A type is considered embeddable if:</p>
     * <ul>
     *   <li>It's a domain type (RECORD or IDENTIFIER)</li>
     *   <li>It has more than one field (multi-field VO)</li>
     * </ul>
     *
     * @param type property type reference
     * @return true if the type should be embedded
     */
    private boolean isEmbeddedValueObject(TypeRef type) {
        String typeName = type.render();

        // Look up in IR
        Optional<DomainTypeView> domainTypeOpt = context.model().domain().findType(typeName);

        if (domainTypeOpt.isEmpty()) {
            return false;
        }

        DomainTypeView domainType = domainTypeOpt.get();

        // Must be a RECORD or IDENTIFIER
        if (domainType.kind() != DomainTypeKind.RECORD && domainType.kind() != DomainTypeKind.IDENTIFIER) {
            return false;
        }

        // Must have MORE than one field (single-field VOs use @Converter instead)
        return domainType.properties().size() > 1;
    }

    /**
     * Resolves column length for a property.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>YAML configuration</li>
     *   <li>Heuristics (delegated to {@link PropertyHeuristicsDetector})</li>
     *   <li>Default (255 for String types)</li>
     * </ol>
     *
     * @param property domain property
     * @return column length or null if not applicable
     */
    private Integer resolveLength(DomainPropertyView property) {
        // Only applicable for String types
        if (!isStringType(property.type())) {
            return null;
        }

        // Priority 1: YAML configuration
        Optional<Integer> yamlLength = PropertyMetadataHelper.getPropertyMetadata(
                pluginOptions, domainTypeQualifiedName, property.name(), "column.length", Integer.class);
        if (yamlLength.isPresent()) {
            return yamlLength.get();
        }

        // Priority 2: Heuristics (delegated to detector)
        Optional<Integer> heuristicLength = heuristicsDetector.detectMaxLength(property);
        if (heuristicLength.isPresent()) {
            return heuristicLength.get();
        }

        // Priority 3: Default
        return 255;
    }

    /**
     * Resolves nullability for a property.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>YAML configuration</li>
     *   <li>Heuristics (delegated to {@link PropertyHeuristicsDetector})</li>
     *   <li>Default (nullable=true)</li>
     * </ol>
     *
     * @param property domain property
     * @return true if nullable, false if NOT NULL
     */
    private boolean resolveNullability(DomainPropertyView property) {
        // Priority 1: YAML configuration
        Optional<Boolean> yamlNullable = PropertyMetadataHelper.getPropertyMetadata(
                pluginOptions, domainTypeQualifiedName, property.name(), "column.nullable", Boolean.class);
        if (yamlNullable.isPresent()) {
            return yamlNullable.get();
        }

        // Priority 2: Heuristics (delegated to detector)
        Optional<Boolean> heuristicNullable = heuristicsDetector.detectNullability(property);
        if (heuristicNullable.isPresent()) {
            return heuristicNullable.get();
        }

        // Priority 3: Default (JPA default is nullable)
        return true;
    }

    /**
     * Resolves uniqueness constraint for a property.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>YAML configuration</li>
     *   <li>Heuristics (delegated to {@link PropertyHeuristicsDetector})</li>
     *   <li>Default (not unique)</li>
     * </ol>
     *
     * @param property domain property
     * @return true if unique constraint should be added
     */
    private boolean resolveUniqueness(DomainPropertyView property) {
        // Priority 1: YAML configuration
        Optional<Boolean> yamlUnique = PropertyMetadataHelper.getPropertyMetadata(
                pluginOptions, domainTypeQualifiedName, property.name(), "column.unique", Boolean.class);
        if (yamlUnique.isPresent()) {
            return yamlUnique.get();
        }

        // Priority 2: Heuristics (delegated to detector)
        Optional<Boolean> heuristicUnique = heuristicsDetector.detectUniqueness(property);
        if (heuristicUnique.isPresent()) {
            return heuristicUnique.get();
        }

        // Priority 3: Default (not unique)
        return false;
    }

    /**
     * Checks if a property should use @Lob annotation (large text/binary).
     *
     * <p>Detection is based on column type heuristics - if the heuristic suggests "TEXT",
     * then @Lob should be used.</p>
     *
     * @param property domain property
     * @return true if @Lob should be used
     */
    private boolean isLobField(DomainPropertyView property) {
        if (!isStringType(property.type())) {
            return false;
        }

        // Delegate to heuristics detector - if it suggests TEXT column type, use @Lob
        Optional<String> columnType = heuristicsDetector.detectColumnType(property);
        return columnType.isPresent() && "TEXT".equalsIgnoreCase(columnType.get());
    }

    /**
     * Checks if a type is an enum.
     *
     * @param type type reference
     * @return true if enum type
     */
    private boolean isEnumType(TypeRef type) {
        String typeName = type.render();
        Optional<DomainTypeView> domainType = context.model().domain().findType(typeName);

        return domainType.isPresent() && domainType.get().kind() == DomainTypeKind.ENUMERATION;
    }

    /**
     * Checks if a type is a temporal type (date/time).
     *
     * @param type type reference
     * @return true if temporal type
     */
    private boolean isTemporalType(TypeRef type) {
        String typeName = type.render();
        return typeName.equals("java.time.LocalDate")
                || typeName.equals("java.time.LocalDateTime")
                || typeName.equals("java.time.Instant")
                || typeName.equals("java.time.ZonedDateTime")
                || typeName.equals("java.util.Date");
    }

    /**
     * Checks if a type is a String type.
     *
     * @param type type reference
     * @return true if String
     */
    private boolean isStringType(TypeRef type) {
        String rendered = type.render();
        return "java.lang.String".equals(rendered) || "String".equals(rendered);
    }

    /**
     * Validates that a type is compatible with JPA persistence.
     *
     * <p>Delegates to {@link TypeCompatibilityValidator} for comprehensive validation.
     * Reports warnings for types that may require custom converters.</p>
     *
     * @param type type to validate
     * @param propertyName property name for diagnostic context
     */
    private void validateTypeCompatibility(TypeRef type, String propertyName) {
        String typeName = type.render();

        // Look up domain type kind if available
        Optional<DomainTypeView> domainTypeOpt = context.model().domain().findType(typeName);
        Optional<DomainTypeKind> typeKind = domainTypeOpt.map(DomainTypeView::kind);

        // Use TypeCompatibilityValidator for comprehensive validation
        Optional<Diagnostic> diagnostic = typeCompatibilityValidator.validate(type, typeKind, propertyName);

        diagnostic.ifPresent(context.diagnostics()::report);
    }
}
