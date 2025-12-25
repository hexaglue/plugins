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

/**
 * Feature flags for JPA plugin capabilities.
 *
 * <p>This record encapsulates boolean flags that enable/disable various
 * JPA features during code generation.</p>
 *
 * <h2>Available Features</h2>
 * <ul>
 *   <li><strong>Auditing</strong>: Automatic createdAt/updatedAt timestamp fields</li>
 *   <li><strong>Soft Delete</strong>: deletedAt field instead of hard deletion</li>
 *   <li><strong>Optimistic Locking</strong>: version field for concurrent modification detection</li>
 *   <li><strong>Query Methods</strong>: Generate derived query methods in Spring Data repositories</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       enableAuditing: true
 *       enableSoftDelete: false
 *       enableOptimisticLocking: true
 *       generateQueryMethods: true
 * }</pre>
 *
 * @param enableAuditing if true, add createdAt/updatedAt fields with @CreatedDate/@LastModifiedDate
 * @param enableSoftDelete if true, add deletedAt field and use @Where clause for soft deletion
 * @param enableOptimisticLocking if true, add version field with @Version annotation
 * @param generateQueryMethods if true, generate derived query methods from port method signatures
 * @since 0.4.0
 */
public record JpaFeatureFlags(
        boolean enableAuditing,
        boolean enableSoftDelete,
        boolean enableOptimisticLocking,
        boolean generateQueryMethods) {

    /**
     * Default feature flags with commonly used settings.
     *
     * <p>Defaults:</p>
     * <ul>
     *   <li>Auditing: ENABLED (recommended for production)</li>
     *   <li>Soft Delete: DISABLED (opt-in behavior)</li>
     *   <li>Optimistic Locking: ENABLED (recommended for concurrent systems)</li>
     *   <li>Query Methods: ENABLED (convenience feature)</li>
     * </ul>
     *
     * @return default feature flags
     */
    public static JpaFeatureFlags defaults() {
        return new JpaFeatureFlags(
                true, // enableAuditing
                false, // enableSoftDelete
                true, // enableOptimisticLocking
                true // generateQueryMethods
                );
    }

    /**
     * Creates feature flags with all features disabled.
     *
     * <p>Useful for minimal code generation or testing.</p>
     *
     * @return feature flags with all features disabled
     */
    public static JpaFeatureFlags none() {
        return new JpaFeatureFlags(false, false, false, false);
    }

    /**
     * Creates feature flags with all features enabled.
     *
     * <p>Useful for full-featured entities with all bells and whistles.</p>
     *
     * @return feature flags with all features enabled
     */
    public static JpaFeatureFlags all() {
        return new JpaFeatureFlags(true, true, true, true);
    }
}
