# HexaGlue JPA Repository Plugin

> Generate Spring Data JPA infrastructure from your Hexagonal Architecture ports and domain model.

## Overview

The **HexaGlue JPA Repository Plugin** automatically generates complete JPA persistence infrastructure for hexagonal architecture applications. It analyzes your domain model and repository ports to produce:

- **JPA Entities** with proper annotations (@Entity, @Table, @Id, etc.)
- **Spring Data JPA Repositories** implementing your port interfaces
- **JPA Adapters** bridging ports to Spring Data repositories
- **MapStruct Mappers** for domain ↔ entity conversion
- **AttributeConverters** for single-field value objects
- **Embeddable classes** for multi-field value objects

### Key Features

- ✅ **Zero boilerplate**: No manual JPA code writing
- ✅ **Type-safe**: Preserves domain type safety through converters
- ✅ **Value object unwrapping**: Automatic persistence mapping for VOs
- ✅ **Relationship detection**: @OneToMany, @ManyToOne, @ElementCollection
- ✅ **Audit support**: Automatic @CreatedDate, @LastModifiedDate fields
- ✅ **Soft delete**: Optional soft delete with @Where annotation
- ✅ **Optimistic locking**: @Version field generation
- ✅ **Query method generation**: Derived query methods from port signatures

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-plugin-jpa-repository</artifactId>
    <version>${hexaglue.version}</version>
</dependency>
```

### 2. Configure Plugin

Create `hexaglue.yaml` in your project root:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      idStrategy: IDENTITY
      enableAuditing: true
      enableOptimisticLocking: true
      generateQueryMethods: true
```

### 3. Annotate Your Ports

```java
@DrivenPort
public interface CustomerRepository {
    Customer findById(CustomerId id);
    List<Customer> findByStatus(CustomerStatus status);
    void save(Customer customer);
    void delete(CustomerId id);
}
```

### 4. Build Your Project

```bash
mvn clean compile
```

The plugin will generate:
- `CustomerEntity.java` - JPA entity
- `CustomerJpaRepository.java` - Spring Data repository
- `CustomerRepositoryAdapter.java` - Port implementation
- `CustomerMapper.java` - Domain ↔ Entity mapper
- Converters for value objects (CustomerId, CustomerStatus, etc.)

## Configuration Reference

### Core Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `basePackage` | String | `{app}.infrastructure.persistence` | Base package for generated code |
| `mergeMode` | Enum | `OVERWRITE` | How to handle existing files: OVERWRITE, SKIP, MERGE |
| `schema` | String | `""` | Database schema name |
| `idStrategy` | Enum | `ASSIGNED` | ID generation: IDENTITY, SEQUENCE, AUTO, UUID, ASSIGNED |
| `sequenceName` | String | `""` | Sequence name (when idStrategy=SEQUENCE) |

### Feature Flags

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enableAuditing` | Boolean | `true` | Add @CreatedDate/@LastModifiedDate fields |
| `enableSoftDelete` | Boolean | `false` | Add soft delete with deleted flag |
| `enableOptimisticLocking` | Boolean | `true` | Add @Version field for optimistic locking |
| `generateQueryMethods` | Boolean | `true` | Generate derived query methods |

### Naming Conventions

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `entitySuffix` | String | `Entity` | Suffix for entity classes |
| `adapterSuffix` | String | `Adapter` | Suffix for adapter classes |
| `springDataRepositorySuffix` | String | `JpaRepository` | Suffix for Spring Data repos |

## Configuration Examples

### Example 1: PostgreSQL with Sequences

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      schema: public
      idStrategy: SEQUENCE
      sequenceName: hibernate_sequence
      enableAuditing: true
      enableSoftDelete: true
      enableOptimisticLocking: true
```

### Example 2: MySQL with Auto-increment

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      idStrategy: IDENTITY
      enableAuditing: false
      enableSoftDelete: false
      enableOptimisticLocking: false
      generateQueryMethods: true
```

### Example 3: UUID-based IDs

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      idStrategy: UUID
      enableAuditing: true
      entitySuffix: JpaEntity
      adapterSuffix: JpaAdapter
```

## Usage Guide

### Domain Model Annotations

The plugin recognizes these domain model patterns:

```java
// Aggregate Root
@AggregateRoot
public record Customer(
    CustomerId id,
    Email email,
    CustomerStatus status,
    Address address,
    List<Order> orders
) {}

// Value Object (single field - will be unwrapped)
@ValueObject
public record CustomerId(Long value) {}

// Value Object (multi-field - will be @Embeddable)
@ValueObject
public record Address(
    String street,
    String city,
    String zipCode
) {}

// Enumeration
public enum CustomerStatus {
    ACTIVE, INACTIVE, SUSPENDED
}
```

### Repository Port Patterns

The plugin generates implementations for these port method patterns:

