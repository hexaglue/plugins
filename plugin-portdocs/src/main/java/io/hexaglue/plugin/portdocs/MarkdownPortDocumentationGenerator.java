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

import io.hexaglue.spi.ir.ports.PortDirection;
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.ir.ports.PortView;

/**
 * Utility class for generating Markdown documentation for ports.
 *
 * <p>This generator creates human-readable documentation that describes
 * port contracts in Hexagonal Architecture terms.</p>
 */
final class MarkdownPortDocumentationGenerator {

    private MarkdownPortDocumentationGenerator() {
        // Utility class
    }

    /**
     * Generates Markdown documentation for a single port.
     *
     * @param port the port to document (never {@code null})
     * @return Markdown content (never blank)
     */
    static String generateDocumentation(PortView port) {
        StringBuilder md = new StringBuilder();

        // Title
        md.append("# Port: ").append(port.simpleName()).append("\n\n");

        // Overview section
        md.append("## Overview\n\n");
        md.append("**Qualified Name:** `").append(port.qualifiedName()).append("`\n\n");
        md.append("**Direction:** ").append(formatDirection(port.direction())).append("\n\n");
        md.append("**Type:** `").append(port.type().render()).append("`\n\n");

        // Port description if available
        port.description().ifPresent(desc -> {
            md.append("**Description:** ").append(desc).append("\n\n");
        });

        // Port ID if available
        port.portId().ifPresent(id -> {
            md.append("**Port ID:** `").append(id).append("`\n\n");
        });

        // Direction explanation
        md.append("### Direction Explanation\n\n");
        if (port.direction() == PortDirection.DRIVING) {
            md.append(
                            "This is a **Driving Port** (Inbound). It expresses what the application offers to the outside world.\n")
                    .append(
                            "Driving adapters (e.g., REST controllers, CLI commands) will translate external protocols ")
                    .append("into calls to this port.\n\n");
        } else {
            md.append(
                            "This is a **Driven Port** (Outbound). It expresses what the application requires from external systems.\n")
                    .append(
                            "Driven adapters (e.g., database repositories, external API clients) will implement this port.\n\n");
        }

        // Methods section
        md.append("## Methods\n\n");

        if (port.methods().isEmpty()) {
            md.append("*No methods declared in this port.*\n\n");
        } else {
            md.append("This port declares **").append(port.methods().size()).append("** method(s):\n\n");

            for (int i = 0; i < port.methods().size(); i++) {
                PortMethodView method = port.methods().get(i);
                md.append("### ")
                        .append(i + 1)
                        .append(". ")
                        .append(method.name())
                        .append("\n\n");

                // Method signature
                md.append("**Signature:**\n\n```java\n");
                md.append(formatMethodSignature(method));
                md.append("\n```\n\n");

                // Method properties
                if (method.isDefault() || method.isStatic()) {
                    md.append("**Properties:**\n\n");
                    if (method.isDefault()) {
                        md.append("- Default method (has implementation)\n");
                    }
                    if (method.isStatic()) {
                        md.append("- Static method\n");
                    }
                    md.append("\n");
                }

                // Method description if available
                method.description().ifPresent(desc -> {
                    md.append("**Description:** ").append(desc).append("\n\n");
                });

                // Signature ID if available
                method.signatureId().ifPresent(sigId -> {
                    md.append("**Signature ID:** `").append(sigId).append("`\n\n");
                });

                // Parameters
                if (!method.parameters().isEmpty()) {
                    md.append("**Parameters:**\n\n");
                    for (PortParameterView param : method.parameters()) {
                        md.append("- `")
                                .append(param.name())
                                .append("`: `")
                                .append(param.type().render())
                                .append("`");
                        if (param.isVarArgs()) {
                            md.append(" (varargs)");
                        }
                        param.description().ifPresent(desc -> {
                            md.append(" - ").append(desc);
                        });
                        md.append("\n");
                    }
                    md.append("\n");
                }

                // Return type
                md.append("**Return Type:** `")
                        .append(method.returnType().render())
                        .append("`\n\n");
            }
        }

        // Footer
        md.append("---\n\n");
        md.append("*Generated by HexaGlue Port Documentation Plugin*\n");

        return md.toString();
    }

    /**
     * Formats a port direction for display.
     */
    private static String formatDirection(PortDirection direction) {
        return switch (direction) {
            case DRIVING -> "Driving (Inbound)";
            case DRIVEN -> "Driven (Outbound)";
        };
    }

    /**
     * Formats a complete method signature as Java code.
     */
    private static String formatMethodSignature(PortMethodView method) {
        StringBuilder sig = new StringBuilder();

        // Modifiers
        if (method.isStatic()) {
            sig.append("static ");
        }
        if (method.isDefault()) {
            sig.append("default ");
        }

        // Return type
        sig.append(method.returnType().render()).append(" ");

        // Method name
        sig.append(method.name());

        // Parameters
        sig.append("(");
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) {
                sig.append(", ");
            }
            PortParameterView param = method.parameters().get(i);
            sig.append(param.type().render());
            if (param.isVarArgs()) {
                sig.append("...");
            }
            sig.append(" ").append(param.name());
        }
        sig.append(")");

        return sig.toString();
    }
}
