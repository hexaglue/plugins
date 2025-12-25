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
import io.hexaglue.plugin.jpa.model.RelationshipModel;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates JPA relationship configurations.
 *
 * <p>This validator checks relationship configurations for common issues:</p>
 * <ul>
 *   <li><strong>Circular dependencies</strong>: Bidirectional relationships without proper mappedBy</li>
 *   <li><strong>Orphan removal</strong>: Only valid for composition relationships</li>
 *   <li><strong>Cascade operations</strong>: Warnings for dangerous cascades (e.g., REMOVE on inter-aggregate)</li>
 *   <li><strong>DDD compliance</strong>: Inter-aggregate relationships should use ID-only references</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>@OneToMany with orphanRemoval=true should be intra-aggregate</li>
 *   <li>@ManyToOne between aggregates should trigger warning</li>
 *   <li>Cascade ALL or REMOVE on inter-aggregate relationships is dangerous</li>
 *   <li>Bidirectional relationships should have proper mappedBy</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * RelationshipValidator validator = new RelationshipValidator("io.hexaglue.plugin.jpa");
 *
 * List<RelationshipModel> relationships = entity.relationships();
 * List<Diagnostic> diagnostics = validator.validateAll(relationships, "OrderEntity");
 *
 * diagnostics.forEach(context.diagnostics()::report);
 * }</pre>
 *
 * @since 0.4.0
 */
public final class RelationshipValidator {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    private final String pluginId;

    /**
     * Creates a relationship validator.
     *
     * @param pluginId plugin ID for diagnostic reporting
     */
    public RelationshipValidator(String pluginId) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }

    /**
     * Creates a relationship validator with default plugin ID.
     */
    public RelationshipValidator() {
        this(PLUGIN_ID);
    }

    /**
     * Validates all relationships for an entity.
     *
     * @param relationships list of relationships to validate
     * @param entityName entity name for error messages
     * @return list of diagnostic warnings/errors
     */
    public List<Diagnostic> validateAll(List<RelationshipModel> relationships, String entityName) {
        Objects.requireNonNull(relationships, "relationships");
        Objects.requireNonNull(entityName, "entityName");

        List<Diagnostic> diagnostics = new ArrayList<>();

        // Track target types to detect potential circular dependencies
        Set<String> targetTypes = new HashSet<>();

        for (RelationshipModel relationship : relationships) {
            // Validate individual relationship
            validate(relationship, entityName).ifPresent(diagnostics::add);

            // Track target type for circular dependency detection
            String targetType = relationship.targetType().render();
            if (targetTypes.contains(targetType)) {
                diagnostics.add(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(JpaDiagnosticCodes.CIRCULAR_RELATIONSHIP)
                        .pluginId(pluginId)
                        .message(String.format(
                                "Multiple relationships to '%s' detected in entity '%s'. "
                                        + "Ensure proper mappedBy configuration to avoid circular references.",
                                targetType, entityName))
                        .build());
            }
            targetTypes.add(targetType);
        }

        return diagnostics;
    }

    /**
     * Validates a single relationship configuration.
     *
     * @param relationship relationship to validate
     * @param entityName owning entity name
     * @return diagnostic if issues found, empty otherwise
     */
    public java.util.Optional<Diagnostic> validate(RelationshipModel relationship, String entityName) {
        Objects.requireNonNull(relationship, "relationship");
        Objects.requireNonNull(entityName, "entityName");

        // Validate orphanRemoval usage
        if (relationship.orphanRemoval() && relationship.isInterAggregate()) {
            return java.util.Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.INVALID_RELATIONSHIP)
                    .pluginId(pluginId)
                    .message(String.format(
                            "Property '%s' in entity '%s' uses orphanRemoval=true for an inter-aggregate relationship. "
                                    + "This violates DDD principles. Orphan removal should only be used for intra-aggregate composition.",
                            relationship.propertyName(), entityName))
                    .build());
        }

        // Validate cascade operations for inter-aggregate relationships
        if (relationship.isInterAggregate() && hasDangerousCascade(relationship)) {
            return java.util.Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.INVALID_RELATIONSHIP)
                    .pluginId(pluginId)
                    .message(String.format(
                            "Property '%s' in entity '%s' has CASCADE ALL or REMOVE for an inter-aggregate relationship. "
                                    + "This can lead to unintended deletions across aggregate boundaries. "
                                    + "Consider using ID-only references instead of @ManyToOne.",
                            relationship.propertyName(), entityName))
                    .build());
        }

        // Validate @ManyToOne for inter-aggregate (already warned in detection, but validate here too)
        if (relationship.relationshipType() == RelationshipModel.RelationshipType.MANY_TO_ONE
                && relationship.isInterAggregate()) {
            return java.util.Optional.of(Diagnostic.builder()
                    .severity(DiagnosticSeverity.WARNING)
                    .code(JpaDiagnosticCodes.INTER_AGGREGATE_MANY_TO_ONE)
                    .pluginId(pluginId)
                    .message(String.format(
                            "Property '%s' in entity '%s' uses @ManyToOne for an inter-aggregate relationship. "
                                    + "DDD best practice recommends ID-only references (e.g., String customerId) "
                                    + "instead of entity references across aggregate boundaries.",
                            relationship.propertyName(), entityName))
                    .build());
        }

        return java.util.Optional.empty();
    }

    /**
     * Checks if a relationship has dangerous cascade operations.
     *
     * <p>Dangerous cascades include ALL and REMOVE, especially for inter-aggregate relationships.</p>
     */
    private boolean hasDangerousCascade(RelationshipModel relationship) {
        for (RelationshipModel.CascadeType cascade : relationship.cascade()) {
            if (cascade == RelationshipModel.CascadeType.ALL || cascade == RelationshipModel.CascadeType.REMOVE) {
                return true;
            }
        }
        return false;
    }
}
