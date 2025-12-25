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
package io.hexaglue.plugin.jpa.validation;

import io.hexaglue.plugin.jpa.diagnostics.JpaDiagnosticCodes;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates that types can be mapped to JPA.
 *
 * <p>This validator checks if domain types are compatible with JPA persistence.
 * Some types cannot be directly mapped and require custom converters.</p>
 *
 * <h2>JPA-Compatible Types</h2>
 * <ul>
 *   <li><strong>Primitives</strong>: int, long, boolean, etc.</li>
 *   <li><strong>Wrappers</strong>: Integer, Long, Boolean, etc.</li>
 *   <li><strong>Strings</strong>: java.lang.String</li>
 *   <li><strong>Temporal</strong>: LocalDate, LocalDateTime, Instant, Date</li>
 *   <li><strong>Large Objects</strong>: byte[], Blob, Clob</li>
 *   <li><strong>Enums</strong>: Any Java enum</li>
 *   <li><strong>Value Objects</strong>: With @Embeddable or @Converter</li>
 * </ul>
 *
 * <h2>Types Requiring Converters</h2>
 * <ul>
 *   <li>Custom Value Objects (record types)</li>
 *   <li>Java 8+ types (Optional, Duration, Period)</li>
 *   <li>Domain-specific types</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class TypeCompatibilityValidator {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    /**
     * Set of primitive types.
     */
    private static final Set<String> PRIMITIVES =
            Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char");

    /**
     * Set of wrapper types.
     */
    private static final Set<String> WRAPPERS = Set.of(
            "java.lang.Boolean",
            "Boolean",
            "java.lang.Byte",
            "Byte",
            "java.lang.Short",
            "Short",
            "java.lang.Integer",
            "Integer",
            "java.lang.Long",
            "Long",
            "java.lang.Float",
            "Float",
            "java.lang.Double",
            "Double",
            "java.lang.Character",
            "Character");

    /**
     * Set of String types.
     */
    private static final Set<String> STRING_TYPES = Set.of("java.lang.String", "String");

    /**
     * Set of temporal types supported by JPA 2.2+.
     */
    private static final Set<String> TEMPORAL_TYPES = Set.of(
            "java.time.LocalDate",
            "LocalDate",
            "java.time.LocalDateTime",
            "LocalDateTime",
            "java.time.LocalTime",
            "LocalTime",
            "java.time.Instant",
            "Instant",
            "java.time.ZonedDateTime",
            "ZonedDateTime",
            "java.time.OffsetDateTime",
            "OffsetDateTime",
            "java.util.Date",
            "Date",
            "java.sql.Date",
            "java.sql.Time",
            "java.sql.Timestamp");

    /**
     * Set of large object types.
     */
    private static final Set<String> LOB_TYPES = Set.of("byte[]", "java.sql.Blob", "Blob", "java.sql.Clob", "Clob");

    /**
     * Set of types that require converters but are common.
     */
    private static final Set<String> CONVERTER_CANDIDATES = Set.of(
            "java.util.Optional",
            "Optional",
            "java.time.Duration",
            "Duration",
            "java.time.Period",
            "Period",
            "java.util.UUID",
            "UUID");

    private final String pluginId;

    /**
     * Creates a type compatibility validator.
     *
     * @param pluginId plugin ID for diagnostic reporting
     */
    public TypeCompatibilityValidator(String pluginId) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }

    /**
     * Creates a type compatibility validator with default plugin ID.
     */
    public TypeCompatibilityValidator() {
        this(PLUGIN_ID);
    }

    /**
     * Validates that a type can be mapped to JPA.
     *
     * @param type type to validate
     * @param typeKind domain type kind (if available)
     * @param propertyName property name for error messages
     * @return diagnostic warning if converter needed, empty if directly mappable
     */
    public Optional<Diagnostic> validate(TypeRef type, Optional<DomainTypeKind> typeKind, String propertyName) {

        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(typeKind, "typeKind");
        Objects.requireNonNull(propertyName, "propertyName");

        String typeName = type.render();

        // Check if directly mappable
        if (isDirectlyMappable(typeName, typeKind)) {
            return Optional.empty();
        }

        // Check if it's a known converter candidate
        if (isConverterCandidate(typeName)) {
            return Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.UNMAPPABLE_TYPE)
                    .pluginId(pluginId)
                    .message(String.format(
                            "Property '%s' has type '%s' which requires a JPA AttributeConverter. "
                                    + "Consider creating a converter or using a directly mappable type.",
                            propertyName, typeName))
                    .build());
        }

        // Unknown type - might need converter
        if (typeKind.isEmpty() || typeKind.get() == DomainTypeKind.OTHER) {
            return Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.UNMAPPABLE_TYPE)
                    .pluginId(pluginId)
                    .message(String.format(
                            "Property '%s' has unknown type '%s'. "
                                    + "JPA may not be able to map this type automatically. "
                                    + "Consider using @Convert with a custom AttributeConverter.",
                            propertyName, typeName))
                    .build());
        }

        return Optional.empty();
    }

    /**
     * Checks if a type is directly mappable to JPA without converters.
     */
    private boolean isDirectlyMappable(String typeName, Optional<DomainTypeKind> typeKind) {
        // Basic types
        if (isPrimitive(typeName)
                || isWrapper(typeName)
                || isString(typeName)
                || isTemporal(typeName)
                || isLob(typeName)) {
            return true;
        }

        // Enums are directly mappable
        if (typeKind.isPresent() && typeKind.get() == DomainTypeKind.ENUMERATION) {
            return true;
        }

        // Value Objects with @Embeddable or single-field (will use converter)
        if (typeKind.isPresent()
                && (typeKind.get() == DomainTypeKind.IDENTIFIER || typeKind.get() == DomainTypeKind.RECORD)) {
            return true; // Handled by EmbeddableGenerator or ConverterGenerator
        }

        return false;
    }

    /**
     * Checks if a type is a known converter candidate.
     */
    private boolean isConverterCandidate(String typeName) {
        return CONVERTER_CANDIDATES.contains(typeName);
    }

    private boolean isPrimitive(String typeName) {
        return PRIMITIVES.contains(typeName);
    }

    private boolean isWrapper(String typeName) {
        return WRAPPERS.contains(typeName);
    }

    private boolean isString(String typeName) {
        return STRING_TYPES.contains(typeName);
    }

    private boolean isTemporal(String typeName) {
        return TEMPORAL_TYPES.contains(typeName);
    }

    private boolean isLob(String typeName) {
        return LOB_TYPES.contains(typeName);
    }
}
