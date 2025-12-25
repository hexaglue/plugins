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
package io.hexaglue.plugin.jpa;

import io.hexaglue.plugin.jpa.analysis.JpaGenerationPlanBuilder;
import io.hexaglue.plugin.jpa.analysis.PortAnalyzer;
import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.diagnostics.JpaDiagnosticCodes;
import io.hexaglue.plugin.jpa.generator.AdapterGenerator;
import io.hexaglue.plugin.jpa.generator.ConverterGenerator;
import io.hexaglue.plugin.jpa.generator.EmbeddableGenerator;
import io.hexaglue.plugin.jpa.generator.EntityGenerator;
import io.hexaglue.plugin.jpa.generator.MapperGenerator;
import io.hexaglue.plugin.jpa.generator.RepositoryGenerator;
import io.hexaglue.plugin.jpa.model.JpaGenerationPlan;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.PluginMetadata;
import io.hexaglue.spi.PluginOrder;
import io.hexaglue.spi.codegen.SourceFile;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.ports.PortDirection;
import io.hexaglue.spi.ir.ports.PortView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * HexaGlue plugin that generates Spring Data JPA persistence artifacts for DRIVEN ports.
 *
 * <p>This plugin implements the complete JPA Repository generation pipeline:</p>
 * <ol>
 *   <li><strong>Analysis</strong>: Detect repository ports, analyze domain types and IDs</li>
 *   <li><strong>Planning</strong>: Build generation plan with all metadata</li>
 *   <li><strong>Generation</strong>: Generate 4 artifacts per port:
 *     <ul>
 *       <li>JPA Entity (@Entity class)</li>
 *       <li>Spring Data Repository (JpaRepository interface)</li>
 *       <li>MapStruct Mapper (domain ↔ entity conversion)</li>
 *       <li>Adapter (port implementation)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Validation</strong>: ID strategy compatibility, type mappability</li>
 *   <li><strong>Features</strong>: Auditing, soft delete, optimistic locking</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       basePackage: com.example.infrastructure.persistence
 *       mergeMode: OVERWRITE
 *       idStrategy: ASSIGNED  # or IDENTITY, SEQUENCE, AUTO, UUID
 *       enableAuditing: true
 *       enableOptimisticLocking: true
 *       entitySuffix: Entity
 *       adapterSuffix: Adapter
 * }</pre>
 *
 * @since 0.4.0
 */
