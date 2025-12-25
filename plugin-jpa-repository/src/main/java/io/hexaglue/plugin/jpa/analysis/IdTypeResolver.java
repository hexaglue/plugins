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

import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.diagnostics.JpaDiagnosticCodes;
import io.hexaglue.plugin.jpa.model.IdModel;
import io.hexaglue.plugin.jpa.validation.IdStrategyValidator;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves and unwraps ID types for JPA persistence.
 *
 * <p>This resolver handles the critical task of converting domain ID types
 * (often Value Objects) into types suitable for JPA persistence:</p>
 *
 * <h2>Unwrapping Strategy</h2>
 * <ul>
 *   <li><strong>Single-field Value Object</strong>: {@code record CustomerId(String value)} → {@code String}</li>
 *   <li><strong>Simple type</strong>: {@code Long}, {@code String} → used as-is</li>
 *   <li><strong>Composite ID</strong>: Multiple properties → @EmbeddedId</li>
 * </ul>
 *
 * <h2>Strategy Validation</h2>
 * <p>Validates that the ID type is compatible with the generation strategy:</p>
 * <ul>
 *   <li><strong>IDENTITY/SEQUENCE/AUTO</strong>: Requires Long or Integer</li>
 *   <li><strong>UUID</strong>: Requires String or java.util.UUID</li>
 *   <li><strong>ASSIGNED</strong>: Works with any type</li>
 * </ul>
 *
 * <h2>Inference Heuristics</h2>
 * <p>If ID type cannot be determined from port methods, uses fallback heuristics:</p>
 * <ol>
 *   <li>Look for {@code findById(ID)}, {@code deleteById(ID)}, {@code existsById(ID)}</li>
 *   <li>Look for any parameter named "id"</li>
 *   <li>Fallback to Object with warning</li>
 * </ol>
 *
 * @since 0.4.0
 */
public final class IdTypeResolver {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    private final GenerationContextSpec context;
    private final JpaPluginOptions options;
    private final IdStrategyValidator idStrategyValidator;

    public IdTypeResolver(GenerationContextSpec context, JpaPluginOptions options) {
        this.context = Objects.requireNonNull(context, "context");
        this.options = Objects.requireNonNull(options, "options");
        this.idStrategyValidator = new IdStrategyValidator(PLUGIN_ID);
    }

    /**
     * Resolves the ID model for a port.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Infers the ID type from port method signatures</li>
     *   <li>Unwraps Value Object IDs to their persistence type</li>
     *   <li>Validates strategy compatibility</li>
     *   <li>Creates an IdModel with all metadata</li>
     * </ol>
     *
     * @param port port to analyze
     * @return ID model, or model with Object type if inference fails
     */
    public IdModel resolve(PortView port) {
        Objects.requireNonNull(port, "port");

        // Infer original ID type from port methods
        TypeRef originalIdType = inferIdType(port).orElseGet(() -> {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(JpaDiagnosticCodes.NO_ID_TYPE)
                            .pluginId(PLUGIN_ID)
                            .message("Could not infer ID type for port '" + port.qualifiedName()
                                    + "'. Using java.lang.Object as fallback.")
                            .build());
            return context.types().objectType();
        });

