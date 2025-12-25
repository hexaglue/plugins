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
package io.hexaglue.plugin.jpa.model;

import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Model representing JPA entity ID metadata.
 *
 * <p>This model captures all information needed to generate a JPA @Id field:</p>
 * <ul>
 *   <li><strong>Unwrapped type</strong>: The actual persistence type (String, Long, etc.)</li>
 *   <li><strong>Original type</strong>: The domain type (CustomerId, OrderId, etc.)</li>
 *   <li><strong>Generation strategy</strong>: How the ID is generated (IDENTITY, UUID, ASSIGNED, etc.)</li>
 *   <li><strong>Sequence metadata</strong>: For SEQUENCE strategy</li>
 *   <li><strong>Composite flag</strong>: Whether this is a composite ID (@EmbeddedId)</li>
 * </ul>
 *
 * <h2>Unwrapping Strategy</h2>
 * <p>Value Object IDs like {@code record CustomerId(String value)} are unwrapped to their
 * persistence type {@code String} for JPA mapping. The mapper handles conversion.</p>
 *
 * <h2>Strategy Validation</h2>
 * <p>ID generation strategies have type requirements:</p>
 * <ul>
 *   <li><strong>IDENTITY/SEQUENCE/AUTO</strong>: Require numeric types (Long, Integer)</li>
 *   <li><strong>UUID</strong>: Requires String or java.util.UUID</li>
 *   <li><strong>ASSIGNED</strong>: Works with any type</li>
 * </ul>
 *
 * @param unwrappedType the persistence type for JPA (String, Long, etc.)
 * @param originalType the domain type (CustomerId, etc.), same as unwrappedType if not a Value Object
 * @param strategy ID generation strategy
 * @param sequenceName sequence name for SEQUENCE strategy (empty if not applicable)
 * @param isComposite true if this is a composite ID requiring @EmbeddedId
 * @since 0.4.0
 */
public record IdModel(
        TypeRef unwrappedType,
        TypeRef originalType,
        JpaPluginOptions.IdGenerationStrategy strategy,
        String sequenceName,
        boolean isComposite) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public IdModel {
        Objects.requireNonNull(unwrappedType, "unwrappedType");
        Objects.requireNonNull(originalType, "originalType");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(sequenceName, "sequenceName");
    }

    /**
     * Creates a simple (non-composite) ID model.
     *
     * @param unwrappedType persistence type
     * @param originalType domain type
     * @param strategy generation strategy
     * @param sequenceName sequence name (empty if not applicable)
     * @return simple ID model
     */
    public static IdModel simple(
            TypeRef unwrappedType,
            TypeRef originalType,
            JpaPluginOptions.IdGenerationStrategy strategy,
            String sequenceName) {
        return new IdModel(unwrappedType, originalType, strategy, sequenceName, false);
    }

    /**
     * Creates a composite ID model.
     *
     * <p>Composite IDs use @EmbeddedId and ASSIGNED strategy.</p>
     *
     * @param embeddableType the @Embeddable type for the composite ID
     * @return composite ID model
     */
    public static IdModel composite(TypeRef embeddableType) {
        return new IdModel(embeddableType, embeddableType, JpaPluginOptions.IdGenerationStrategy.ASSIGNED, "", true);
    }

    /**
     * Checks if this ID was unwrapped from a Value Object.
     *
     * @return true if unwrapped, false if using original type directly
     */
    public boolean isUnwrapped() {
        return !unwrappedType.equals(originalType);
    }

    /**
     * Gets the sequence name if this ID uses SEQUENCE strategy.
     *
     * @return sequence name or empty if not applicable
     */
    public Optional<String> sequenceNameIfPresent() {
        return sequenceName.isBlank() ? Optional.empty() : Optional.of(sequenceName);
    }

    /**
     * Checks if this ID requires @GeneratedValue annotation.
     *
     * @return true if strategy requires @GeneratedValue, false otherwise
     */
    public boolean requiresGeneratedValue() {
        return strategy != JpaPluginOptions.IdGenerationStrategy.UUID
                && strategy != JpaPluginOptions.IdGenerationStrategy.ASSIGNED;
    }
}
