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

import io.hexaglue.spi.codegen.MergeMode;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.util.Preconditions;
import io.hexaglue.spi.util.Strings;
import java.util.Locale;
import java.util.Objects;

/**
 * Centralized configuration for the JPA repository plugin.
 *
 * <p>This immutable record consolidates all plugin options:</p>
 * <ul>
 *   <li><strong>Base package</strong>: Where to generate infrastructure code</li>
 *   <li><strong>Merge mode</strong>: How to handle existing files (OVERWRITE, SKIP, etc.)</li>
 *   <li><strong>Database options</strong>: Schema, ID generation strategy, sequence names</li>
 *   <li><strong>Feature flags</strong>: Auditing, soft delete, optimistic locking, etc.</li>
 *   <li><strong>Naming conventions</strong>: Suffixes for entities, adapters, repositories</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       basePackage: com.example.infrastructure.persistence
 *       mergeMode: OVERWRITE
 *       schema: public
 *       idStrategy: ASSIGNED
 *       sequenceName: ""
 *       enableAuditing: true
 *       enableSoftDelete: false
 *       enableOptimisticLocking: true
 *       generateQueryMethods: true
 *       entitySuffix: Entity
 *       adapterSuffix: Adapter
 *       springDataRepositorySuffix: JpaRepository
 * }</pre>
 *
 * @param basePackage base package for generated infrastructure code
 * @param mergeMode how to handle existing files during generation
 * @param schema database schema name (optional, empty string if not specified)
 * @param idStrategy ID generation strategy (IDENTITY, SEQUENCE, AUTO, UUID, ASSIGNED)
 * @param sequenceName sequence name for SEQUENCE strategy (optional)
 * @param featureFlags feature flags for optional capabilities
 * @param namingConventions naming conventions for generated classes
 * @since 0.4.0
 */
public record JpaPluginOptions(
        String basePackage,
        MergeMode mergeMode,
        String schema,
        IdGenerationStrategy idStrategy,
        String sequenceName,
        JpaFeatureFlags featureFlags,
        NamingConventions namingConventions) {

    /**
     * ID generation strategies for JPA entities.
     */
    public enum IdGenerationStrategy {
        /** Database auto-increment (MySQL, PostgreSQL SERIAL) */
        IDENTITY,
        /** Database sequence (PostgreSQL, Oracle) */
        SEQUENCE,
        /** JPA provider chooses strategy */
        AUTO,
        /** UUID generation (recommended for String IDs) */
        UUID,
        /** Application-assigned ID (no @GeneratedValue) */
        ASSIGNED
    }

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if basePackage is blank
     * @throws NullPointerException if any required parameter is null
     */
    public JpaPluginOptions {
        Preconditions.requireNonBlank(basePackage, "basePackage");
        Objects.requireNonNull(mergeMode, "mergeMode");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(idStrategy, "idStrategy");
        Objects.requireNonNull(sequenceName, "sequenceName");
        Objects.requireNonNull(featureFlags, "featureFlags");
        Objects.requireNonNull(namingConventions, "namingConventions");
    }

    /**
     * Resolves plugin options from YAML configuration and context.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>YAML configuration (highest priority)</li>
     *   <li>Context-derived defaults (e.g., basePackage from naming surface)</li>
     *   <li>Plugin defaults (lowest priority)</li>
     * </ol>
     *
     * @param pluginOptions plugin options view from context
     * @param context generation context
     * @return resolved JPA plugin options
     * @throws NullPointerException if any parameter is null
     */
    public static JpaPluginOptions resolve(OptionsView.PluginOptionsView pluginOptions, GenerationContextSpec context) {
        Objects.requireNonNull(pluginOptions, "pluginOptions");
        Objects.requireNonNull(context, "context");

        // Base package: YAML → context naming surface → fallback
        String basePackage =
                pluginOptions.getOrDefault("basePackage", String.class, "").trim();
        if (Strings.isBlank(basePackage)) {
            String contextBase = context.names().basePackage();
            basePackage = (Strings.isBlank(contextBase) ? "generated" : contextBase) + ".infrastructure.persistence";
        }

        // Merge mode
        String mergeModeRaw = pluginOptions.getOrDefault("mergeMode", String.class, MergeMode.OVERWRITE.name());
        MergeMode mergeMode = parseMergeModeOrDefault(mergeModeRaw);

        // Database options
        String schema = pluginOptions.getOrDefault("schema", String.class, "");
        String idStrategyRaw = pluginOptions
                .getOrDefault("idStrategy", String.class, "ASSIGNED")
                .toUpperCase(Locale.ROOT);
        IdGenerationStrategy idStrategy = parseIdStrategyOrDefault(idStrategyRaw);
        String sequenceName = pluginOptions.getOrDefault("sequenceName", String.class, "");

        // Feature flags
        boolean enableAuditing = pluginOptions.getOrDefault("enableAuditing", Boolean.class, true);
        boolean enableSoftDelete = pluginOptions.getOrDefault("enableSoftDelete", Boolean.class, false);
        boolean enableOptimisticLocking = pluginOptions.getOrDefault("enableOptimisticLocking", Boolean.class, true);
        boolean generateQueryMethods = pluginOptions.getOrDefault("generateQueryMethods", Boolean.class, true);
        JpaFeatureFlags featureFlags =
                new JpaFeatureFlags(enableAuditing, enableSoftDelete, enableOptimisticLocking, generateQueryMethods);

        // Naming conventions
        String entitySuffix = pluginOptions
                .getOrDefault("entitySuffix", String.class, "Entity")
                .trim();
        String adapterSuffix = pluginOptions
                .getOrDefault("adapterSuffix", String.class, "Adapter")
                .trim();
        String repoSuffix = pluginOptions
                .getOrDefault("springDataRepositorySuffix", String.class, "JpaRepository")
                .trim();
        NamingConventions namingConventions = new NamingConventions(entitySuffix, adapterSuffix, repoSuffix);

        return new JpaPluginOptions(
                basePackage, mergeMode, schema, idStrategy, sequenceName, featureFlags, namingConventions);
    }

    /**
     * Parses merge mode from string with fallback to OVERWRITE.
     *
     * @param raw raw string from configuration
     * @return parsed MergeMode or OVERWRITE if invalid
     */
    private static MergeMode parseMergeModeOrDefault(String raw) {
        if (Strings.isBlank(raw)) {
            return MergeMode.OVERWRITE;
        }
        try {
            return MergeMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MergeMode.OVERWRITE;
        }
    }

    /**
     * Parses ID generation strategy from string with fallback to ASSIGNED.
     *
     * @param raw raw string from configuration
     * @return parsed strategy or ASSIGNED if invalid
     */
    private static IdGenerationStrategy parseIdStrategyOrDefault(String raw) {
        if (Strings.isBlank(raw)) {
            return IdGenerationStrategy.ASSIGNED;
        }
        try {
            return IdGenerationStrategy.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return IdGenerationStrategy.ASSIGNED;
        }
    }
}
