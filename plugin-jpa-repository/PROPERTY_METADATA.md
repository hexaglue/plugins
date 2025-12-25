# JPA Plugin - Property Metadata Configuration

**Fine-grained control over JPA entity properties and relationships**

> **Audience:** Developers using the HexaGlue JPA plugin
> **Prerequisites:** Understanding of JPA annotations and hexagonal architecture

---

## Table of Contents

- [Overview](#overview)
- [Configuration Structure](#configuration-structure)
- [Column Metadata](#column-metadata)
- [Relationship Metadata](#relationship-metadata)
- [Complete Examples](#complete-examples)
- [Resolution Strategy](#resolution-strategy)
- [Best Practices](#best-practices)

---

## Overview

The HexaGlue JPA plugin uses **Evidence-Based Resolution** to determine how domain properties map to JPA entity fields:

1. **Annotations** - JPA/jMolecules annotations in domain code (if present)
2. **YAML Configuration** - Explicit property metadata in `hexaglue.yaml`
3. **Heuristics** - Smart defaults based on property types and names
4. **Defaults** - JPA standard defaults

Property metadata allows you to override defaults and fine-tune JPA mappings without polluting your domain model with framework annotations.

---

## Configuration Structure

Property metadata is configured per-type in `hexaglue.yaml`:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      types:
        com.example.domain.Customer:
          properties:
            propertyName:
              column:
                # Column metadata
              relationship:
                # Relationship metadata
```

### Metadata Categories

Each property can have two categories of metadata:

- **`column:`** - JPA column annotations (@Column, @Lob, @Enumerated, etc.)
- **`relationship:`** - JPA relationship annotations (@OneToMany, @ManyToOne, etc.)

---

## Column Metadata

Configure JPA `@Column` annotation attributes and special type handling.

### Basic Column Attributes

```yaml
properties:
  name:
    column:
      name: customer_name      # Column name (default: snake_case of property)
      length: 100              # String length (default: 255)
      nullable: false          # Allow NULL (default: true)
      unique: true             # Unique constraint (default: false)
      precision: 10            # Decimal precision (for BigDecimal)
      scale: 2                 # Decimal scale (for BigDecimal)
```

**Generated code:**
```java
@Column(name = "customer_name", length = 100, nullable = false, unique = true)
private String name;
```

### String Properties

```yaml
properties:
  email:
    column:
      length: 255
      nullable: false
      unique: true

  description:
    column:
      length: 1000
      nullable: true

  notes:
    column:
      lob: true              # Use @Lob for large text
      nullable: true
```

**Generated code:**
```java
@Column(name = "email", length = 255, nullable = false, unique = true)
private String email;

@Column(name = "description", length = 1000)
private String description;

@Lob
@Column(name = "notes")
private String notes;
```

### Numeric Properties

```yaml
properties:
  price:
    column:
      precision: 10
      scale: 2
      nullable: false

  quantity:
    column:
      nullable: false
```

**Generated code:**
```java
@Column(name = "price", precision = 10, scale = 2, nullable = false)
private BigDecimal price;

@Column(name = "quantity", nullable = false)
private Integer quantity;
```

### Enum Properties

```yaml
properties:
  status:
    column:
      nullable: false
      enumType: STRING       # STRING (default) or ORDINAL
```

**Generated code:**
```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private CustomerStatus status;
```

### Temporal Properties

```yaml
properties:
  birthDate:
    column:
      nullable: true
      temporalType: DATE     # DATE, TIME, or TIMESTAMP
```

**Generated code:**
```java
@Temporal(TemporalType.DATE)
@Column(name = "birth_date")
private Date birthDate;
```

### Large Objects (LOB)

For large text or binary data:

```yaml
properties:
  profilePicture:
    column:
      lob: true
      nullable: true

  documentContent:
    column:
      lob: true
      nullable: false
```

**Generated code:**
```java
@Lob
@Column(name = "profile_picture")
private byte[] profilePicture;

@Lob
@Column(name = "document_content", nullable = false)
private String documentContent;
```

### Column Metadata Reference

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | String | snake_case of property | Database column name |
| `length` | Integer | 255 | Maximum length for String columns |
| `nullable` | Boolean | true | Allow NULL values |
| `unique` | Boolean | false | Unique constraint |
| `precision` | Integer | 19 | Precision for BigDecimal (total digits) |
| `scale` | Integer | 2 | Scale for BigDecimal (decimal digits) |
| `lob` | Boolean | false | Use @Lob for large objects |
| `enumType` | Enum | STRING | STRING or ORDINAL for enums |
| `temporalType` | Enum | - | DATE, TIME, or TIMESTAMP for temporal types |

---

## Relationship Metadata

Configure JPA relationship annotations for associations between entities.

### Basic Relationship Structure

```yaml
properties:
  propertyName:
    relationship:
      kind: MANY_TO_ONE             # Relationship type
      target: com.example.domain.Type  # Target domain type
      cascade: [PERSIST, MERGE]     # Cascade operations
      fetch: LAZY                   # Fetch strategy
      interAggregate: false         # DDD aggregate boundary
      bidirectional: false          # Bidirectional relationship
      mappedBy: propertyName        # Owning side (if bidirectional)
      joinColumn: column_name       # FK column name
      orphanRemoval: true           # Remove orphans
```

### Many-to-One (Intra-Aggregate)

Child entity references parent within same aggregate:

```yaml
properties:
  order:
    relationship:
      kind: MANY_TO_ONE
      target: com.example.domain.Order
      fetch: LAZY
      cascade: []
      interAggregate: false
```

**Generated code:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private OrderEntity order;
```

### Many-to-One (Inter-Aggregate)

⚠️ **DDD Warning**: Cross-aggregate references should use ID references, not @ManyToOne:

```yaml
# ❌ BAD - Direct @ManyToOne across aggregates
properties:
  customer:
    relationship:
      kind: MANY_TO_ONE
      target: com.example.domain.Customer
      interAggregate: true

# ✅ GOOD - Use simple FK
properties:
  customerId:
    column:
      nullable: false
```

**Generated code:**
```java
// Simple FK column (recommended for inter-aggregate)
@Column(name = "customer_id", nullable = false)
private String customerId;
```

### One-to-Many (Parent-Child Composition)

Parent owns collection of children:

```yaml
properties:
  orderLines:
    relationship:
      kind: ONE_TO_MANY
      target: com.example.domain.OrderLine
      cascade: [ALL]
      fetch: LAZY
      orphanRemoval: true
      bidirectional: true
      mappedBy: order
```

**Generated code:**
```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private List<OrderLineEntity> orderLines;
```

### Element Collection

Collection of simple types or embeddables:

```yaml
properties:
  tags:
    relationship:
      kind: ELEMENT_COLLECTION
      target: java.lang.String
      fetch: LAZY
```

**Generated code:**
```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "customer_tags", joinColumns = @JoinColumn(name = "customer_id"))
@Column(name = "tag")
private Set<String> tags;
```

### Embedded Value Objects

Multi-field value objects:

```yaml
properties:
  address:
    relationship:
      kind: EMBEDDED
      target: com.example.domain.Address
```

**Generated code:**
```java
@Embedded
private AddressEmbeddable address;
```

### Relationship Metadata Reference

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `kind` | Enum | - | ONE_TO_MANY, MANY_TO_ONE, EMBEDDED, ELEMENT_COLLECTION |
| `target` | String | - | Fully qualified target domain type |
| `cascade` | Array | `[]` | PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL |
| `fetch` | Enum | LAZY | LAZY or EAGER |
| `interAggregate` | Boolean | false | Cross-aggregate boundary (DDD) |
| `bidirectional` | Boolean | false | Bidirectional relationship |
| `mappedBy` | String | - | Property name in target entity (for bidirectional) |
| `joinColumn` | String | auto | Foreign key column name |
| `orphanRemoval` | Boolean | false | Remove orphaned entities |
| `collectionType` | Enum | LIST | LIST, SET, or MAP (for collections) |

---

## Complete Examples

### Example 1: Customer Entity

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      types:
        com.example.domain.Customer:
          tableName: customers
          schema: public
          properties:
            name:
              column:
                length: 100
                nullable: false
            email:
              column:
                length: 255
                unique: true
                nullable: false
            status:
              column:
                nullable: false
                enumType: STRING
            address:
              relationship:
                kind: EMBEDDED
                target: com.example.domain.Address
            phoneNumbers:
              relationship:
                kind: ELEMENT_COLLECTION
                target: java.lang.String
                fetch: LAZY
```

**Generated entity:**
```java
@Entity
@Table(name = "customers", schema = "public")
public class CustomerEntity {

    @Id
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 255, unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;

    @Embedded
    private AddressEmbeddable address;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_phone_numbers")
    private List<String> phoneNumbers;
}
```

### Example 2: Order with Relationships

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      types:
        com.example.domain.Order:
          tableName: orders
          properties:
            customerId:
              column:
                nullable: false
                # Simple FK for inter-aggregate reference
            orderDate:
              column:
                nullable: false
            status:
              column:
                nullable: false
                enumType: STRING
            orderLines:
              relationship:
                kind: ONE_TO_MANY
                target: com.example.domain.OrderLine
                cascade: [ALL]
                orphanRemoval: true
                bidirectional: true
                mappedBy: order
                fetch: LAZY
```

**Generated entity:**
```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;  // Simple FK, not @ManyToOne

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OrderLineEntity> orderLines;
}
```

### Example 3: Audit and Soft Delete

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      enableAuditing: true
      enableSoftDelete: true
      types:
        com.example.domain.Product:
          tableName: products
          properties:
            name:
              column:
                length: 200
                nullable: false
            description:
              column:
                lob: true
            price:
              column:
                precision: 10
                scale: 2
                nullable: false
            category:
              column:
                nullable: false
                enumType: STRING
```

**Generated entity:**
```java
@Entity
@Table(name = "products")
@Where(clause = "deleted = false")
public class ProductEntity {

    @Id
    private String id;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    // Auditing fields
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    // Soft delete field
    @Column(name = "deleted")
    private boolean deleted = false;

    // Optimistic locking
    @Version
    private Long version;
}
```

---

## Resolution Strategy

The plugin resolves property metadata using this priority:

1. **Explicit YAML** - Highest priority
2. **Domain annotations** - JPA/jMolecules annotations (if present)
3. **Heuristics** - Smart defaults based on types
4. **JPA defaults** - Standard JPA defaults

### Example Resolution

**Domain model:**
```java
@AggregateRoot
public record Customer(
    @Identity CustomerId id,
    @NotNull String name,      // ← javax.validation annotation
    Email email,
    CustomerStatus status
) {}
```

**YAML config:**
```yaml
types:
  com.example.domain.Customer:
    properties:
      name:
        column:
          length: 100
          nullable: false
```

**Resolution result:**
- `name.length` → 100 (from YAML)
- `name.nullable` → false (from YAML)
- `email.nullable` → true (JPA default, no annotation or config)
- `status.enumType` → STRING (heuristic default for enums)

---

## Best Practices

### 1. Start with Defaults

Don't over-configure. Let the plugin use intelligent defaults:

```yaml
# ✅ GOOD - Only configure what's necessary
types:
  com.example.domain.Customer:
    properties:
      email:
        column:
          unique: true

# ❌ BAD - Over-specification
types:
  com.example.domain.Customer:
    properties:
      email:
        column:
          name: email
          length: 255
          nullable: true
          unique: true
```

### 2. Inter-Aggregate References

Use simple FK fields, not @ManyToOne:

```yaml
# ✅ GOOD - Simple FK
types:
  com.example.domain.Order:
    properties:
      customerId:
        column:
          nullable: false

# ❌ BAD - Direct @ManyToOne across aggregates
types:
  com.example.domain.Order:
    properties:
      customer:
        relationship:
          kind: MANY_TO_ONE
          target: com.example.domain.Customer
          interAggregate: true
```

### 3. Intra-Aggregate Composition

Use ONE_TO_MANY with orphanRemoval:

```yaml
types:
  com.example.domain.Order:
    properties:
      orderLines:
        relationship:
          kind: ONE_TO_MANY
          target: com.example.domain.OrderLine
          cascade: [ALL]
          orphanRemoval: true
          bidirectional: true
          mappedBy: order
```

### 4. String Lengths

Configure realistic lengths to avoid truncation:

```yaml
properties:
  name:
    column:
      length: 100      # Person names
  email:
    column:
      length: 255      # Email addresses
  description:
    column:
      length: 1000     # Short descriptions
  content:
    column:
      lob: true        # Large content
```

### 5. Null Safety

Be explicit about nullability:

```yaml
properties:
  requiredField:
    column:
      nullable: false

  optionalField:
    column:
      nullable: true
```

### 6. Fetch Strategies

Default to LAZY, use EAGER sparingly:

```yaml
# ✅ GOOD - Lazy by default
orderLines:
  relationship:
    kind: ONE_TO_MANY
    fetch: LAZY

# ⚠️ CAUTION - Eager can cause N+1
customer:
  relationship:
    kind: MANY_TO_ONE
    fetch: EAGER  # Only if always needed
```

---

## Related Documentation

- [JPA Plugin README](README.md) - Plugin overview and quick start
- [JPA Plugin Architecture](ARCHITECTURE.md) - How the plugin works internally
- [Plugin Development](../PLUGIN_DEVELOPMENT.md) - Creating custom plugins
- [SPI Reference](../../engine/engine-spi/SPI_REFERENCE.md) - Plugin API reference

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
