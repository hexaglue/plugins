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
package io.hexaglue.plugin.portdocs;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.PluginMetadata;
import io.hexaglue.spi.PluginOrder;
import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.MergeMode;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.ports.PortView;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * HexaGlue plugin that generates Markdown documentation for discovered ports.
 *
 * <p>This plugin is intentionally used as a reference for contributors. It demonstrates:
 * <ul>
 *   <li>Stable plugin metadata</li>
 *   <li>Plugin-scoped option retrieval from {@code hexaglue.yaml}</li>
 *   <li>Diagnostics reporting with structured codes</li>
 *   <li>Resilient generation (continue when one port fails)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Example configuration:</p>
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.portdocs:
 *       outputDir: docs/hex/
 *       mergeMode: OVERWRITE
 * }</pre>
 *
 * <p><strong>Stability note:</strong> this plugin must work even when no YAML file is provided,
 * relying on safe defaults.</p>
 *
 * <h2>Diagnostic Codes</h2>
 * <pre>{@code
 * HG-PORTDOCS-xxx
 * ├── 001-099: Informational messages
 * ├── 100-199: Warnings
 * └── 200-299: Errors
 * }</pre>
 */
public final class PortDocumentationPlugin implements HexaGluePlugin {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.portdocs";
    private static final String PLUGIN_NAME = "Port Documentation Generator";
    private static final String PLUGIN_VERSION = "1.0.0";
    private static final String PLUGIN_DESCRIPTION = "Generates Markdown documentation files for all discovered ports";
    private static final String PLUGIN_VENDOR = "Scalastic";
    private static final String PLUGIN_WEBSITE_URL = "https://hexaglue.io";
    private static final HexaGlueVersion PLUGIN_MINIMAL_HEXAGLUE_VERSION = HexaGlueVersion.of(0, 2, 0);
    private static final Set<String> PLUGIN_CAPABILITIES = Set.of("documentation", "ports");

    /**
     * Threshold for considering a port interface "large".
     *
     * <p><strong>Intent:</strong> this is guidance only. We emit an INFO diagnostic to help
     * users notice "fat ports" and potentially decompose them.</p>
     */
    private static final int LARGE_PORT_METHOD_COUNT = 10;

    /**
     * Default output directory when no configuration is provided.
     */
    private static final String DEFAULT_OUTPUT_DIR = "docs/ports/";

    /**
     * Default merge mode when no configuration is provided (or when configuration is invalid).
     */
    private static final MergeMode DEFAULT_MERGE_MODE = MergeMode.OVERWRITE;

    /**
     * Resolved output directory (computed once per {@link #apply(GenerationContextSpec)} call).
     *
     * <p><strong>BEST PRACTICE:</strong> avoid reading options repeatedly inside per-port loops.</p>
     */
    private String outputDir;

    /**
     * Resolved merge mode (computed once per {@link #apply(GenerationContextSpec)} call).
     */
    private MergeMode mergeMode;

    /**
     * Diagnostic codes used by this plugin.
     *
     * <p><strong>BEST PRACTICE:</strong> keep all codes in one place:
     * <ul>
     *   <li>easy to audit</li>
     *   <li>easy to refactor</li>
     *   <li>prevents typos</li>
     * </ul>
     */
    private static final class DiagnosticCodes {
        // Informational messages (001-099)
        static final DiagnosticCode GENERATION_START = DiagnosticCode.of("HG-PORTDOCS-001");
        static final DiagnosticCode PORT_GENERATED = DiagnosticCode.of("HG-PORTDOCS-002");
        static final DiagnosticCode GENERATION_COMPLETE = DiagnosticCode.of("HG-PORTDOCS-003");

        // Optional informational/debug-style messages (still INFO, but more "plumbing")
        static final DiagnosticCode SETTINGS_RESOLVED = DiagnosticCode.of("HG-PORTDOCS-011");
        static final DiagnosticCode WRITING_FILE = DiagnosticCode.of("HG-PORTDOCS-012");

        // Warnings (100-199)
        static final DiagnosticCode NO_PORTS_FOUND = DiagnosticCode.of("HG-PORTDOCS-100");
        static final DiagnosticCode EMPTY_PORT = DiagnosticCode.of("HG-PORTDOCS-101");
        static final DiagnosticCode LARGE_PORT_INTERFACE = DiagnosticCode.of("HG-PORTDOCS-102");
        static final DiagnosticCode INVALID_MERGE_MODE = DiagnosticCode.of("HG-PORTDOCS-110");

        // Errors (200-299)
        static final DiagnosticCode GENERATION_FAILED = DiagnosticCode.of("HG-PORTDOCS-200");
        static final DiagnosticCode WRITE_FAILED = DiagnosticCode.of("HG-PORTDOCS-201");

