# HexaGlue Plugin Development Guide

This guide explains how to create HexaGlue plugins that generate infrastructure code from analyzed domain models and ports.

**Target audience:** Plugin developers
**Prerequisites:** Understanding of Java, Maven, and annotation processing
**SPI Version:** 1

---

## Table of Contents

- [Quick Start](#quick-start)
- [Plugin Architecture](#plugin-architecture)
- [Implementing a Plugin](#implementing-a-plugin)
- [Accessing the IR](#accessing-the-ir)
- [Code Generation](#code-generation)
- [Configuration](#configuration)
- [Metadata Helpers](#metadata-helpers)
- [Diagnostic Reporting](#diagnostic-reporting)
- [Testing Your Plugin](#testing-your-plugin)
- [Best Practices](#best-practices)
- [Publishing Your Plugin](#publishing-your-plugin)

---

## Quick Start

### 1. Create a Maven Project

```xml
<project>
    <groupId>io.hexaglue</groupId>
    <artifactId>plugin-myname</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <!-- Dependency management -->
    <dependencyManagement>
        <dependencies>
            <!-- Import engine BOM for engine versions -->
            <dependency>
                <groupId>io.hexaglue</groupId>
                <artifactId>engine-bom</artifactId>
                <version>1.0.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Plugin API (ONLY dependency for runtime) -->
        <dependency>
            <groupId>io.hexaglue</groupId>
            <artifactId>engine-spi</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Testing utilities -->
        <dependency>
            <groupId>io.hexaglue</groupId>
            <artifactId>engine-testing-harness</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**Critical:** Plugins MUST depend ONLY on `engine-spi`, never on `engine-core`.

### 2. Implement HexaGluePlugin

```java
package io.hexaglue.plugin.myname;

import io.hexaglue.spi.*;
import io.hexaglue.spi.context.PluginContext;

public class MyPlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "io.hexaglue.plugin.myname";
    }

    @Override
    public PluginMetadata metadata() {
        return PluginMetadata.of(
            "MyPlugin",
            "Description of what my plugin does",
            HexaGlueVersion.of(0, 1, 0)
        );
    }

    @Override
    public PluginOrder order() {
        return PluginOrder.NORMAL;
    }

    @Override
    public void execute(PluginContext context) {
        // Access analyzed application
        AnalyzedApplication app = context.getAnalyzedApplication();

        // Generate code
        for (PortView port : app.getPorts()) {
            // ... generate infrastructure for this port
        }
    }
}
```

### 3. Register via ServiceLoader

Create `src/main/resources/META-INF/services/io.hexaglue.spi.HexaGluePlugin`:

```
io.hexaglue.plugin.myname.MyPlugin
```

### 4. Build and Use

```bash
mvn clean install
```

Then add your plugin to a project's annotation processor path.

---

## Plugin Architecture

### Plugin Lifecycle

```
┌─────────────────────────────────────┐
│  1. DISCOVERY (by engine-core)      │
│     ServiceLoader finds plugins     │
│     Plugins instantiated            │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  2. SORTING                         │
│     Plugins sorted by PluginOrder   │
│     - EARLY: Documentation, validation
│     - NORMAL: Most generators       │
│     - LATE: Aggregation, summaries  │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  3. EXECUTION (in order)            │
│     plugin.execute(context)         │
│     Access IR via context           │
│     Generate artifacts              │
└─────────────────────────────────────┘
```

### Plugin Interface

```java
public interface HexaGluePlugin {

    /**
     * Unique plugin identifier.
     * Convention: reverse domain name (e.g., "io.hexaglue.plugin.jpa")
     */
    String id();

    /**
     * Plugin metadata (name, description, version).
     */
    PluginMetadata metadata();

    /**
     * Execution order relative to other plugins.
     */
    PluginOrder order();

    /**
     * Execute plugin logic.
     * Called during GENERATE phase of compilation.
     */
    void execute(PluginContext context);
}
```

### Plugin Isolation

- Plugins execute in isolation
- No inter-plugin communication
- No shared state
- Each plugin sees the same IR snapshot
- Execution order within same `PluginOrder` is undefined

---

## Implementing a Plugin

### Plugin Structure

```
plugin-myname/
├── src/main/java/
│   └── io/hexaglue/plugin/myname/
│       ├── MyPlugin.java                  (main plugin class)
│       ├── generators/
│       │   ├── EntityGenerator.java       (code generators)
│       │   └── RepositoryGenerator.java
│       ├── templates/
│       │   └── TemplateRenderer.java      (optional templates)
│       └── util/
│           └── NamingStrategy.java        (utilities)
├── src/main/resources/
│   └── META-INF/services/
│       └── io.hexaglue.spi.HexaGluePlugin (ServiceLoader registration)
└── src/test/java/
    └── io/hexaglue/plugin/myname/
        └── MyPluginTest.java              (compilation tests)
```

### Example: Minimal Plugin

```java
package io.hexaglue.plugin.docs;

import io.hexaglue.spi.*;
import io.hexaglue.spi.context.PluginContext;
import io.hexaglue.spi.ir.PortView;
import java.io.IOException;

public class PortDocsPlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "io.hexaglue.plugin.portdocs";
    }

    @Override
    public PluginMetadata metadata() {
        return PluginMetadata.of(
            "Port Documentation Plugin",
            "Generates Markdown documentation for ports",
            HexaGlueVersion.of(0, 1, 0)
        );
    }

    @Override
    public PluginOrder order() {
        return PluginOrder.EARLY;  // Documentation first
    }

    @Override
    public void execute(PluginContext context) {
        AnalyzedApplication app = context.getAnalyzedApplication();

        for (PortView port : app.getPorts()) {
            try {
                generateDocumentation(port, context);
            } catch (IOException e) {
                context.getDiagnostics().error(
                    "HG-PORTDOCS-200",
                    "Failed to generate documentation for " + port.simpleName(),
                    e
                );
            }
        }
    }

    private void generateDocumentation(PortView port, PluginContext context)
            throws IOException {
        String markdown = buildMarkdown(port);
        String fileName = "docs/ports/" + port.simpleName() + ".md";

        context.getFiler().createResource(fileName, markdown);

        context.getDiagnostics().info(
            "HG-PORTDOCS-001",
            "Generated documentation: " + fileName
        );
    }

    private String buildMarkdown(PortView port) {
        // Build Markdown content...
        return "# " + port.simpleName() + "\n\n...";
    }
}
```

---

## Accessing the IR

### PluginContext

The `PluginContext` provides access to everything a plugin needs:

```java
public interface PluginContext {

    /** Analyzed application (IR views) */
    AnalyzedApplication getAnalyzedApplication();

    /** Plugin configuration options */
    PluginOptionsView getOptions();

    /** Diagnostic reporter */
    DiagnosticReporter getDiagnostics();

    /** File generation API */
    Filer getFiler();

    /** Processing environment */
    ProcessingEnvironment getProcessingEnv();
}
```

### AnalyzedApplication

Access different parts of the IR:

```java
AnalyzedApplication app = context.getAnalyzedApplication();

// Get all ports
List<PortView> allPorts = app.getPorts();

// Filter by direction
List<PortView> drivenPorts = app.getPorts().stream()
    .filter(p -> p.direction() == PortDirection.DRIVEN)
    .collect(toList());

// Get domain types
List<DomainTypeView> domainTypes = app.getDomainTypes();

// Filter by kind
List<DomainTypeView> aggregates = app.getDomainTypes().stream()
    .filter(DomainTypeView::isAggregateRoot)
    .collect(toList());
```

### PortView

Access port details:

```java
PortView port = ...;

String qualifiedName = port.qualifiedName();
String simpleName = port.simpleName();
String packageName = port.packageName();
PortDirection direction = port.direction();  // DRIVING or DRIVEN

// Methods
for (MethodView method : port.methods()) {
    String methodName = method.name();
    TypeRef returnType = method.returnType();
    List<ParameterView> params = method.parameters();
    Optional<String> javadoc = method.documentation();
}

// Annotations
Optional<AnnotationView> repo = port.annotations()
    .findFirst("org.jmolecules.ddd.annotation.Repository");
```

### DomainTypeView

Access domain type details:

```java
DomainTypeView type = ...;

String qualifiedName = type.qualifiedName();
DomainTypeKind kind = type.kind();  // AGGREGATE_ROOT, ENTITY, VALUE_OBJECT
boolean isAggregate = type.isAggregateRoot();
boolean isImmutable = type.isImmutable();

// Properties
for (PropertyView prop : type.properties()) {
    String name = prop.name();
    TypeRef propType = prop.type();
    boolean isNullable = prop.isNullable();
}

// Identity
Optional<PropertyView> id = type.identity();
```

### TypeRef

Work with types:

```java
TypeRef type = method.returnType();

// Type information
String qualifiedName = type.qualifiedName();
String simpleName = type.simpleName();
boolean isPrimitive = type.isPrimitive();
boolean isNullable = type.isNullable();

// Collections
if (type instanceof GenericTypeRef generic) {
    TypeRef elementType = generic.typeArguments().get(0).type();
}

// Rendering
String code = TypeDisplay.qualifiedName(type);  // "java.util.List<com.example.Customer>"
```

📖 **For complete IR reference**, see [Engine Architecture](https://github.com/hexaglue/engine/blob/main/ARCHITECTURE.md#intermediate-representation)

---

## Code Generation

### Filer API

Generate files using the `Filer`:

```java
Filer filer = context.getFiler();

// Generate source file
filer.createSourceFile(
    "com.example.infrastructure.CustomerAdapter",
    javaCode
);

// Generate resource file
filer.createResource(
    "docs/ports/CustomerRepository.md",
    markdownContent
);
```

### Merge Modes

Control how generated files are merged with existing content:

```java
public enum MergeMode {
    OVERWRITE,     // Replace entire file
    APPEND,        // Append to existing file
    SKIP_EXISTING  // Keep existing file, don't regenerate
}
```

Configure via `hexaglue.yaml`:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.myname:
      mergeMode: OVERWRITE
```

### Code Generation Utilities

Use JavaPoet or similar libraries for type-safe code generation:

```xml
<dependency>
    <groupId>com.squareup</groupId>
    <artifactId>javapoet</artifactId>
    <version>1.13.0</version>
</dependency>
```

Example:

```java
import com.squareup.javapoet.*;

TypeSpec adapter = TypeSpec.classBuilder("CustomerRepositoryAdapter")
    .addModifiers(Modifier.PUBLIC)
    .addAnnotation(Component.class)
    .addSuperinterface(ClassName.get("com.example.ports", "CustomerRepository"))
    .addField(jpaRepositoryField())
    .addMethod(findByIdMethod())
    .build();

JavaFile javaFile = JavaFile.builder("com.example.infrastructure", adapter)
    .build();

filer.createSourceFile(
    "com.example.infrastructure.CustomerRepositoryAdapter",
    javaFile.toString()
);
```

---

## Configuration

### Reading Configuration

Access plugin-specific configuration:

```java
@Override
public void execute(PluginContext context) {
    PluginOptionsView options = context.getOptions();

    // Get simple option
    String basePackage = options.getString("basePackage")
        .orElse("com.example.infrastructure");

    // Get boolean option
    boolean enableAuditing = options.getBoolean("enableAuditing")
        .orElse(true);

    // Get nested configuration
    Optional<Map<String, Object>> typesConfig =
        options.getMap("types");
}
```

### Configuration Structure

Users configure plugins in `hexaglue.yaml`:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.myname:
      basePackage: com.example.infrastructure
      enableFeature: true
      customOption: value

      # Nested configuration
      types:
        com.example.domain.Customer:
          tableName: customers
          properties:
            name:
              column:
                length: 100
```

### Default Values

Always provide sensible defaults:

```java
String basePackage = options.getString("basePackage")
    .orElse("generated");  // ← Default value

boolean enabled = options.getBoolean("enableFeature")
    .orElse(true);  // ← Enabled by default
```

---

## Metadata Helpers

The SPI provides helpers for accessing property-level metadata.

### PropertyMetadataHelper

Access hierarchical property configuration:

```java
import io.hexaglue.spi.options.PropertyMetadataHelper;

// Get column length for Customer.name
Optional<Integer> length = PropertyMetadataHelper.getPropertyMetadata(
    options,
    "com.example.domain.Customer",  // type
    "name",                          // property
    "column",                        // metadata category
    "length",                        // metadata key
    Integer.class                    // expected type
);

// Get entire column configuration
Map<String, Object> columnConfig = PropertyMetadataHelper.getPropertyConfig(
    options,
    "com.example.domain.Customer",
    "name",
    "column"
);
```

This reads from:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      types:
        com.example.domain.Customer:
          properties:
            name:
              column:              # ← metadata category
                length: 100        # ← metadata key
                nullable: false
```

### RelationshipMetadataHelper

Access relationship configuration:

```java
import io.hexaglue.spi.options.RelationshipMetadataHelper;

Optional<String> relationshipKind = RelationshipMetadataHelper.getRelationshipMetadata(
    options,
    "com.example.domain.Order",
    "customerId",
    "kind",
    String.class
);
```

### Evidence-Based Resolution

Use the resolution pattern: **Annotations → YAML → Heuristics → Defaults**

```java
// 1. Check annotations
Optional<Integer> fromAnnotation = getFromColumnAnnotation(property);
if (fromAnnotation.isPresent()) {
    return fromAnnotation.get();
}

// 2. Check YAML configuration
Optional<Integer> fromYaml = PropertyMetadataHelper.getPropertyMetadata(...);
if (fromYaml.isPresent()) {
    return fromYaml.get();
}

// 3. Apply heuristics
if (property.type().equals("String") && property.name().endsWith("Email")) {
    return 255;  // Email heuristic
}

// 4. Use default
return 50;  // Default column length
```

---

## Diagnostic Reporting

### Reporting Diagnostics

Use structured diagnostic codes:

```java
DiagnosticReporter diagnostics = context.getDiagnostics();

// Info
diagnostics.info("HG-MYPLUGIN-001", "Starting generation");

// Warning
diagnostics.warn(
    "HG-MYPLUGIN-100",
    "No configuration found, using defaults"
);

// Error
diagnostics.error(
    "HG-MYPLUGIN-200",
    "Failed to generate adapter for " + port.simpleName()
);

// Error with exception
diagnostics.error(
    "HG-MYPLUGIN-201",
    "I/O error while writing file",
    ioException
);
```

### Diagnostic Code Convention

```
HG-{PLUGIN}-{NUMBER}

Where:
- PLUGIN: Your plugin identifier (uppercase, e.g., MYPLUGIN)
- NUMBER:
  - 001-099: INFO messages
  - 100-199: WARN messages
  - 200-299: ERROR messages
```

Example codes for `plugin-jpa`:
- `HG-JPA-001` - "Generating JPA entity for X"
- `HG-JPA-100` - "No @Id field found, using heuristics"
- `HG-JPA-200` - "Failed to generate entity"

### Best Practices

1. **Be specific** - Include entity/file names in messages
2. **Be actionable** - Tell users how to fix problems
3. **Use appropriate severity**:
   - INFO: Normal progress
   - WARN: Potential issues, fallback used
   - ERROR: Generation failed

```java
// Good
diagnostics.error(
    "HG-JPA-200",
    "Failed to generate JPA entity for aggregate 'Customer': " +
    "No identity field found. Add @Identity annotation or use 'id' field name."
);

// Bad
diagnostics.error("HG-JPA-200", "Error");
```

---

## Testing Your Plugin

### Using CompilationTestCase

Test your plugin with the testing harness:

```java
import io.hexaglue.testing.CompilationTestCase;
import org.junit.jupiter.api.Test;

class MyPluginTest {

    @Test
    void shouldGenerateAdapter() {
        String portCode = """
            package com.example.ports;

            public interface CustomerRepository {
                Customer save(Customer customer);
            }
            """;

        CompilationTestCase.run()
            .withSourceFile("CustomerRepository.java", portCode)
            .withProcessor(MyPlugin.class)
            .expectSuccess()
            .expectGeneratedFile("com.example.infrastructure.CustomerRepositoryAdapter.java")
            .verify();
    }

    @Test
    void shouldReportError() {
        String invalidCode = """
            package com.example;

            public class Invalid {
                // No ports defined
            }
            """;

        CompilationTestCase.run()
            .withSourceFile("Invalid.java", invalidCode)
            .withProcessor(MyPlugin.class)
            .expectDiagnostic("HG-MYPLUGIN-100", "No ports found")
            .verify();
    }
}
```

### Testing Configuration

Test configuration parsing:

```java
@Test
void shouldUseConfiguredPackage() {
    String config = """
        hexaglue:
          plugins:
            io.hexaglue.plugin.myname:
              basePackage: custom.package
        """;

    CompilationTestCase.run()
        .withSourceFile("Port.java", portCode)
        .withConfig(config)
        .withProcessor(MyPlugin.class)
        .expectGeneratedFile("custom.package.Adapter.java")
        .verify();
}
```

📖 **For complete testing documentation**, see [engine-testing-harness/README.md](https://github.com/hexaglue/engine/tree/main/engine-testing-harness)

---

## Best Practices

### 1. Follow SPI-Only Rule

**NEVER depend on `engine-core`** - only on `engine-spi`.

```xml
<!-- GOOD -->
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>engine-spi</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- BAD - Will break -->
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>engine-core</artifactId>
</dependency>
```

### 2. Provide Sensible Defaults

Your plugin should work with zero configuration:

```java
String basePackage = options.getString("basePackage")
    .orElse("generated.infrastructure");  // Sensible default
```

### 3. Handle Errors Gracefully

Don't fail the entire compilation for one bad element:

```java
for (PortView port : ports) {
    try {
        generateAdapter(port, context);
    } catch (Exception e) {
        // Report error but continue with other ports
        diagnostics.error(
            "HG-MYPLUGIN-200",
            "Failed to generate adapter for " + port.simpleName(),
            e
        );
    }
}
```

### 4. Document Your Configuration

Provide examples in your plugin README:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.myname:
      # Base package for generated code (default: generated.infrastructure)
      basePackage: com.example.infrastructure

      # Enable feature X (default: true)
      enableFeature: true
```

### 5. Use Structured Logging

Use diagnostic codes consistently:

```java
// Start of phase
diagnostics.info("HG-MYPLUGIN-001", "Starting adapter generation");

// Progress
for (Port port : ports) {
    diagnostics.info("HG-MYPLUGIN-002", "Generating adapter for " + port.name());
}

// Completion
diagnostics.info("HG-MYPLUGIN-003", "Generated " + count + " adapters");
```

---

## Publishing Your Plugin

### 1. Package Your Plugin

```bash
mvn clean package
```

This creates `target/plugin-myname-0.1.0-SNAPSHOT.jar` with:
- Compiled classes
- `META-INF/services/io.hexaglue.spi.HexaGluePlugin`
- Resources

### 2. Install Locally

```bash
mvn install
```

### 3. Publish to Maven Central

Add Maven Central publishing configuration to your POM.

### 4. Document Your Plugin

Create a `README.md` for your plugin:

```markdown
# MyPlugin

Description of what your plugin does.

## Installation

\`\`\`xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>plugin-myname</artifactId>
    <version>0.1.0</version>
</dependency>
\`\`\`

## Configuration

\`\`\`yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.myname:
      option1: value1
\`\`\`

## Examples

...
```

---

## Reference Implementations

Study these plugins as examples:

- [plugin-portdocs](plugin-portdocs/) - Simple documentation generator
- [plugin-jpa-repository](plugin-jpa-repository/) - Advanced JPA code generation with metadata helpers

---

## Related Documentation

- [Engine Architecture](https://github.com/hexaglue/engine/blob/main/ARCHITECTURE.md) - Understanding the IR and type system
- [Engine SPI](https://github.com/hexaglue/engine/tree/main/engine-spi) - Complete SPI reference
- [Engine Testing Harness](https://github.com/hexaglue/engine/tree/main/engine-testing-harness) - Testing utilities

---

## Support

- [GitHub Issues](https://github.com/hexaglue/plugins/issues) - Bug reports and feature requests
- [GitHub Discussions](https://github.com/hexaglue/plugins/discussions) - Plugin development questions

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