public final class JpaRepositoryPlugin implements HexaGluePlugin {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";
    private static final String PLUGIN_NAME = "JPA Repository Generator (Spring Data)";
    private static final String PLUGIN_VERSION = "1.0.0-SNAPSHOT";
    private static final String PLUGIN_DESCRIPTION =
            "Generates Spring Data JPA adapters, entities, mappers, and repositories from DRIVEN ports.";
    private static final String PLUGIN_VENDOR = "Scalastic";
    private static final String PLUGIN_WEBSITE = "https://hexaglue.io";
    private static final HexaGlueVersion MIN_HEXAGLUE_VERSION = HexaGlueVersion.of(0, 4, 0);
    private static final Set<String> CAPABILITIES = Set.of("persistence", "jpa", "spring-data");

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                PLUGIN_ID,
                PLUGIN_NAME,
                PLUGIN_DESCRIPTION,
                PLUGIN_VENDOR,
                PLUGIN_WEBSITE,
                PLUGIN_VERSION,
                MIN_HEXAGLUE_VERSION,
                CAPABILITIES);
    }

    @Override
    public PluginOrder order() {
        return PluginOrder.NORMAL;
    }

    @Override
    public void apply(GenerationContextSpec context) {
        Objects.requireNonNull(context, "context");

        // Step 1: Resolve configuration
        JpaPluginOptions options;
        try {
            options = JpaPluginOptions.resolve(context.options().forPlugin(PLUGIN_ID), context);
        } catch (Exception e) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.ERROR)
                            .code(JpaDiagnosticCodes.INVALID_CONFIG)
                            .pluginId(PLUGIN_ID)
                            .message("Invalid JPA plugin configuration: " + e.getMessage())
                            .cause(e)
                            .build());
            return;
        }

        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(JpaDiagnosticCodes.CONFIG_RESOLVED)
                        .pluginId(PLUGIN_ID)
                        .message(String.format(
                                "JPA plugin configured: basePackage=%s, idStrategy=%s, auditing=%s",
                                options.basePackage(),
                                options.idStrategy(),
                                options.featureFlags().enableAuditing()))
                        .build());

        // Step 2: Find repository-like ports
        List<PortView> drivenPorts = context.model().ports().allPorts(PortDirection.DRIVEN);
        List<PortView> repositoryPorts =
                drivenPorts.stream().filter(PortAnalyzer::looksLikeRepository).toList();

        if (repositoryPorts.isEmpty()) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(JpaDiagnosticCodes.NO_PORTS)
                            .pluginId(PLUGIN_ID)
                            .message("No repository-like DRIVEN ports found. JPA generation skipped.")
                            .build());
            return;
        }

        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(JpaDiagnosticCodes.START)
                        .pluginId(PLUGIN_ID)
                        .message(String.format(
                                "Generating JPA artifacts for %d repository port(s)", repositoryPorts.size()))
                        .build());

        // Step 3: Generate artifacts for each port
        int successCount = 0;
        for (PortView port : repositoryPorts) {
            try {
                if (generateArtifacts(context, port, options)) {
                    successCount++;
                }
            } catch (Exception e) {
                context.diagnostics()
                        .report(Diagnostic.builder()
                                .severity(DiagnosticSeverity.ERROR)
                                .code(JpaDiagnosticCodes.GENERATION_FAILED)
                                .pluginId(PLUGIN_ID)
                                .message(String.format(
                                        "Failed to generate artifacts for port '%s': %s",
                                        port.qualifiedName(), e.getMessage()))
                                .cause(e)
                                .build());
            }
        }

        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(JpaDiagnosticCodes.COMPLETE)
                        .pluginId(PLUGIN_ID)
                        .message(String.format(
                                "JPA generation complete (%d/%d port(s) processed)",
                                successCount, repositoryPorts.size()))
                        .build());
    }

    /**
     * Generates all 4 artifacts for a single port.
     */
    private boolean generateArtifacts(GenerationContextSpec context, PortView port, JpaPluginOptions options) {
        // Step 1: Analyze port and build generation plan
        JpaGenerationPlanBuilder planBuilder = new JpaGenerationPlanBuilder(context, options);
        JpaGenerationPlan plan = planBuilder.build(port);

        // Step 2: Create generators
        EntityGenerator entityGen = new EntityGenerator(options);
        RepositoryGenerator repoGen =
                new RepositoryGenerator(options.featureFlags().generateQueryMethods());
        MapperGenerator mapperGen = new MapperGenerator(context);
        AdapterGenerator adapterGen = new AdapterGenerator(context);
        EmbeddableGenerator embeddableGen = new EmbeddableGenerator(options.basePackage());
        ConverterGenerator converterGen = new ConverterGenerator(options.basePackage());

        // Step 3: Generate Value Object support classes
        List<SourceFile> files = new ArrayList<>();
        generateEmbeddablesForEntity(context, plan, embeddableGen, options.mergeMode(), files);
        generateConvertersForEntity(context, plan, converterGen, options.mergeMode(), files);

        // Step 4: Generate main artifacts
        files.add(entityGen.generate(plan.entityModel(), options.mergeMode()));
        files.add(repoGen.generate(plan, options.mergeMode()));
        files.add(mapperGen.generate(plan, options.mergeMode()));
        files.add(adapterGen.generate(plan, options.mergeMode()));

        // Step 4: Write files
        for (SourceFile file : files) {
            try {
                context.output().write(file);
            } catch (Exception e) {
                context.diagnostics()
                        .report(Diagnostic.builder()
                                .severity(DiagnosticSeverity.ERROR)
                                .code(JpaDiagnosticCodes.WRITE_FAILED)
                                .pluginId(PLUGIN_ID)
                                .message(String.format(
                                        "Failed to write file '%s': %s", file.qualifiedTypeName(), e.getMessage()))
                                .cause(e)
                                .build());
                throw e;
            }
        }

        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(JpaDiagnosticCodes.GENERATED)
                        .pluginId(PLUGIN_ID)
                        .message(String.format(
                                "Generated JPA artifacts for port '%s': entity=%s, adapter=%s",
                                port.qualifiedName(), plan.entityQualifiedName(), plan.adapterQualifiedName()))
                        .build());

        return true;
    }

    /**
     * Generates @Embeddable classes for all multi-field Value Objects found in the entity.
     *
     * @param context generation context
     * @param plan JPA generation plan
     * @param embeddableGen embeddable generator
     * @param mergeMode merge mode for files
     * @param files list to add generated files to
     */
    private void generateEmbeddablesForEntity(
            GenerationContextSpec context,
            JpaGenerationPlan plan,
            EmbeddableGenerator embeddableGen,
            io.hexaglue.spi.codegen.MergeMode mergeMode,
            List<SourceFile> files) {

        // Track processed VO types to avoid duplicates
        java.util.Set<String> processedVoTypes = new java.util.HashSet<>();

        // Step 1: Check if ID is composite and generate its embeddable
        if (plan.entityModel().idModel().isComposite()) {
            String idTypeName = plan.entityModel().idModel().originalType().render();
            Optional<io.hexaglue.spi.ir.domain.DomainTypeView> idTypeOpt =
                    context.model().domain().findType(idTypeName);

            if (idTypeOpt.isPresent()) {
                // Generate embeddable with Serializable and equals/hashCode for composite ID
                SourceFile embeddableFile = embeddableGen.generate(idTypeOpt.get(), mergeMode, true);
                files.add(embeddableFile);
                processedVoTypes.add(idTypeName);
            }
        }

        // Step 2: Scan entity properties for embedded Value Objects
        for (io.hexaglue.plugin.jpa.model.PropertyModel property :
                plan.entityModel().properties()) {
            if (property.embedded()) {
                String voTypeName = property.type().render();

                // Generate embeddable only once per VO type
                if (!processedVoTypes.contains(voTypeName)) {
                    Optional<io.hexaglue.spi.ir.domain.DomainTypeView> voTypeOpt =
                            context.model().domain().findType(voTypeName);

                    if (voTypeOpt.isPresent()) {
                        // Regular embedded VOs don't need Serializable/equals/hashCode
                        SourceFile embeddableFile = embeddableGen.generate(voTypeOpt.get(), mergeMode, false);
                        files.add(embeddableFile);
                        processedVoTypes.add(voTypeName);
                    }
                }
            }
        }
    }

    /**
     * Generates JPA AttributeConverter classes for single-field Value Objects found in the entity.
     *
     * <p>Converters are generated with autoApply=false, allowing manual application via @Convert.</p>
     *
     * @param context generation context
     * @param plan JPA generation plan
     * @param converterGen converter generator
     * @param mergeMode merge mode for files
     * @param files list to add generated files to
     */
    private void generateConvertersForEntity(
            GenerationContextSpec context,
            JpaGenerationPlan plan,
            ConverterGenerator converterGen,
            io.hexaglue.spi.codegen.MergeMode mergeMode,
            List<SourceFile> files) {

        // Track processed VO types to avoid duplicates
        java.util.Set<String> processedVoTypes = new java.util.HashSet<>();

        // Also generate converters for single-field VOs used by MapStruct
        // (These are currently handled by MapStruct mappers, but converters provide an alternative)
        String domainTypeName = plan.entityModel().domainType().render();
        Optional<io.hexaglue.spi.ir.domain.DomainTypeView> domainTypeOpt =
                context.model().domain().findType(domainTypeName);

        if (domainTypeOpt.isEmpty()) {
            return;
        }

        io.hexaglue.spi.ir.domain.DomainTypeView domainType = domainTypeOpt.get();

        // Scan properties for single-field Value Objects (not embedded, not already processed)
        for (io.hexaglue.spi.ir.domain.DomainPropertyView property : domainType.properties()) {
            String propertyTypeName = property.type().render();
            Optional<io.hexaglue.spi.ir.domain.DomainTypeView> propertyDomainType =
                    context.model().domain().findType(propertyTypeName);

            if (propertyDomainType.isPresent()) {
                io.hexaglue.spi.ir.domain.DomainTypeView voType = propertyDomainType.get();

                // Single-field Value Object (excluding IDs which are unwrapped in entities)
                if ((voType.kind() == io.hexaglue.spi.ir.domain.DomainTypeKind.RECORD
                                || voType.kind() == io.hexaglue.spi.ir.domain.DomainTypeKind.IDENTIFIER)
                        && voType.properties().size() == 1
                        && !property.name().equalsIgnoreCase("id")
                        && !processedVoTypes.contains(voType.qualifiedName())) {

                    try {
                        SourceFile converterFile = converterGen.generate(voType, mergeMode);
                        files.add(converterFile);
                        processedVoTypes.add(voType.qualifiedName());
                    } catch (IllegalArgumentException e) {
                        // Skip if converter generation fails (e.g., multi-field VO)
                    }
                }
            }
        }
    }
}