        // Check if this is a composite ID (multi-property Value Object)
        if (isCompositeId(originalIdType)) {
            // Composite IDs use @EmbeddedId with ASSIGNED strategy
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.INFO)
                            .code(JpaDiagnosticCodes.CONFIG_RESOLVED)
                            .pluginId(PLUGIN_ID)
                            .message("Detected composite ID '" + originalIdType.render()
                                    + "' for port '" + port.qualifiedName()
                                    + "'. Will generate @EmbeddedId with @Embeddable class.")
                            .build());
            return IdModel.composite(originalIdType);
        }

        // Unwrap Value Object IDs to persistence type
        TypeRef unwrappedIdType = unwrapIdType(originalIdType);

        // Validate strategy compatibility
        validateStrategyCompatibility(unwrappedIdType, port);

        // Create ID model
        return IdModel.simple(unwrappedIdType, originalIdType, options.idStrategy(), options.sequenceName());
    }

    /**
     * Checks if an ID type is composite (has multiple properties).
     *
     * <p>Composite IDs require @EmbeddedId instead of @Id.</p>
     *
     * @param idType ID type to check
     * @return true if composite (multi-property), false otherwise
     */
    private boolean isCompositeId(TypeRef idType) {
        String idTypeName = idType.render();

        // Look up in domain model
        Optional<DomainTypeView> domainType = context.model().domain().findType(idTypeName);

        if (domainType.isPresent()) {
            DomainTypeView type = domainType.get();

            // Composite ID: Value Object with multiple properties
            if (type.kind() == DomainTypeKind.IDENTIFIER || type.kind() == DomainTypeKind.RECORD) {
                return type.properties().size() > 1;
            }
        }

        return false;
    }

    /**
     * Infers ID type from port method signatures.
     *
     * <p>Looks for methods like:</p>
     * <ul>
     *   <li>{@code findById(ID)}</li>
     *   <li>{@code deleteById(ID)}</li>
     *   <li>{@code existsById(ID)}</li>
     * </ul>
     *
     * @param port port to analyze
     * @return inferred ID type or empty if not found
     */
    private Optional<TypeRef> inferIdType(PortView port) {
        // Look for ID methods
        for (PortMethodView method : port.methods()) {
            String methodName = method.name().toLowerCase(Locale.ROOT);
            if ((methodName.equals("findbyid") || methodName.equals("deletebyid") || methodName.equals("existsbyid"))
                    && method.parameters().size() == 1) {
                return Optional.of(method.parameters().get(0).type());
            }
        }

        // Fallback: look for parameter named "id"
        for (PortMethodView method : port.methods()) {
            for (PortParameterView param : method.parameters()) {
                if ("id".equalsIgnoreCase(param.name())) {
                    return Optional.of(param.type());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Unwraps a Value Object ID to its persistence type.
     *
     * <p>Example: {@code record CustomerId(String value)} → {@code String}</p>
     *
     * @param idType original ID type
     * @return unwrapped type suitable for JPA persistence
     */
    private TypeRef unwrapIdType(TypeRef idType) {
        String idTypeName = idType.render();

        // Look up in domain model
        Optional<DomainTypeView> domainType = context.model().domain().findType(idTypeName);

        if (domainType.isPresent()) {
            DomainTypeView type = domainType.get();

            // Single-field Value Object: unwrap to inner type
            if (type.kind() == DomainTypeKind.IDENTIFIER || type.kind() == DomainTypeKind.RECORD) {
                List<DomainPropertyView> properties = type.properties();
                if (properties.size() == 1) {
                    return properties.get(0).type();
                }
            }
        }

        // Fallback heuristic: XxxId types ending in "Id" → assume String
        if ((idTypeName.endsWith("Id") || idTypeName.endsWith("ID"))
                && !idTypeName.equals("java.lang.Long")
                && !idTypeName.equals("java.lang.Integer")
                && !idTypeName.equals("java.lang.String")
                && !idTypeName.equals("java.util.UUID")) {
            return context.types().classRef("java.lang.String");
        }

        // No unwrapping needed
        return idType;
    }

    /**
     * Validates that the ID type is compatible with the generation strategy.
     *
     * <p>Emits ERROR diagnostic if incompatibility is detected.</p>
     *
     * @param unwrappedIdType unwrapped persistence ID type
     * @param port port being processed (for diagnostic context)
     */
    /**
     * Validates ID generation strategy compatibility with ID type.
     *
     * <p>Delegates to {@link IdStrategyValidator} for comprehensive validation.</p>
     */
    private void validateStrategyCompatibility(TypeRef unwrappedIdType, PortView port) {
        JpaPluginOptions.IdGenerationStrategy strategy = options.idStrategy();

        // Use IdStrategyValidator for comprehensive validation
        Optional<Diagnostic> diagnostic = idStrategyValidator.validate(unwrappedIdType, strategy, port.qualifiedName());

        diagnostic.ifPresent(context.diagnostics()::report);
    }
}
