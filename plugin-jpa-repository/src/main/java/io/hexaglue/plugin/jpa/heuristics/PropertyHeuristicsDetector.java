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
package io.hexaglue.plugin.jpa.heuristics;

import io.hexaglue.spi.ir.domain.DomainPropertyView;
import java.util.Optional;

/**
 * Interface for property heuristics detection.
 *
 * <p>Property heuristics detectors analyze domain properties and infer metadata based on
 * naming conventions, type patterns, and structural characteristics. This provides sensible
 * defaults when explicit annotations or YAML configuration are not present.</p>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Heuristics are applied as the third level in the metadata resolution chain:
 * <ol>
 *   <li>Explicit annotations (highest priority)</li>
 *   <li>YAML configuration</li>
 *   <li>Heuristics (this layer)</li>
 *   <li>System defaults (lowest priority)</li>
 * </ol>
 * </p>
 *
 * @since 0.4.0
 */
public interface PropertyHeuristicsDetector {

    /**
     * Detects the suggested column type for a property.
     *
     * <p>Examples:
     * <ul>
     *   <li>Property named "description" → "TEXT"</li>
     *   <li>Property named "email" → "VARCHAR(320)"</li>
     *   <li>Property named "url" → "VARCHAR(2048)"</li>
     * </ul>
     *
     * @param property domain property to analyze
     * @return detected column type or empty if no heuristic applies
     */
    Optional<String> detectColumnType(DomainPropertyView property);

    /**
     * Detects the suggested nullability for a property.
     *
     * <p>Examples:
     * <ul>
     *   <li>Primitive types → false (NOT NULL)</li>
     *   <li>Collections → false (empty collection instead of null)</li>
     *   <li>Properties containing "id" → false (NOT NULL)</li>
     *   <li>Reference types → true (nullable by default)</li>
     * </ul>
     *
     * @param property domain property to analyze
     * @return detected nullability or empty if no heuristic applies
     */
    Optional<Boolean> detectNullability(DomainPropertyView property);

    /**
     * Detects the suggested maximum length for a property.
     *
     * <p>Examples:
     * <ul>
     *   <li>Property named "code" or "reference" → 50</li>
     *   <li>Property named "name" → 100</li>
     *   <li>Property named "title" → 200</li>
     *   <li>Property named "email" → 320</li>
     * </ul>
     *
     * @param property domain property to analyze
     * @return detected max length or empty if no heuristic applies
     */
    Optional<Integer> detectMaxLength(DomainPropertyView property);

    /**
     * Detects whether a property should be unique.
     *
     * <p>Examples:
     * <ul>
     *   <li>Property named "email" → true</li>
     *   <li>Property named "username" → true</li>
     *   <li>Property named "code" or "reference" → true</li>
     * </ul>
     *
     * @param property domain property to analyze
     * @return detected uniqueness or empty if no heuristic applies
     */
    Optional<Boolean> detectUniqueness(DomainPropertyView property);
}
