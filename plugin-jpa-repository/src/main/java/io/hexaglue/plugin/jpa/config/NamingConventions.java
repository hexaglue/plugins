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
package io.hexaglue.plugin.jpa.config;

import io.hexaglue.spi.util.Preconditions;

/**
 * Naming conventions for JPA code generation.
 *
 * <p>Defines suffixes and naming patterns for generated artifacts:</p>
 * <ul>
 *   <li><strong>Entity suffix</strong>: Appended to JPA entity classes (e.g., "CustomerEntity")</li>
 *   <li><strong>Adapter suffix</strong>: Appended to adapter implementations (e.g., "CustomerAdapter")</li>
 *   <li><strong>Repository suffix</strong>: Appended to Spring Data repos (e.g., "CustomerJpaRepository")</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       entitySuffix: Entity
 *       adapterSuffix: Adapter
 *       springDataRepositorySuffix: JpaRepository
 * }</pre>
 *
 * @param entitySuffix suffix for JPA entity classes (e.g., "Entity", "Jpa", "Model")
 * @param adapterSuffix suffix for adapter implementations (e.g., "Adapter", "JpaAdapter")
 * @param springDataRepositorySuffix suffix for Spring Data repository interfaces
 * @since 0.4.0
 */
public record NamingConventions(String entitySuffix, String adapterSuffix, String springDataRepositorySuffix) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any suffix is blank
     */
    public NamingConventions {
        Preconditions.requireNonBlank(entitySuffix, "entitySuffix");
        Preconditions.requireNonBlank(adapterSuffix, "adapterSuffix");
        Preconditions.requireNonBlank(springDataRepositorySuffix, "springDataRepositorySuffix");
    }

    /**
     * Default naming conventions matching common JPA practices.
     *
     * <p>Defaults:</p>
     * <ul>
     *   <li>Entity suffix: "Entity"</li>
     *   <li>Adapter suffix: "Adapter"</li>
     *   <li>Spring Data repository suffix: "JpaRepository"</li>
     * </ul>
     *
     * @return default naming conventions
     */
    public static NamingConventions defaults() {
        return new NamingConventions("Entity", "Adapter", "JpaRepository");
    }

    /**
     * Minimal naming conventions (empty suffixes).
     *
     * <p>Results in names like "Customer", "CustomerAdapter", "CustomerRepository".</p>
     *
     * @return minimal naming conventions
     */
    public static NamingConventions minimal() {
        return new NamingConventions("", "Adapter", "Repository");
    }
}
