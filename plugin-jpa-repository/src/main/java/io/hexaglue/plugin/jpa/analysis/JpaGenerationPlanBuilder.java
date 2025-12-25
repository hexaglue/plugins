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
import io.hexaglue.plugin.jpa.model.EntityModel;
import io.hexaglue.plugin.jpa.model.IdModel;
import io.hexaglue.plugin.jpa.model.JpaGenerationPlan;
import io.hexaglue.plugin.jpa.model.PropertyModel;
import io.hexaglue.plugin.jpa.model.QueryMethodModel;
import io.hexaglue.plugin.jpa.model.RelationshipModel;
import io.hexaglue.plugin.jpa.util.NamingUtils;
import io.hexaglue.plugin.jpa.validation.RelationshipValidator;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates the analysis of a port to build a complete JPA generation plan.
 *
 * <p>This builder coordinates all analyzers and resolvers to produce a
 * {@link JpaGenerationPlan} containing everything needed to generate
 * JPA artifacts.</p>
 *
 * <h2>Build Process</h2>
 * <ol>
 *   <li><strong>Analyze port</strong>: Extract domain type and validate repository pattern</li>
 *   <li><strong>Resolve ID</strong>: Unwrap Value Object IDs, validate strategy</li>
 *   <li><strong>Resolve properties</strong>: Analyze each domain property for JPA mapping</li>
 *   <li><strong>Detect relationships</strong>: Identify @OneToMany, @Embedded, @ElementCollection</li>
 *   <li><strong>Build entity model</strong>: Assemble complete entity metadata</li>
 *   <li><strong>Generate names</strong>: Compute qualified names for all artifacts</li>
 *   <li><strong>Create plan</strong>: Package everything into JpaGenerationPlan</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * JpaGenerationPlanBuilder builder = new JpaGenerationPlanBuilder(context, options);
 * JpaGenerationPlan plan = builder.build(customerRepositoryPort);
 *
 * // Plan contains:
 * // - EntityModel with all properties and metadata
 * // - Qualified names for entity, repository, mapper, adapter
 * // - Port reference for method implementation
 * }</pre>
 *
 * @since 0.4.0
 */
public final class JpaGenerationPlanBuilder {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    private final GenerationContextSpec context;
    private final JpaPluginOptions options;
    private final RelationshipValidator relationshipValidator;

    public JpaGenerationPlanBuilder(GenerationContextSpec context, JpaPluginOptions options) {
        this.context = Objects.requireNonNull(context, "context");
        this.options = Objects.requireNonNull(options, "options");
        this.relationshipValidator = new RelationshipValidator(PLUGIN_ID);
    }

    /**
     * Builds a complete JPA generation plan for a port.
     *
     * <p>This method orchestrates all analysis steps:</p>
     * <ol>
     *   <li>Infer domain type from port</li>
     *   <li>Resolve ID model with unwrapping</li>
     *   <li>Resolve all domain properties</li>
     *   <li>Detect all relationships</li>
     *   <li>Build entity model</li>
     *   <li>Generate qualified artifact names</li>
     * </ol>
     *
     * @param port port to analyze
     * @return complete generation plan
     */
    public JpaGenerationPlan build(PortView port) {
        Objects.requireNonNull(port, "port");

        // Step 1: Infer domain type
        TypeRef domainType = PortAnalyzer.inferDomainType(port).orElseGet(() -> {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(JpaDiagnosticCodes.NO_DOMAIN_TYPE)
                            .pluginId(PLUGIN_ID)
                            .message("Could not infer domain type for port '" + port.qualifiedName()
                                    + "'. Using java.lang.Object as fallback.")
                            .build());
            return context.types().objectType();
        });

        // Step 2: Resolve ID model
        IdTypeResolver idResolver = new IdTypeResolver(context, options);
        IdModel idModel = idResolver.resolve(port);

        // Step 3: Infer entity name from port name
        String entityBaseName = NamingUtils.inferEntityName(port.simpleName());
        String entityClassName = entityBaseName + options.namingConventions().entitySuffix();
        String tableName = resolveTableName(domainType, entityBaseName);

        // Step 4: Resolve domain properties
        List<PropertyModel> properties = resolveDomainProperties(domainType);

        // Step 5: Detect relationships
        List<RelationshipModel> relationships = detectRelationships(domainType);

        // Step 6: Analyze port methods for query patterns
        List<QueryMethodModel> queryMethods = analyzeQueryMethods(port);

        // Step 7: Build entity model
        EntityModel entityModel = EntityModel.builder()
                .entityClassName(entityClassName)
                .entityPackage(options.basePackage() + ".entity")
                .tableName(tableName)
                .schema(options.schema())
                .domainType(domainType)
                .idModel(idModel)
                .properties(properties)
                .relationships(relationships)
                .enableAuditing(options.featureFlags().enableAuditing())
                .enableSoftDelete(options.featureFlags().enableSoftDelete())
                .enableOptimisticLocking(options.featureFlags().enableOptimisticLocking())
                .build();

        // Step 8: Generate qualified names for all artifacts
        String basePackage = options.basePackage();
        String entityQn = basePackage + ".entity." + entityClassName;
        String repoQn = basePackage + ".springdata." + entityBaseName
                + options.namingConventions().springDataRepositorySuffix();
        String mapperQn = basePackage + ".mapper." + entityBaseName + "Mapper";
        String adapterQn = basePackage + ".adapter." + entityBaseName
                + options.namingConventions().adapterSuffix();