```java
@DrivenPort
public interface CustomerRepository {

    // Find by ID
    Optional<Customer> findById(CustomerId id);

    // Find all
    List<Customer> findAll();

    // Query methods (generates derived queries)
    List<Customer> findByStatus(CustomerStatus status);
    List<Customer> findByEmail(Email email);

    // Save
    Customer save(Customer customer);

    // Delete
    void deleteById(CustomerId id);
    void delete(Customer customer);

    // Exists
    boolean existsById(CustomerId id);
}
```

### Generated Code Structure

For a `Customer` aggregate, the plugin generates:

```
com.example.infrastructure.persistence/
├── entity/
│   ├── CustomerEntity.java          # @Entity with JPA annotations
│   └── AddressEmbeddable.java       # @Embeddable for Address VO
├── repository/
│   └── CustomerJpaRepository.java   # Spring Data JpaRepository
├── adapter/
│   └── CustomerRepositoryAdapter.java  # Implements CustomerRepository port
├── mapper/
│   └── CustomerMapper.java          # MapStruct mapper
└── converter/
    ├── CustomerIdConverter.java     # @Converter for CustomerId
    └── CustomerStatusConverter.java # @Converter for CustomerStatus enum
```

## ID Strategies

### IDENTITY (Auto-increment)

Best for: MySQL, PostgreSQL SERIAL

```yaml
idStrategy: IDENTITY
```

Generated code:
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

### SEQUENCE

Best for: PostgreSQL, Oracle

```yaml
idStrategy: SEQUENCE
sequenceName: customer_seq
```

Generated code:
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
@SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq")
private Long id;
```

### UUID

Best for: Distributed systems, String IDs

```yaml
idStrategy: UUID
```

Generated code:
```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private String id;
```

### ASSIGNED

Best for: Application-managed IDs

```yaml
idStrategy: ASSIGNED
```

Generated code:
```java
@Id
private String id;  // No @GeneratedValue
```

## Value Object Mapping

### Single-Field Value Objects

Single-field VOs are **unwrapped** to their underlying type in JPA entities and use `@Converter`:

**Domain:**
```java
@ValueObject
public record CustomerId(Long value) {}

@AggregateRoot
public record Customer(CustomerId id, String name) {}
```

**Generated Entity:**
```java
@Entity
@Table(name = "customer")
public class CustomerEntity {
    @Id
    @Convert(converter = CustomerIdConverter.class)
    private Long id;  // Unwrapped to Long

    private String name;
}
```

**Generated Converter:**
```java
@Converter(autoApply = true)
public class CustomerIdConverter implements AttributeConverter<CustomerId, Long> {
    @Override
    public Long convertToDatabaseColumn(CustomerId customerId) {
        return customerId != null ? customerId.value() : null;
    }

    @Override
    public CustomerId convertToEntityAttribute(Long dbData) {
        return dbData != null ? new CustomerId(dbData) : null;
    }
}
```

### Multi-Field Value Objects

Multi-field VOs are mapped as `@Embeddable`:

**Domain:**
```java
@ValueObject
public record Address(String street, String city, String zipCode) {}

@AggregateRoot
public record Customer(CustomerId id, Address address) {}
```

**Generated Embeddable:**
```java
@Embeddable
public class AddressEmbeddable {
    private String street;
    private String city;
    private String zipCode;

    // Constructors, getters, setters
}
```

**Generated Entity:**
```java
@Entity
public class CustomerEntity {
    @Embedded
    private AddressEmbeddable address;
}
```

## Relationships

### @OneToMany

**Domain:**
```java
@AggregateRoot
public record Customer(
    CustomerId id,
    List<Order> orders  // Collection detected
) {}
```

**Generated:**
```java
@Entity
public class CustomerEntity {
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEntity> orders = new ArrayList<>();
}
```

### @ManyToOne

**Domain:**
```java
@Entity
public record Order(
    OrderId id,
    CustomerId customerId  // FK reference detected
) {}
```

**Generated:**
```java
@Entity
public class OrderEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
}
```

### @ElementCollection

**Domain:**
```java
@AggregateRoot
public record Product(
    ProductId id,
    List<String> tags  // Primitive collection
) {}
```

**Generated:**
```java
@Entity
public class ProductEntity {
    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();
}
```

## Audit Fields

When `enableAuditing: true`, the plugin adds audit fields to entities:

**Generated Entity:**
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class CustomerEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

**Spring Configuration Required:**
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

## Soft Delete

When `enableSoftDelete: true`, the plugin adds soft delete support:

**Generated Entity:**
```java
@Entity
@Where(clause = "deleted = false")
public class CustomerEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
```

**Generated Adapter:**
```java
@Override
public void delete(CustomerId id) {
    repository.findById(id.value())
        .ifPresent(entity -> {
            entity.setDeleted(true);
            repository.save(entity);
        });
}
```

## Optimistic Locking

When `enableOptimisticLocking: true`, the plugin adds version control:

**Generated Entity:**
```java
@Entity
public class CustomerEntity {

