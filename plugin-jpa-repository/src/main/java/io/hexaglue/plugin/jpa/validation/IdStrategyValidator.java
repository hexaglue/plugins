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

import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.diagnostics.JpaDiagnosticCodes;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates ID generation strategy compatibility with ID types.
 *
 * <p>This validator ensures that the configured ID generation strategy
 * is compatible with the actual ID type used in the domain model.</p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>IDENTITY/SEQUENCE/AUTO</strong>: Requires numeric types (Long, Integer, Short, Byte)</li>
 *   <li><strong>UUID</strong>: Requires String or java.util.UUID</li>
 *   <li><strong>ASSIGNED</strong>: Works with any type</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * IdStrategyValidator validator = new IdStrategyValidator("io.hexaglue.plugin.jpa");
 *
 * TypeRef idType = context.types().classRef("java.lang.String");
 * JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;
 *
 * Optional<Diagnostic> error = validator.validate(idType, strategy, "CustomerRepository");
 * if (error.isPresent()) {
 *     context.diagnostics().report(error.get());
 * }
 * }</pre>
 *
 * @since 0.4.0
 */
public final class IdStrategyValidator {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    /**
     * Set of numeric types that support database-generated IDs.
     */
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "java.lang.Long",
            "Long",
            "long",
            "java.lang.Integer",
            "Integer",
            "int",
            "java.lang.Short",
            "Short",
            "short",
            "java.lang.Byte",
            "Byte",
            "byte");

    /**
     * Set of String types for UUID strategy.
     */
    private static final Set<String> STRING_TYPES = Set.of("java.lang.String", "String");

    /**
     * Set of UUID types.
     */
    private static final Set<String> UUID_TYPES = Set.of("java.util.UUID", "UUID");

    private final String pluginId;

    /**
     * Creates an ID strategy validator.
     *
     * @param pluginId plugin ID for diagnostic reporting
     */
    public IdStrategyValidator(String pluginId) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }

    /**
     * Creates an ID strategy validator with default plugin ID.
     */
    public IdStrategyValidator() {
        this(PLUGIN_ID);
    }

    /**
     * Validates ID type compatibility with generation strategy.
     *
     * @param idType ID type to validate
     * @param strategy generation strategy
     * @param contextName context name for error messages (e.g., port name)
     * @return diagnostic error if incompatible, empty otherwise
     */
    public Optional<Diagnostic> validate(
            TypeRef idType, JpaPluginOptions.IdGenerationStrategy strategy, String contextName) {

        Objects.requireNonNull(idType, "idType");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(contextName, "contextName");

        String idTypeName = idType.render();

        // Check strategy-specific requirements
        switch (strategy) {
            case IDENTITY:
            case SEQUENCE:
            case AUTO:
                return validateNumericStrategy(idTypeName, strategy, contextName);

            case UUID:
                return validateUuidStrategy(idTypeName, contextName);

            case ASSIGNED:
                // ASSIGNED works with any type
                return Optional.empty();

            default:
                return Optional.of(Diagnostic.builder()
                        .severity(DiagnosticSeverity.ERROR)
                        .code(JpaDiagnosticCodes.INVALID_CONFIG)
                        .pluginId(pluginId)
                        .message(String.format("Unknown ID generation strategy '%s' for '%s'", strategy, contextName))
                        .build());
        }
    }

    /**
     * Validates that ID type is numeric for database-generated strategies.
     */
    private Optional<Diagnostic> validateNumericStrategy(
            String idTypeName, JpaPluginOptions.IdGenerationStrategy strategy, String contextName) {

        if (!isNumericType(idTypeName)) {
            return Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.ERROR)
                    .code(JpaDiagnosticCodes.INCOMPATIBLE_ID_STRATEGY)
                    .pluginId(pluginId)
                    .message(String.format(
                            "ID generation strategy '%s' is incompatible with ID type '%s' for '%s'. "
                                    + "Numeric strategies (IDENTITY, SEQUENCE, AUTO) require numeric types (Long, Integer, Short, Byte). "
                                    + "Please either: (1) change idStrategy to 'UUID' or 'ASSIGNED' in hexaglue.yaml, "
                                    + "or (2) change the domain ID type to use Long/Integer.",
                            strategy, idTypeName, contextName))
                    .build());
        }

        return Optional.empty();
    }

    /**
     * Validates that ID type is String or UUID for UUID strategy.
     */
    private Optional<Diagnostic> validateUuidStrategy(String idTypeName, String contextName) {

        if (!isStringType(idTypeName) && !isUuidType(idTypeName)) {
            return Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.INCOMPATIBLE_ID_STRATEGY)
                    .pluginId(pluginId)
                    .message(String.format(
                            "ID generation strategy 'UUID' is typically used with String or java.util.UUID types, "
                                    + "but ID type is '%s' for '%s'. "
                                    + "Consider using String or UUID type, or change strategy to 'ASSIGNED'.",
                            idTypeName, contextName))
                    .build());
        }

        return Optional.empty();
    }

    /**
     * Checks if a type is numeric.
     */
    private boolean isNumericType(String typeName) {
        return NUMERIC_TYPES.contains(typeName);
    }

    /**
     * Checks if a type is String.
     */
    private boolean isStringType(String typeName) {
        return STRING_TYPES.contains(typeName);
    }

    /**
     * Checks if a type is UUID.
     */
    private boolean isUuidType(String typeName) {
        return UUID_TYPES.contains(typeName);
    }
}