        // Step 9: Create plan
        return JpaGenerationPlan.builder()
                .port(port)
                .entityModel(entityModel)
                .entityQualifiedName(entityQn)
                .springDataRepoQualifiedName(repoQn)
                .mapperQualifiedName(mapperQn)
                .adapterQualifiedName(adapterQn)
                .queryMethods(queryMethods)
                .build();
    }

    /**
     * Resolves the table name for a domain type.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>YAML configuration (types.{fqn}.tableName)</li>
     *   <li>Default: snake_case + pluralization</li>
     * </ol>
     *
     * @param domainType domain type reference
     * @param entityBaseName entity base name (e.g., "Customer")
     * @return table name
     */
    private String resolveTableName(TypeRef domainType, String entityBaseName) {
        // Check YAML configuration
        String domainTypeName = domainType.render();
        String yamlTableName = context.options()
                .forPlugin(PLUGIN_ID)
                .getOrDefault("types." + domainTypeName + ".tableName", String.class, "");

        if (!yamlTableName.isBlank()) {
            return yamlTableName;
        }

        // Default: snake_case + pluralization
        return NamingUtils.toTableName(entityBaseName);
    }

    /**
     * Resolves all properties for a domain type.
     *
     * <p>Analyzes each domain property and converts it to a JPA property model.</p>
     *
     * @param domainType domain type reference
     * @return list of property models
     */
    private List<PropertyModel> resolveDomainProperties(TypeRef domainType) {
        String domainTypeName = domainType.render();

        // Look up domain type in IR
        Optional<DomainTypeView> domainTypeOpt = context.model().domain().findType(domainTypeName);

        if (domainTypeOpt.isEmpty()) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(JpaDiagnosticCodes.DOMAIN_TYPE_NOT_IN_IR)
                            .pluginId(PLUGIN_ID)
                            .message("Domain type '" + domainTypeName
                                    + "' not found in IR. Skipping property generation.")
                            .build());
            return List.of();
        }

        DomainTypeView domainTypeView = domainTypeOpt.get();
        PropertyTypeResolver propertyResolver =
                new PropertyTypeResolver(context, context.options().forPlugin(PLUGIN_ID), domainTypeName);

        List<PropertyModel> properties = new ArrayList<>();
        for (DomainPropertyView property : domainTypeView.properties()) {
            // Skip ID property (handled separately)
            if ("id".equalsIgnoreCase(property.name())) {
                continue;
            }

            PropertyModel propertyModel = propertyResolver.resolve(property);
            properties.add(propertyModel);
        }

        return properties;
    }

    /**
     * Detects all JPA relationships for a domain type.
     *
     * <p>Uses {@link RelationshipDetector} to identify:</p>
     * <ul>
     *   <li>@OneToMany relationships (collections of entities)</li>
     *   <li>@Embedded relationships (multi-field Value Objects)</li>
     *   <li>@ElementCollection relationships (collections of simple types/embeddables)</li>
     *   <li>@ManyToOne relationships (entity references, with warnings for inter-aggregate)</li>
     * </ul>
     *
     * @param domainType domain type reference
     * @return list of detected relationships
     */
    private List<RelationshipModel> detectRelationships(TypeRef domainType) {
        String domainTypeName = domainType.render();

        // Look up domain type in IR
        Optional<DomainTypeView> domainTypeOpt = context.model().domain().findType(domainTypeName);

        if (domainTypeOpt.isEmpty()) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(JpaDiagnosticCodes.DOMAIN_TYPE_NOT_IN_IR)
                            .pluginId(PLUGIN_ID)
                            .message("Domain type '" + domainTypeName
                                    + "' not found in IR. Skipping relationship detection.")
                            .build());
            return List.of();
        }

        DomainTypeView domainTypeView = domainTypeOpt.get();
        RelationshipDetector relationshipDetector = new RelationshipDetector(context, domainTypeName);

        List<RelationshipModel> relationships = relationshipDetector.detectRelationships(domainTypeView);

        // Validate all relationships (orphan removal, cascade, inter-aggregate, circular dependencies)
        List<Diagnostic> validationDiagnostics = relationshipValidator.validateAll(relationships, domainTypeName);
        validationDiagnostics.forEach(context.diagnostics()::report);

        return relationships;
    }

    /**
     * Analyzes port methods to detect derived query method patterns.
     *
     * <p>This method uses {@link PortMethodAnalyzer} to detect Spring Data JPA
     * query patterns like findByX, existsByX, countByX, etc.</p>
     *
     * @param port port to analyze
     * @return list of detected query methods
     */
    private List<QueryMethodModel> analyzeQueryMethods(PortView port) {
        PortMethodAnalyzer methodAnalyzer = new PortMethodAnalyzer();
        List<QueryMethodModel> queryMethods = new ArrayList<>();

        // Analyze each port method
        for (var portMethod : port.methods()) {
            Optional<QueryMethodModel> queryMethod = methodAnalyzer.analyzeMethod(portMethod);

            if (queryMethod.isPresent()) {
                queryMethods.add(queryMethod.get());

                // Log info diagnostic for detected query method
                context.diagnostics()
                        .report(Diagnostic.builder()
                                .severity(DiagnosticSeverity.INFO)
                                .code(JpaDiagnosticCodes.QUERY_METHOD_DETECTED)
                                .pluginId(PLUGIN_ID)
                                .message("Detected Spring Data query method: '"
                                        + portMethod.name() + "' in port '" + port.qualifiedName()
                                        + "'. Query type: " + queryMethod.get().queryType())
                                .build());
            }
        }

        return queryMethods;
    }
}
