# Contributing to HexaGlue JPA Plugin

Thank you for your interest in contributing to the HexaGlue JPA Repository Plugin! This guide will help you get started.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Project Structure](#project-structure)
5. [Coding Standards](#coding-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [Pull Request Process](#pull-request-process)
8. [Common Tasks](#common-tasks)
9. [Debugging Tips](#debugging-tips)

---

## Code of Conduct

This project follows the [HexaGlue Code of Conduct](../CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

---

## Getting Started

### Prerequisites

- **Java 17+** (for compilation)
- **Maven 3.9+** (build tool)
- **Git** (version control)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Fork and Clone

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:

```bash
git clone https://github.com/YOUR_USERNAME/hexaglue.git
cd hexaglue/hexaglue-plugin-jpa-repository
```

3. **Add upstream remote**:

```bash
git remote add upstream https://github.com/hexaglue/hexaglue.git
```

---

## Development Setup

### Building from Source

```bash
# Clean build
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build with verbose output
mvn clean install -X
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=IdTypeResolverTest

# Run specific test method
mvn test -Dtest=IdTypeResolverTest#shouldResolveLongIdFromFindById

# Run integration tests only
mvn test -Dtest=*IntegrationTest

# Run with coverage report
mvn clean test jacoco:report
# Report location: target/site/jacoco/index.html
```

### Code Formatting

This project uses **Palantir Java Format** via the Spotless Maven plugin.

```bash
# Apply formatting (run before committing!)
mvn spotless:apply

# Check formatting without modifying files
mvn spotless:check
```

**IMPORTANT**: All pull requests must pass `mvn spotless:check`. Run `mvn spotless:apply` before committing.

### Generating Javadoc

```bash
# Generate Javadoc for all modules
mvn javadoc:javadoc

# Open in browser
open target/site/apidocs/index.html
```

---

## Project Structure

```
hexaglue-plugin-jpa-repository/
├── src/main/java/                  # Source code
│   └── io/hexaglue/plugin/jpa/
│       ├── JpaRepositoryPlugin.java           # Main plugin entry point
│       ├── analysis/                          # Analysis components
│       ├── validation/                        # Validation components
│       ├── generator/                         # Code generators
│       ├── codegen/                           # Code templates
│       ├── model/                             # Internal data models
│       ├── config/                            # Configuration
│       ├── heuristics/                        # Detection heuristics
│       ├── util/                              # Utilities
│       └── diagnostics/                       # Diagnostic codes
├── src/test/java/                  # Test code
│   └── io/hexaglue/plugin/jpa/
│       ├── analysis/                          # Unit tests for analyzers
│       ├── validation/                        # Unit tests for validators
│       ├── generator/                         # Unit tests for generators
│       └── integration/                       # Integration tests
├── src/test/resources/             # Test resources
│   └── test-domain/                           # Test domain models
├── README.md                       # User documentation
├── ARCHITECTURE.md                 # Developer architecture guide
├── CONTRIBUTING.md                 # This file
└── pom.xml                         # Maven build configuration
```

---

## Coding Standards

### Java Style

Follow **HexaGlue coding conventions**:

- **Package structure**: `io.hexaglue.plugin.jpa.<module>`
- **Naming**:
  - Classes: `PascalCase` (e.g., `IdTypeResolver`)
  - Methods: `camelCase` (e.g., `resolveIdType`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_ID_STRATEGY`)
  - Private fields: `camelCase` (e.g., `generationPlan`)
- **Indentation**: 4 spaces (handled by Spotless)
- **Line length**: 120 characters max
- **No wildcard imports**: Use explicit imports

### Code Organization

1. **Group imports** by category:
   - Java standard library (`java.*`, `javax.*`)
   - Third-party libraries
   - HexaGlue modules
   - Current module

2. **Class member order**:
   - Static fields
   - Instance fields
   - Constructors
   - Public methods
   - Package-private methods
   - Private methods
   - Inner classes

3. **Method size**: Keep methods < 50 lines when possible

### Immutability

Prefer **immutable data structures**:

```java
// ✅ Good: Immutable record
public record IdModel(
    TypeRef unwrappedType,
    TypeRef originalType,
    IdGenerationStrategy strategy,
    String sequenceName,
    boolean isComposite
) {}

// ❌ Bad: Mutable class
public class IdModel {
    private TypeRef unwrappedType;  // Mutable!
    public void setUnwrappedType(TypeRef type) { ... }
}
```

### Null Safety

Use **Optional** for nullable return values:

```java
// ✅ Good
public Optional<IdModel> resolveId(PortView port) { ... }

// ❌ Bad
public IdModel resolveId(PortView port) { return null; }
```

Use `@Nullable` and `@NonNull` annotations when appropriate.

### Error Handling

**Never swallow exceptions**. Always report via `DiagnosticReporter`:

```java
// ✅ Good
if (invalidState) {
    Diagnostic error = DiagnosticFactory.error(
        DiagnosticCode.of("HG-JPA-INVALID-STATE"),
        "Detailed error message",
        sourceLocation,
        "io.hexaglue.plugin.jpa"
    );
    context.diagnostics().report(error);
    return Optional.empty();
}

// ❌ Bad
try {
    riskyOperation();
} catch (Exception e) {
    e.printStackTrace();  // Silent failure!
}
```

---

## Testing Guidelines

### Test Structure

Use **JUnit 5** with **@DisplayName** and **@Nested** for readability:

```java
@DisplayName("IdTypeResolver")
class IdTypeResolverTest {

    @Nested
    @DisplayName("Simple ID types")
    class SimpleIdTypesTests {

        @Test
        @DisplayName("should resolve Long ID from findById method")
        void shouldResolveLongIdFromFindById() {
            // Given
            PortView port = createPortWithFindById("java.lang.Long");

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals("java.lang.Long", result.unwrappedType().render());
        }
    }
}
```

### Test Naming

Follow **BDD-style naming**:

- Test class: `<ClassUnderTest>Test`
- Test method: `should<ExpectedBehavior>When<Condition>`

Examples:
- `shouldResolveLongIdFromFindById`
- `shouldUnwrapSingleFieldValueObject`
- `shouldDetectCompositeIdFromMultiFieldValueObject`

### Mocking

Use **Mockito** for mocking SPI interfaces:

```java
@BeforeEach
void setUp() {
    context = mock(GenerationContextSpec.class);
    IrView irModel = mock(IrView.class);
    DomainModelView domainModel = mock(DomainModelView.class);

    when(context.model()).thenReturn(irModel);
    when(irModel.domain()).thenReturn(domainModel);

    resolver = new IdTypeResolver(context, options);
}
```

### Assertions

Use **JUnit assertions** with descriptive messages:

```java
// ✅ Good
assertEquals("java.lang.Long", result.unwrappedType().render(),
    "Should unwrap CustomerId to Long");

// ❌ Bad
assertEquals("java.lang.Long", result.unwrappedType().render());
```

### Coverage Goals

Maintain **>80% code coverage** for:
- Analysis components
- Validation components
- Generation components

Run coverage report:

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## Pull Request Process

### 1. Create a Feature Branch

```bash
git checkout main
git pull upstream main
git checkout -b feature/add-custom-naming-strategy
```

Branch naming:
- Feature: `feature/description`
- Bug fix: `fix/issue-number-description`
- Documentation: `docs/description`

### 2. Make Your Changes

- Write code following coding standards
- Add tests for new functionality
- Update documentation if needed
- Run `mvn spotless:apply` before committing

### 3. Commit Your Changes

Write **clear, descriptive commit messages**:

```bash
git commit -m "feat(naming): add custom naming strategy for entities

- Add CustomNamingStrategy interface
- Implement DefaultNamingStrategy
- Add configuration option 'namingStrategy'
- Update EntityGenerator to use naming strategy
- Add unit tests for naming strategies

Closes #123"
```

**Commit message format**:
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

### 4. Push to Your Fork

```bash
git push origin feature/add-custom-naming-strategy
```

### 5. Create Pull Request

1. Go to GitHub and create a PR from your fork
2. Fill in the PR template with:
   - **Description**: What does this PR do?
   - **Motivation**: Why is this change needed?
   - **Testing**: How did you test it?
   - **Checklist**: Check all applicable items

### 6. Code Review

- Address reviewer feedback promptly
- Push additional commits to your branch
- Keep discussions focused and professional

### 7. Merge

Once approved, a maintainer will merge your PR. **Do not merge your own PR** unless you are a maintainer.

---

## Common Tasks

### Adding a New Code Generator

1. **Create generator class** in `generator/` package:

```java
package io.hexaglue.plugin.jpa.generator;

public class CustomArtifactGenerator {

    public void generate(JpaGenerationPlan plan, GenerationContextSpec context) {
        String packageName = plan.options().basePackage() + ".custom";
        String className = plan.entity().name() + "CustomArtifact";

        String content = generateContent(plan);

        SourceFile source = SourceFile.builder()
            .packageName(packageName)
            .className(className)
            .content(content)
            .mergeMode(plan.options().mergeMode())
            .build();

        context.output().writeSource(source);
    }

    private String generateContent(JpaGenerationPlan plan) {
        // Your code generation logic
        return "package " + packageName + ";\n\n" +
               "public class " + className + " { ... }";
    }
}
```

2. **Register in plugin**:

```java
// In JpaRepositoryPlugin.java
@Override
public void generate(GenerationContextSpec context) {
    CustomArtifactGenerator customGenerator = new CustomArtifactGenerator();

    for (JpaGenerationPlan plan : plans.values()) {
        entityGenerator.generate(plan, context);
        // ... other generators
        customGenerator.generate(plan, context);  // Add here
    }
}
```

3. **Add tests**:

```java
@Test
void shouldGenerateCustomArtifact() {
    // Given
    JpaGenerationPlan plan = createTestPlan();

    // When
    customGenerator.generate(plan, context);

    // Then
    verify(context.output()).writeSource(argThat(source ->
        source.className().equals("CustomerCustomArtifact")
    ));
}
```

### Adding a New Validator

1. **Create validator class** in `validation/` package:

```java
package io.hexaglue.plugin.jpa.validation;

public class CustomConstraintValidator {

    public void validate(JpaGenerationPlan plan, GenerationContextSpec context) {
        for (PropertyModel property : plan.properties()) {
            if (violatesConstraint(property)) {
                reportError(property, context);
            }
        }
    }

    private boolean violatesConstraint(PropertyModel property) {
        // Your validation logic
    }

    private void reportError(PropertyModel property, GenerationContextSpec context) {
        Diagnostic error = DiagnosticFactory.error(
            DiagnosticCode.of("HG-JPA-CUSTOM-CONSTRAINT"),
            "Property violates custom constraint: " + property.name(),
            property.sourceRef(),
            "io.hexaglue.plugin.jpa"
        );
        context.diagnostics().report(error);
    }
}
```

2. **Register in plugin**:

```java
// In JpaRepositoryPlugin.java
@Override
public void validate(GenerationContextSpec context) {
    CustomConstraintValidator customValidator = new CustomConstraintValidator();

    for (JpaGenerationPlan plan : plans.values()) {
        idStrategyValidator.validate(plan, context);
        // ... other validators
        customValidator.validate(plan, context);  // Add here
    }
}
```

3. **Add tests** (see Testing Guidelines above)

### Adding a Configuration Option

1. **Add field to `JpaPluginOptions`**:

```java
public record JpaPluginOptions(
    String basePackage,
    // ... existing fields
    boolean enableCustomFeature  // New option
) {
    // ...
}
```

2. **Add resolver in `resolve()` method**:

```java
public static JpaPluginOptions resolve(OptionsView.PluginOptionsView pluginOptions, GenerationContextSpec context) {
    // ... existing resolution

    boolean enableCustomFeature = pluginOptions.getOrDefault("enableCustomFeature", Boolean.class, false);

    return new JpaPluginOptions(
        basePackage,
        // ... existing parameters
        enableCustomFeature  // Add here
    );
}
```

3. **Document in README.md**:

```markdown
| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enableCustomFeature` | Boolean | `false` | Description of the feature |
```

4. **Add test**:

```java
@Test
void shouldResolveCustomFeatureOption() {
    when(pluginOptions.getOrDefault("enableCustomFeature", Boolean.class, false))
        .thenReturn(true);

    JpaPluginOptions options = JpaPluginOptions.resolve(pluginOptions, context);

    assertTrue(options.enableCustomFeature());
}
```

---

## Debugging Tips

### Debug Annotation Processing

1. **Enable Maven debug output**:

```bash
mvn clean compile -X > debug.log 2>&1
```

2. **Add debug logging** in plugin code:

```java
public class JpaRepositoryPlugin implements HexaGluePlugin {

    @Override
    public void analyze(GenerationContextSpec context) {
        System.err.println("JPA Plugin: analyze() called");
        System.err.println("Found " + plans.size() + " repository ports");
    }
}
```

3. **Remote debugging**:

Add to Maven command:
```bash
export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
mvn clean compile
```

Then attach debugger on port 5005.

### Inspect Generated Code

Generated code location:
```
target/generated-sources/annotations/
```

View generated files:
```bash
find target/generated-sources/annotations -name "*.java" -exec cat {} \;
```

### Test Compilation

Use `compile-testing` library to test annotation processing:

```java
@Test
void shouldCompileSuccessfully() {
    Compilation compilation = javac()
        .withProcessors(new HexaGlueProcessor())
        .compile(JavaFileObjects.forResource("Customer.java"));

    assertThat(compilation).succeededWithoutWarnings();
}
```

---

## Questions?

- **GitHub Discussions**: https://github.com/hexaglue/hexaglue/discussions
- **Slack**: Join #hexaglue-dev channel

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