    @Version
    @Column(name = "version")
    private Long version;
}
```

This prevents lost updates in concurrent modifications.

## Query Methods

When `generateQueryMethods: true`, the plugin generates derived query methods:

**Port Method:**
```java
List<Customer> findByStatus(CustomerStatus status);
```

**Generated Spring Data Repository:**
```java
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    List<CustomerEntity> findByStatus(CustomerStatus status);
}
```

**Generated Adapter:**
```java
@Override
public List<Customer> findByStatus(CustomerStatus status) {
    return repository.findByStatus(status)
        .stream()
        .map(mapper::toDomain)
        .toList();
}
```

## FAQ

### Q: How does the plugin detect aggregate roots?

**A:** The plugin uses multiple detection strategies:
1. `@AggregateRoot` annotation (explicit)
2. Package heuristics (types in `*.domain.*` or `*.model.*` packages)
3. Port method return types (types returned by `@DrivenPort` methods)

### Q: Can I customize the generated entity table name?

**A:** Currently, table names are derived from the aggregate name using snake_case. Example: `CustomerOrder` → `customer_order`. Custom naming will be added in a future version.

### Q: What if my domain uses primitive types instead of value objects for IDs?

**A:** The plugin supports primitive and wrapper IDs directly:
```java
public record Customer(Long id, String name) {}  // Works fine
```

### Q: How do I handle bidirectional relationships?

**A:** The plugin detects bidirectional relationships when both sides reference each other. Use `mappedBy` in the parent entity to indicate the owning side.

### Q: Can I mix generated and handwritten code?

**A:** Yes! Use `mergeMode: SKIP` to prevent overwriting existing files. The plugin will only generate missing files.

### Q: Does the plugin support inheritance?

**A:** Currently, JPA inheritance strategies (@Inheritance, @DiscriminatorColumn) are not supported. This is planned for a future release.

### Q: What databases are supported?

**A:** The plugin generates standard JPA code compatible with all JPA providers (Hibernate, EclipseLink) and databases (PostgreSQL, MySQL, Oracle, H2, etc.).

## Migration Guide

### From Manual JPA Code

If you have existing JPA infrastructure:

1. **Backup your code**
2. **Configure the plugin** with `mergeMode: SKIP`
3. **Run generation** to see what would be created
4. **Gradually adopt** generated code by removing manual implementations
5. **Switch to** `mergeMode: OVERWRITE` for full automation

### From Old Plugin Version (Pre-0.4.0)

The 0.4.0 release is a complete rewrite with breaking changes:

**Before (0.3.x):**
```yaml
jpa:
  package: com.example.persistence
  strategy: identity
```

**After (0.4.0):**
```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      idStrategy: IDENTITY
```

**Key Changes:**
- Plugin ID changed to `io.hexaglue.plugin.jpa`
- Configuration moved under `hexaglue.plugins` namespace
- Package option renamed to `basePackage`
- Strategy option renamed to `idStrategy` with enum values
- New feature flags added (auditing, soft delete, etc.)

## Architecture

For developers interested in how the plugin works:

### Plugin Phases

1. **ANALYZE**: Scan domain model, detect aggregates, value objects, relationships
2. **VALIDATE**: Check ID strategies, type compatibility, relationship constraints
3. **GENERATE**: Produce entities, repositories, adapters, mappers, converters
4. **WRITE**: Write source files via HexaGlue Filer API

### Key Components

- **IdTypeResolver**: Detects and unwraps ID types from port methods
- **PropertyTypeResolver**: Maps domain properties to JPA types
- **RelationshipDetector**: Identifies @OneToMany, @ManyToOne, @ElementCollection
- **EntityGenerator**: Generates JPA entities with proper annotations
- **MapperGenerator**: Creates MapStruct mappers for domain ↔ entity conversion
- **AdapterGenerator**: Implements port interfaces using Spring Data repositories

### Extension Points

To add custom generation logic:

1. Extend `EntityGenerator` or `AdapterGenerator`
2. Register via ServiceLoader
3. Override specific methods (e.g., `generateFields()`, `generateMethods()`)

See `CONTRIBUTING.md` for detailed developer documentation.

## Contributing

We welcome contributions! Please see:

- **CONTRIBUTING.md**: Developer guide, coding standards, pull request process
- **ARCHITECTURE.md**: Detailed plugin architecture and design decisions
- **doc/internal/work_in_progress/**: Implementation plans and design documents

### Building from Source

```bash
git clone https://github.com/hexaglue/hexaglue.git
cd hexaglue/hexaglue-plugin-jpa-repository
mvn clean install
```

### Running Tests

```bash
mvn test                                    # Unit tests
mvn test -Dtest=*IntegrationTest           # Integration tests
mvn clean test jacoco:report               # With coverage
```

## License

This project is licensed under the **Mozilla Public License 2.0** (MPL-2.0).

Commercial licensing options are available for organizations wishing to use HexaGlue under different terms.

Contact: info@hexaglue.io

## Support

- **Documentation**: https://hexaglue.io/docs/plugins/jpa
- **Issues**: https://github.com/hexaglue/hexaglue/issues
- **Discussions**: https://github.com/hexaglue/hexaglue/discussions

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
