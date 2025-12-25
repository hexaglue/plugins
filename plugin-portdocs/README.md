# Port Documentation Plugin - Sample Plugin for Contributors

This plugin serves as a **reference implementation** demonstrating best practices for HexaGlue plugin development, with special focus on **diagnostic reporting**.

## Purpose

While this plugin generates useful Markdown documentation for ports, its primary purpose is to **teach contributors** how to write high-quality HexaGlue plugins that provide excellent user experience through proper diagnostic reporting.

## What This Plugin Demonstrates

### 1. **Structured Diagnostic Codes** ✅

All diagnostic codes follow the HexaGlue convention: `HG-<PLUGIN>-<NUMBER>`

```java
private static final class DiagnosticCodes {
    // Informational messages (001-099)
    static final DiagnosticCode GENERATION_START = DiagnosticCode.of("HG-PORTDOCS-001");
    static final DiagnosticCode PORT_GENERATED = DiagnosticCode.of("HG-PORTDOCS-002");

    // Warnings (100-199)
    static final DiagnosticCode NO_PORTS_FOUND = DiagnosticCode.of("HG-PORTDOCS-100");
    static final DiagnosticCode EMPTY_PORT = DiagnosticCode.of("HG-PORTDOCS-101");

    // Errors (200-299)
    static final DiagnosticCode GENERATION_FAILED = DiagnosticCode.of("HG-PORTDOCS-200");
}
```

**Why this matters:**
- ✅ Type-safe (no typos in code strings)
- ✅ Easy to refactor (IDE can find all usages)
- ✅ Clear documentation (all codes in one place)
- ✅ Easy to audit (prevents duplicate codes)

### 2. **Code Organization by Severity** ✅

Diagnostic codes are organized in numeric ranges by severity:

| Range | Severity | Purpose |
|-------|----------|---------|
| 001-099 | INFO | Informational messages (progress, success) |
| 100-199 | WARNING | Warnings (edge cases, recommendations) |
| 200-299 | ERROR | Errors (failures that prevent generation) |

This convention makes it easy to:
- Understand severity at a glance
- Avoid code collisions
- Document codes systematically

### 3. **Edge Case Handling** ✅

The plugin demonstrates how to handle common edge cases gracefully:

```java
// Example: No ports found
if (ports.isEmpty()) {
    context.diagnostics().report(Diagnostic.builder()
        .severity(DiagnosticSeverity.WARNING)
        .code(DiagnosticCodes.NO_PORTS_FOUND)
        .message("No ports found in project. Documentation generation skipped.")
        .pluginId(PLUGIN_ID)
        .build());
    return;
}
```

**Edge cases covered:**
- ✅ No ports found in the project
- ✅ Ports with no methods (empty interfaces)
- ✅ Large ports that might need decomposition
- ✅ File write failures
- ✅ Content generation failures

### 4. **Graceful Error Handling** ✅

Errors don't cause the entire build to fail:

```java
for (PortView port : ports) {
    try {
        generatePortDocumentation(context, port);
        successCount++;
    } catch (Exception e) {
        // Report error but continue processing other ports
        context.diagnostics().report(/* error diagnostic */);
        // DON'T re-throw - let other ports succeed
    }
}
```

**Benefits:**
- ✅ One bad port doesn't stop documentation for others
- ✅ Users get partial results instead of total failure
- ✅ Better developer experience

### 5. **Informative Success Messages** ✅

Every successful operation reports what was done:

```java
context.diagnostics().report(Diagnostic.builder()
    .severity(DiagnosticSeverity.INFO)
    .code(DiagnosticCodes.PORT_GENERATED)
    .message(String.format(
        "Generated documentation for port '%s' at %s",
        port.simpleName(),
        outputPath))
    .pluginId(PLUGIN_ID)
    .build());
```

**Why this matters:**
- ✅ Users know exactly what was generated
- ✅ Output paths are explicit (easy to find files)
- ✅ Progress is transparent

### 6. **Meaningful Context in Errors** ✅

Error messages include all relevant context:

```java
context.diagnostics().report(Diagnostic.builder()
    .severity(DiagnosticSeverity.ERROR)
    .code(DiagnosticCodes.GENERATION_FAILED)
    .message(String.format(
        "Failed to generate documentation for port '%s': %s",
        port.qualifiedName(),  // Which port failed
        e.getMessage()))       // Why it failed
    .pluginId(PLUGIN_ID)
    .cause(e)                  // Full exception for debugging
    .build());
```

