/**
 * HexaGlue Port Documentation Plugin.
 *
 * <p>
 * This plugin generates Markdown documentation files for all ports discovered
 * during compilation. Each port gets a dedicated documentation file describing
 * its contract, methods, and parameters.
 * </p>
 *
 * <p>
 * This module serves as a reference implementation demonstrating:
 * <ul>
 *   <li>How to implement a HexaGlue plugin</li>
 *   <li>How to access the IR (Intermediate Representation)</li>
 *   <li>How to generate documentation artifacts</li>
 *   <li>How to report diagnostics</li>
 * </ul>
 * </p>
 */
module io.hexaglue.plugin.portdocs {
    requires io.hexaglue.spi;

    // Provide HexaGluePlugin implementation
    provides io.hexaglue.spi.HexaGluePlugin with
            io.hexaglue.plugin.portdocs.PortDocumentationPlugin;
}
