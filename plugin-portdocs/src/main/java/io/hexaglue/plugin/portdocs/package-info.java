/**
 * HexaGlue plugin for generating port documentation in Markdown format.
 *
 * <p>This plugin analyzes all ports discovered by the HexaGlue compiler and generates
 * comprehensive Markdown documentation files for each port. The documentation includes:
 * <ul>
 *   <li>Port metadata (name, direction, type)</li>
 *   <li>Detailed method signatures</li>
 *   <li>Parameter and return type information</li>
 *   <li>Descriptions and annotations when available</li>
 * </ul>
 *
 * <p>Generated documentation files are placed in {@code docs/ports/} by default,
 * with one file per port named {@code {PortSimpleName}.md}.
 *
 * <p>This plugin serves as both a useful documentation tool and a reference
 * implementation for plugin authors learning to work with the HexaGlue SPI.
 *
 * @see io.hexaglue.plugin.portdocs.PortDocumentationPlugin
 */
package io.hexaglue.plugin.portdocs;