**Context included:**
- ✅ What operation failed (generation, write, etc.)
- ✅ Which entity was being processed (port name)
- ✅ Why it failed (exception message)
- ✅ Full stack trace (via `.cause()`)

### 7. **Summary Statistics** ✅

Final messages provide clear summaries:

```java
context.diagnostics().report(Diagnostic.builder()
    .severity(DiagnosticSeverity.INFO)
    .code(DiagnosticCodes.GENERATION_COMPLETE)
    .message(String.format(
        "Port documentation generation complete (%d/%d file(s) generated successfully)",
        successCount,
        ports.size()))
    .build());
```

**Benefits:**
- ✅ Users know exactly what succeeded vs. failed
- ✅ Easy to spot partial failures (e.g., 3/5 succeeded)
- ✅ Clear indication of completion

## Diagnostic Code Reference

See `doc/DIAGNOSTIC_CODES.md` for the complete catalog of codes used by this plugin.

| Code | Severity | When Used |
|------|----------|-----------|
| HG-PORTDOCS-001 | INFO | Starting documentation generation |
| HG-PORTDOCS-002 | INFO | Successfully generated documentation for a port |
| HG-PORTDOCS-003 | INFO | Completed documentation generation |
| HG-PORTDOCS-100 | WARNING | No ports found in the project |
| HG-PORTDOCS-101 | WARNING | Port has no methods |
| HG-PORTDOCS-102 | INFO | Port has many methods (suggestion to decompose) |
| HG-PORTDOCS-200 | ERROR | Failed to generate documentation for a port |
| HG-PORTDOCS-201 | ERROR | Failed to write documentation file |

## Best Practices for Plugin Authors

When writing your own HexaGlue plugins, follow these patterns from this sample:

### ✅ DO: Define Diagnostic Codes as Constants

```java
private static final class DiagnosticCodes {
    static final DiagnosticCode MY_CODE = DiagnosticCode.of("HG-MYPLUGIN-001");
}
```

### ✅ DO: Use the Plugin ID in All Diagnostics

```java
.pluginId(PLUGIN_ID)  // Always include this
```

### ✅ DO: Include Exception Causes for Errors

```java
.cause(e)  // Attach the exception for debugging
```

### ✅ DO: Continue Processing After Errors

```java
try {
    processItem(item);
} catch (Exception e) {
    reportError(e);
    // DON'T re-throw - continue with next item
}
```

### ✅ DO: Provide Actionable Messages

```java
// GOOD: Specific and actionable
"Failed to write file 'Customer.java': Permission denied"

// BAD: Vague and unhelpful
"An error occurred"
```

### ❌ DON'T: Use Magic Strings for Codes

```java
// BAD
DiagnosticCode.of("MY_CODE")

// GOOD
DiagnosticCodes.MY_CODE
```

### ❌ DON'T: Fail Silently

```java
// BAD
catch (Exception e) {
    // Do nothing
}

// GOOD
catch (Exception e) {
    context.diagnostics().report(/* error */);
}
```

### ❌ DON'T: Skip Success Reporting

```java
// BAD
context.output().write(file);
// No diagnostic

// GOOD
context.output().write(file);
context.diagnostics().report(/* success message */);
```

## Running the Plugin

This plugin is automatically discovered and executed during HexaGlue compilation.

To enable debug logging and see all diagnostics:

```bash
mvn clean compile -Ahexaglue.debug=true
```

## Generated Documentation

Documentation files are generated in `docs/ports/` with the naming pattern:

```
docs/ports/{PortSimpleName}.md
```

For example, a port `com.example.CustomerRepository` will generate:

```
docs/ports/CustomerRepository.md
```

## Usage

Simply add this plugin to your HexaGlue-based project's classpath:

```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-plugin-port-docs</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

## Testing the Plugin

See the test cases in `src/test/java/` for examples of:
- Testing with mock ports
- Verifying diagnostic reporting
- Edge case handling

## Contributing

When contributing to HexaGlue, use this plugin as a template for:
- Diagnostic code organization
- Error handling patterns
- User-facing message quality
- Edge case coverage

## License

MPL 2.0 - See LICENSE file

## Contact

For questions about plugin development best practices:
- Docs: https://hexaglue.io/docs/plugin-development

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
