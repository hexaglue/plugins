# HexaGlue Plugins

**Official infrastructure generation plugins for HexaGlue Engine.**

<div align="center">
  <img src="doc/logo-hexaglue-plugins.png" alt="HexaGlue" width="400">
</div>

[![CI](https://github.com/hexaglue/plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/hexaglue/plugins/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

---

## What are HexaGlue Plugins?

HexaGlue plugins are modules that **generate infrastructure code** from your domain model and port interfaces. They leverage the [HexaGlue Engine](https://github.com/hexaglue/engine)'s compile-time analysis to produce adapters, configurations, and wiring code for various technology stacks.

**Key Features:**
- Zero boilerplate in your domain code
- Type-safe code generation at compile time
- Plugin-specific configuration via `hexaglue.yaml`
- Extensible architecture for any technology stack

---

## Available Plugins

### Official Plugins

| Plugin | Status | Description |
|--------|--------|-------------|
| **plugin-portdocs** | ✅ Available | Generates Markdown documentation for all ports |
| **plugin-jpa-repository** | ✅ Available | JPA entities, Spring Data repositories, adapters, and MapStruct mappers |

### Plugin Documentation

Each plugin has detailed documentation:

- [**plugin-portdocs**](plugin-portdocs/README.md) - Port documentation generation
- [**plugin-jpa-repository**](plugin-jpa-repository/README.md) - JPA persistence layer generation

---

## Quick Start

### Installation

Add plugins to your Maven build configuration:

```xml
<project>
    <!-- Import BOMs for version management -->
    <dependencyManagement>
        <dependencies>
            <!-- HexaGlue Engine BOM -->
            <dependency>
                <groupId>io.hexaglue</groupId>
                <artifactId>engine-bom</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- HexaGlue Plugins BOM -->
            <dependency>
                <groupId>io.hexaglue</groupId>
                <artifactId>plugins-bom</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.1</version>
                <configuration>
                    <annotationProcessorPaths>
                        <!-- HexaGlue Engine (required) -->
                        <path>
                            <groupId>io.hexaglue</groupId>
                            <artifactId>engine-core</artifactId>
                        </path>

                        <!-- Add the plugins you need -->
                        <path>
                            <groupId>io.hexaglue</groupId>
                            <artifactId>plugin-portdocs</artifactId>
                        </path>
                        <path>
                            <groupId>io.hexaglue</groupId>
                            <artifactId>plugin-jpa-repository</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Configuration

Create `hexaglue.yaml` in your project root or `src/main/resources/`:

```yaml
hexaglue:
  plugins:
    # Port Documentation Plugin
    io.hexaglue.plugin.portdocs:
      outputDir: docs/ports/
      mergeMode: OVERWRITE

    # JPA Plugin
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      idStrategy: ASSIGNED
      enableAuditing: true
      enableSoftDelete: true
      enableOptimisticLocking: true
```

### Build Your Project

```bash
mvn clean compile
```

---

## Plugin: Port Documentation

**Generates Markdown documentation for all discovered ports.**

Generates:
- One `.md` file per port interface
- Method signatures with parameter and return types
- Javadoc comments from your ports
- Port classification (driving vs driven)

📖 **For complete documentation and examples**, see [plugin-portdocs/README.md](plugin-portdocs/README.md)

---

## Plugin: JPA Repository

**Generates JPA persistence layer from your ports and domain model.**

Generates for each repository port:

1. **JPA Entity** - `@Entity` class with JPA annotations
2. **Spring Data Repository** - `JpaRepository` interface
3. **Adapter** - Implementation connecting port to Spring Data repository
4. **MapStruct Mapper** - Bidirectional mapping between domain and entity
5. **AttributeConverters** - For value objects (single-field)
6. **Embeddable Classes** - For value objects (multi-field)

📖 **For complete documentation, configuration options, and examples**, see [plugin-jpa-repository/README.md](plugin-jpa-repository/README.md)

---

## Plugin Configuration

### Configuration File Location

Place `hexaglue.yaml` in one of these locations:

1. Project root: `./hexaglue.yaml`
2. Resources directory: `src/main/resources/hexaglue.yaml`
3. Any location on the compile-time classpath

### Configuration Structure

```yaml
hexaglue:
  plugins:
    {plugin-id}:
      # Plugin-specific options
```

### Plugin IDs

- **Port Documentation**: `io.hexaglue.plugin.portdocs`
- **JPA Repository**: `io.hexaglue.plugin.jpa`

### Default Behavior

All plugins work with **sensible defaults** if no configuration is provided. Configuration is optional and used to customize behavior.

### Configuration Options

For plugin-specific configuration options, see:
- [plugin-portdocs/README.md](plugin-portdocs/README.md) - Port documentation configuration
- [plugin-jpa-repository/README.md](plugin-jpa-repository/README.md) - JPA plugin configuration

---

## Diagnostic Codes

Plugins use structured diagnostic codes following the pattern:

```
HG-{PLUGIN}-{NUMBER}
```

### Port Documentation Plugin

- `HG-PORTDOCS-001` to `HG-PORTDOCS-099` - Informational
- `HG-PORTDOCS-100` to `HG-PORTDOCS-199` - Warnings
- `HG-PORTDOCS-200` to `HG-PORTDOCS-299` - Errors

### JPA Plugin

- `HG-JPA-001` to `HG-JPA-099` - Informational
- `HG-JPA-100` to `HG-JPA-199` - Warnings
- `HG-JPA-200` to `HG-JPA-299` - Errors

---

## Creating Your Own Plugin

Want to create a custom plugin?

📖 **For plugin development guide**, see the [engine repository](https://github.com/hexaglue/engine) for:
- Plugin SPI reference
- Testing harness usage ([engine-testing-harness/README.md](https://github.com/hexaglue/engine/tree/main/engine-testing-harness))
- Best practices and examples

**Reference implementations** in this repository:
- [plugin-portdocs](plugin-portdocs/) - Simple plugin demonstrating documentation generation
- [plugin-jpa-repository](plugin-jpa-repository/) - Advanced plugin demonstrating code generation

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/hexaglue/plugins.git
cd plugins

# Build all plugins
mvn clean install

# Run tests
mvn test
```

---

## Examples

📖 **Complete working examples** using these plugins are available in the [examples repository](https://github.com/hexaglue/examples).

See the [examples README](https://github.com/hexaglue/examples) for learning resources and usage examples.

---

## Contributing

We welcome plugin contributions! To contribute:

1. Fork this repository
2. Create a feature branch
3. Follow the plugin development guidelines
4. Submit a pull request

See each plugin's README for development guidelines.

---

## License

HexaGlue Plugins are distributed under the **Mozilla Public License 2.0 (MPL-2.0)**.

- ✅ May be used in commercial and proprietary products
- ✅ Modifications to HexaGlue source files must be shared under MPL-2.0
- ✅ Your application code remains your own and may remain proprietary

Learn more: [https://www.mozilla.org/MPL/2.0/](https://www.mozilla.org/MPL/2.0/)

---

## Support

- [GitHub Issues](https://github.com/hexaglue/plugins/issues) - Report bugs or request features
- [GitHub Discussions](https://github.com/hexaglue/plugins/discussions) - Ask questions and share ideas

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