        private DiagnosticCodes() {
            // Prevent instantiation
        }
    }

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
                PLUGIN_WEBSITE_URL,
                PLUGIN_VERSION,
                PLUGIN_MINIMAL_HEXAGLUE_VERSION,
                PLUGIN_CAPABILITIES);
    }

    @Override
    public PluginOrder order() {
        // Documentation generation can happen at any order; NORMAL is sufficient here.
        return PluginOrder.NORMAL;
    }

    @Override
    public void apply(GenerationContextSpec context) {
        Objects.requireNonNull(context, "context");

        // ─────────────────────────────────────────────────────────────────────
        // BEST PRACTICE: Resolve plugin options once
        // ─────────────────────────────────────────────────────────────────────
        //
        // Intent:
        // - Options are plugin-scoped, so we read them via options().forPlugin(PLUGIN_ID).
        // - The plugin MUST work without any YAML file, therefore defaults are mandatory.
        // - We normalize values (outputDir trailing slash) to prevent path bugs.
        //
        var opt = context.options().forPlugin(PLUGIN_ID);

        this.outputDir = normalizeOutputDir(opt.getOrDefault("outputDir", String.class, DEFAULT_OUTPUT_DIR));

        String mergeModeRaw = opt.getOrDefault("mergeMode", String.class, DEFAULT_MERGE_MODE.name());
        this.mergeMode = parseMergeModeOrDefault(mergeModeRaw, context);

        // BEST PRACTICE: Emit an INFO diagnostic that summarizes resolved settings.
        // Intent:
        // - Helps users verify their YAML is being taken into account.
        // - Helps maintainers debug "why is it still generating in the default folder?"
        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(DiagnosticCodes.SETTINGS_RESOLVED)
                        .message("PortDocs resolved outputDir=" + this.outputDir + ", mergeMode=" + this.mergeMode)
                        .pluginId(PLUGIN_ID)
                        .build());

        // ─────────────────────────────────────────────────────────────────────
        // BEST PRACTICE: Read inputs from the architecture model (ports)
        // ─────────────────────────────────────────────────────────────────────
        //
        // Intent:
        // - HexaGlue plugins should only depend on the stable SPI view.
        // - Do not touch compiler internals from plugins.
        //
        List<PortView> ports = context.model().ports().allPorts();

        // BEST PRACTICE: Handle the "nothing to do" case explicitly.
        // Intent:
        // - In real projects, an empty port list usually indicates missing sources, wrong module,
        //   or misconfigured discovery rules. A WARNING makes it noticeable without failing builds.
        if (ports.isEmpty()) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(DiagnosticCodes.NO_PORTS_FOUND)
                            .message("No ports found in project. Documentation generation skipped.")
                            .pluginId(PLUGIN_ID)
                            .build());
            return;
        }

        // BEST PRACTICE: Report informational message at the start.
        // Intent:
        // - Lets users know the plugin is active and doing work.
        // - Useful when multiple plugins run in the same compilation.
        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(DiagnosticCodes.GENERATION_START)
                        .message(String.format("Generating documentation for %d port(s)", ports.size()))
                        .pluginId(PLUGIN_ID)
                        .build());

        // BEST PRACTICE: Track success/failure in a resilient loop.
        // Intent:
        // - One bad port should not prevent docs for other ports.
        // - This approach yields a better developer experience.
        int successCount = 0;

        for (PortView port : ports) {
            try {
                generatePortDocumentation(context, port);
                successCount++;
            } catch (Exception e) {
                // BEST PRACTICE: Report structured errors with port context and exception cause.
                context.diagnostics()
                        .report(Diagnostic.builder()
                                .severity(DiagnosticSeverity.ERROR)
                                .code(DiagnosticCodes.GENERATION_FAILED)
                                .message(String.format(
                                        "Failed to generate documentation for port '%s': %s",
                                        port.qualifiedName(), safeMessage(e)))
                                .pluginId(PLUGIN_ID)
                                .cause(e)
                                .build());
                // BEST PRACTICE: Continue with the next port instead of failing fast.
            }
        }

        // BEST PRACTICE: Report final summary.
        // Intent:
        // - Gives a clear outcome without requiring log spelunking.
        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(DiagnosticCodes.GENERATION_COMPLETE)
                        .message(String.format(
                                "Port documentation generation complete (%d/%d file(s) generated successfully)",
                                successCount, ports.size()))
                        .pluginId(PLUGIN_ID)
                        .build());
    }

    /**
     * Generates documentation for a single port.
     *
     * <p><strong>BEST PRACTICE:</strong> keep per-port generation self-contained and deterministic:
     * <ul>
     *   <li>Validate input early and report meaningful diagnostics</li>
     *   <li>Generate content before I/O (fail fast)</li>
     *   <li>Use deterministic paths</li>
     *   <li>Wrap output write failures with actionable error diagnostics</li>
     * </ul>
     *
     * <p><strong>Intent:</strong> this method is the place where contributors should look
     * to understand “how to generate one artifact per model element”.</p>
     *
     * @param context generation context providing output and diagnostics
     * @param port the port to document
     */
    private void generatePortDocumentation(GenerationContextSpec context, PortView port) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(port, "port");

        // BEST PRACTICE: Check for edge cases and report warnings.
        // Intent:
        // - An empty port is valid (placeholder), but likely unintended.
        // - We warn but still generate a file (minimal doc), so users see it.
        if (port.methods().isEmpty()) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(DiagnosticCodes.EMPTY_PORT)
                            .message(String.format(
                                    "Port '%s' has no methods. Generated minimal documentation.", port.simpleName()))
                            .pluginId(PLUGIN_ID)
                            .build());
        }

        // BEST PRACTICE: Report informational diagnostics for unusual but valid cases.
        // Intent:
        // - Large ports may still be correct, so this is INFO (guidance), not WARNING.
        // - Helps drive architectural discussions without breaking builds.
        if (port.methods().size() >= LARGE_PORT_METHOD_COUNT) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.INFO)
                            .code(DiagnosticCodes.LARGE_PORT_INTERFACE)
                            .message(String.format(
                                    "Port '%s' has %d methods (large interface). Consider decomposition for maintainability.",
                                    port.simpleName(), port.methods().size()))
                            .pluginId(PLUGIN_ID)
                            .build());
        }

        // BEST PRACTICE: Generate content first (fail fast before I/O).
        // Intent:
        // - If content generation fails, we avoid producing partial files.
        String markdownContent = MarkdownPortDocumentationGenerator.generateDocumentation(port);

        // BEST PRACTICE: Use clear, deterministic output paths.
        // Intent:
        // - Determinism is critical for generated code/docs (stable diffs, reproducible builds).
        // - outputDir is resolved once from options in apply(), and normalized with trailing slash.
        String outputPath = this.outputDir + port.simpleName() + ".md";

        // BEST PRACTICE: Use builders for artifact descriptors (clear, extensible API).
        DocFile docFile = DocFile.builder()
                .path(outputPath)
                .content(markdownContent)
                .mergeMode(this.mergeMode)
                .build();

        // BEST PRACTICE: Optional "plumbing" INFO diagnostic before writing.
        // Intent:
        // - Extremely helpful when debugging output paths (as in your case).
        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(DiagnosticCodes.WRITING_FILE)
                        .message("PortDocs writing DocFile.path=" + outputPath)
                        .pluginId(PLUGIN_ID)
                        .build());

        // BEST PRACTICE: Wrap output writes with specific error handling.
        // Intent:
        // - Convert opaque IO exceptions into actionable diagnostics.
        // - Keep the exception as cause for maintainers.
        try {
            context.output().write(docFile);
        } catch (Exception e) {
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.ERROR)
                            .code(DiagnosticCodes.WRITE_FAILED)
                            .message(String.format(
                                    "Failed to write documentation file '%s': %s", outputPath, safeMessage(e)))
                            .pluginId(PLUGIN_ID)
                            .cause(e)
                            .build());
            throw e;
        }

        // BEST PRACTICE: Report success with file location.
        // Intent:
        // - Users should know exactly where the artifact ended up.
        context.diagnostics()
                .report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.INFO)
                        .code(DiagnosticCodes.PORT_GENERATED)
                        .message(String.format(
                                "Generated documentation for port '%s' at %s", port.simpleName(), outputPath))
                        .pluginId(PLUGIN_ID)
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers (option normalization + parsing)
    // ─────────────────────────────────────────────────────────────────────────

    private static String normalizeOutputDir(String dir) {
        String d = dir == null ? "" : dir.trim();
        if (d.isEmpty()) {
            d = DEFAULT_OUTPUT_DIR;
        }
        // BEST PRACTICE: normalize directory-like options to avoid accidental "docs/hexFoo.md".
        if (!d.endsWith("/")) {
            d = d + "/";
        }
        return d;
    }

    private static MergeMode parseMergeModeOrDefault(String raw, GenerationContextSpec context) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return DEFAULT_MERGE_MODE;
        }
        try {
            return MergeMode.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            // BEST PRACTICE: invalid config should not crash the plugin.
            // Intent:
            // - warn the user
            // - fall back to a safe default
            context.diagnostics()
                    .report(Diagnostic.builder()
                            .severity(DiagnosticSeverity.WARNING)
                            .code(DiagnosticCodes.INVALID_MERGE_MODE)
                            .message("Invalid mergeMode '" + raw + "', using " + DEFAULT_MERGE_MODE)
                            .pluginId(PLUGIN_ID)
                            .cause(ex)
                            .build());
            return DEFAULT_MERGE_MODE;
        }
    }

    private static String safeMessage(Throwable t) {
        if (t == null) return "Unknown error";
        String m = t.getMessage();
        if (m == null || m.trim().isEmpty()) {
            return t.getClass().getSimpleName();
        }
        return m;
    }
}
