# Contributing to HexaGlue

> **📝 Documentation in Progress**
>
> These contributing guidelines are currently being written. Check back soon for complete contribution instructions.

---

Thank you for your interest in contributing to HexaGlue! We welcome contributions from the community.

---

## What This Guide Will Cover

This guide will provide comprehensive information on:

1. **Code of Conduct**
   - Community guidelines
   - Expected behavior
   - Enforcement

2. **Getting Started**
   - Finding issues to work on
   - Understanding the codebase
   - Setting up your development environment

3. **Contributing Code**
   - Forking and cloning
   - Creating feature branches
   - Writing tests
   - Code style and formatting

4. **Submitting Changes**
   - Commit message conventions
   - Pull request process
   - Code review expectations
   - Continuous integration

5. **Contributing Plugins**
   - Plugin contribution guidelines
   - Plugin quality standards
   - Documentation requirements
   - Publishing process

6. **Contributing Documentation**
   - Documentation style guide
   - Where to add documentation
   - Documentation review process

7. **Reporting Issues**
   - Bug report template
   - Feature request template
   - Security issues

---

## Quick Start for Contributors

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR-USERNAME/HexaGlue.git
cd HexaGlue

# Add upstream remote
git remote add upstream https://github.com/hexaglue/plugins.git
```

### 2. Create a Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/my-new-feature
```

### 3. Make Your Changes

- Write code following the existing style
- Add tests for new functionality
- Update documentation as needed
- Run `mvn spotless:apply` to format code

### 4. Test Your Changes

```bash
# Run all tests
mvn clean test

# Build the entire project
mvn clean install
```

### 5. Commit Your Changes

```bash
# Stage your changes
git add .

# Commit with a clear message
git commit -m "feat: add new feature X

- Detailed description of the change
- Why this change is needed
- Any breaking changes or migration notes"
```

### 6. Submit a Pull Request

```bash
# Push to your fork
git push origin feature/my-new-feature
```

Then open a pull request on GitHub.

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

- **Be respectful** - Treat everyone with respect
- **Be collaborative** - Work together constructively
- **Be inclusive** - Welcome diverse perspectives
- **Be professional** - Keep discussions focused on technical merit

### Enforcement

Instances of unacceptable behavior may be reported to info@hexaglue.io.

---

## Types of Contributions

We welcome many types of contributions:

- **Bug fixes** - Fix issues in the core or plugins
- **New features** - Add capabilities to the core
- **New plugins** - Create plugins for new frameworks or technologies
- **Documentation** - Improve guides, examples, and API docs
- **Tests** - Increase test coverage
- **Examples** - Create example projects
- **Performance** - Optimize compilation or generation

---

## Code Style

HexaGlue uses:
- **Java 17** language features
- **Palantir Java Format** (enforced via Spotless)
- **Javadoc** on all public APIs
- **Meaningful variable names**
- **Clear, concise comments** explaining the "why"

Run `mvn spotless:apply` before committing.

---

## Commit Message Convention

We follow conventional commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Example**:
```
feat(spi): add support for custom type mappings

- Added TypeMapper interface to SPI
- Plugins can now register custom type conversions
- Includes tests and documentation

Closes #123
```

---

## Pull Request Process

1. **Create a clear PR description**
   - What does this PR do?
   - Why is this change needed?
   - What testing was done?

2. **Ensure CI passes**
   - All tests must pass
   - Code must be formatted
   - No new warnings

3. **Request review**
   - Tag relevant maintainers
   - Address review comments
   - Keep the PR focused

4. **Merge**
   - Squash commits if requested
   - Maintainers will merge approved PRs

---

## Plugin Contribution Guidelines

If you're contributing a plugin:

1. **Follow the reference plugin** (`sample-plugin-port-docs`)
2. **Document all options** in plugin Javadoc and README
3. **Include tests** using `hexaglue-testing-harness`
4. **Add diagnostic codes** following HG-PLUGINID-xxx pattern
5. **Work with defaults** - plugin should work without configuration

See [Plugin Development Guide](doc/PLUGIN_DEVELOPMENT.md) for details.

---

## Need Help?

- **Questions**: Ask in [GitHub Discussions](https://github.com/hexaglue/plugins/discussions)
- **Bugs**: Report in [GitHub Issues](https://github.com/hexaglue/plugins/issues)

---

## License

By contributing to HexaGlue, you agree that your contributions will be licensed under the **Mozilla Public License 2.0 (MPL-2.0)**.

See [LICENSE](LICENSE) for details.

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
