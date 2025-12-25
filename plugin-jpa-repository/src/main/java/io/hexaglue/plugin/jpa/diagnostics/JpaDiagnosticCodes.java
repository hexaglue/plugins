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
package io.hexaglue.plugin.jpa.diagnostics;

import io.hexaglue.spi.diagnostics.DiagnosticCode;

/**
 * Centralized diagnostic codes for the JPA repository plugin.
 *
 * <h2>Code Structure</h2>
 * <pre>
 * HG-JPA-xxx
 * ├── 001-099: Informational messages
 * ├── 100-199: Warnings
 * └── 200-299: Errors
 * </pre>
 *
 * <h2>Best Practice</h2>
 * <p>Keeping all diagnostic codes in one place provides several benefits:</p>
 * <ul>
 *   <li>Easy to audit and document</li>
 *   <li>Prevents code duplication and typos</li>
 *   <li>Enables systematic error handling</li>
 *   <li>Facilitates documentation generation</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class JpaDiagnosticCodes {

    // ========================================
    // INFORMATIONAL MESSAGES (001-099)
    // ========================================

    /** Plugin started processing ports */
    public static final DiagnosticCode START = DiagnosticCode.of("HG-JPA-001");

    /** Port skipped (not repository-like) */
    public static final DiagnosticCode PORT_SKIPPED = DiagnosticCode.of("HG-JPA-010");

    /** Configuration successfully resolved */
    public static final DiagnosticCode CONFIG_RESOLVED = DiagnosticCode.of("HG-JPA-011");

    /** Artifacts generated successfully */
    public static final DiagnosticCode GENERATED = DiagnosticCode.of("HG-JPA-020");

    /** Query method pattern detected in port */
    public static final DiagnosticCode QUERY_METHOD_DETECTED = DiagnosticCode.of("HG-JPA-021");

    /** Plugin completed successfully */
    public static final DiagnosticCode COMPLETE = DiagnosticCode.of("HG-JPA-099");

    // ========================================
    // WARNINGS (100-199)
    // ========================================

    /** No driven ports found in project */
    public static final DiagnosticCode NO_PORTS = DiagnosticCode.of("HG-JPA-100");

    /** Domain type could not be inferred from port */
    public static final DiagnosticCode NO_DOMAIN_TYPE = DiagnosticCode.of("HG-JPA-110");

    /** ID type could not be inferred from port */
    public static final DiagnosticCode NO_ID_TYPE = DiagnosticCode.of("HG-JPA-111");

    /** Domain type not found in IR */
    public static final DiagnosticCode DOMAIN_TYPE_NOT_IN_IR = DiagnosticCode.of("HG-JPA-112");

    /** ID type is incompatible with generation strategy */
    public static final DiagnosticCode INCOMPATIBLE_ID_STRATEGY = DiagnosticCode.of("HG-JPA-120");

    /** Aggregate root detection heuristic may be inaccurate */
    public static final DiagnosticCode AGGREGATE_ROOT_HEURISTIC = DiagnosticCode.of("HG-JPA-130");

    /** Relationship detected between aggregates - should use ID-only reference */
    public static final DiagnosticCode INTER_AGGREGATE_RELATIONSHIP_WARNING = DiagnosticCode.of("HG-JPA-140");

    /** Inter-aggregate @ManyToOne detected - should use ID-only reference */
    public static final DiagnosticCode INTER_AGGREGATE_MANY_TO_ONE = DiagnosticCode.of("HG-JPA-141");

    /** Property type cannot be mapped to JPA - converter needed */
    public static final DiagnosticCode UNMAPPABLE_TYPE = DiagnosticCode.of("HG-JPA-150");

    // ========================================
    // ERRORS (200-299)
    // ========================================

    /** Plugin configuration is invalid */
    public static final DiagnosticCode INVALID_CONFIG = DiagnosticCode.of("HG-JPA-200");

    /** Generation failed for a port */
    public static final DiagnosticCode GENERATION_FAILED = DiagnosticCode.of("HG-JPA-201");

    /** Failed to write generated source file */
    public static final DiagnosticCode WRITE_FAILED = DiagnosticCode.of("HG-JPA-202");

    /** Composite ID detected but not supported yet */
    public static final DiagnosticCode COMPOSITE_ID_NOT_SUPPORTED = DiagnosticCode.of("HG-JPA-210");

    /** Circular relationship detected */
    public static final DiagnosticCode CIRCULAR_RELATIONSHIP = DiagnosticCode.of("HG-JPA-220");

    /** Invalid relationship configuration */
    public static final DiagnosticCode INVALID_RELATIONSHIP = DiagnosticCode.of("HG-JPA-221");

    private JpaDiagnosticCodes() {
        // Prevent instantiation
    }
}
