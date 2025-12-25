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
package io.hexaglue.plugin.jpa.model;

import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Model representing a complete JPA entity with all metadata.
 *
 * <p>This model contains everything needed to generate a JPA @Entity class:</p>
 * <ul>
 *   <li><strong>Names</strong>: Entity class name, table name, schema</li>
 *   <li><strong>Domain mapping</strong>: Original domain type reference</li>
 *   <li><strong>ID</strong>: ID field metadata and generation strategy</li>
 *   <li><strong>Properties</strong>: All persistent fields</li>
 *   <li><strong>Feature flags</strong>: Auditing, soft delete, optimistic locking</li>
 * </ul>
 *
 * <h2>Generated Structure</h2>
 * <pre>{@code
 * @Entity
 * @Table(name = "customers", schema = "public")
 * public class CustomerEntity {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     @Version
 *     private Long version; // if optimistic locking enabled
 *
 *     @Column(name = "created_at")
 *     private Instant createdAt; // if auditing enabled
 *
 *     @Column(name = "name", length = 100, nullable = false)
 *     private String name;
 *
 *     // ... more properties
 * }
 * }</pre>
 *
 * @param entityClassName simple class name (e.g., "CustomerEntity")
 * @param entityPackage package name for the entity
 * @param tableName database table name (e.g., "customers")
 * @param schema database schema (empty if not specified)
 * @param domainType original domain type reference
 * @param idModel ID field metadata
 * @param properties list of persistent properties
 * @param relationships list of JPA relationships (@OneToMany, @Embedded, etc.)
 * @param enableAuditing if true, add createdAt/updatedAt fields
 * @param enableSoftDelete if true, add deletedAt field
 * @param enableOptimisticLocking if true, add version field
 * @since 0.4.0
 */
public record EntityModel(
        String entityClassName,
        String entityPackage,
        String tableName,
        String schema,
        TypeRef domainType,
        IdModel idModel,
        List<PropertyModel> properties,
        List<RelationshipModel> relationships,
        boolean enableAuditing,
        boolean enableSoftDelete,
        boolean enableOptimisticLocking) {

    /**
     * Compact constructor with validation and defensive copying.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public EntityModel {
        Objects.requireNonNull(entityClassName, "entityClassName");
        Objects.requireNonNull(entityPackage, "entityPackage");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(domainType, "domainType");
        Objects.requireNonNull(idModel, "idModel");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(relationships, "relationships");
        properties = List.copyOf(properties); // Defensive copy
        relationships = List.copyOf(relationships); // Defensive copy
    }

    /**
     * Gets the fully qualified entity class name.
     *
     * @return qualified name (e.g., "com.example.persistence.entity.CustomerEntity")
     */
    public String qualifiedClassName() {
        return entityPackage + "." + entityClassName;
    }

    /**
     * Gets the schema if specified.
     *
     * @return schema or empty if not specified
     */
    public Optional<String> schemaIfPresent() {
        return schema.isBlank() ? Optional.empty() : Optional.of(schema);
    }

    /**
     * Checks if any feature flag is enabled.
     *
     * @return true if auditing, soft delete, or optimistic locking is enabled
     */
    public boolean hasFeatures() {
        return enableAuditing || enableSoftDelete || enableOptimisticLocking;
    }

    /**
     * Checks if this entity has any relationships.
     *
     * @return true if relationships list is not empty
     */
    public boolean hasRelationships() {
        return !relationships.isEmpty();
    }

    /**
     * Builder for constructing EntityModel instances.
     */
    public static class Builder {
        private String entityClassName;
        private String entityPackage;
        private String tableName;
        private String schema = "";
        private TypeRef domainType;
        private IdModel idModel;
        private List<PropertyModel> properties = List.of();
        private List<RelationshipModel> relationships = List.of();
        private boolean enableAuditing = false;
        private boolean enableSoftDelete = false;
        private boolean enableOptimisticLocking = false;

        public Builder entityClassName(String entityClassName) {
            this.entityClassName = entityClassName;
            return this;
        }

        public Builder entityPackage(String entityPackage) {
            this.entityPackage = entityPackage;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder domainType(TypeRef domainType) {
            this.domainType = domainType;
            return this;
        }

        public Builder idModel(IdModel idModel) {
            this.idModel = idModel;
            return this;
        }

        public Builder properties(List<PropertyModel> properties) {
            this.properties = properties;
            return this;
        }

        public Builder relationships(List<RelationshipModel> relationships) {
            this.relationships = relationships;
            return this;
        }

        public Builder enableAuditing(boolean enableAuditing) {
            this.enableAuditing = enableAuditing;
            return this;
        }

        public Builder enableSoftDelete(boolean enableSoftDelete) {
            this.enableSoftDelete = enableSoftDelete;
            return this;
        }

        public Builder enableOptimisticLocking(boolean enableOptimisticLocking) {
            this.enableOptimisticLocking = enableOptimisticLocking;
            return this;
        }

        public EntityModel build() {
            return new EntityModel(
                    entityClassName,
                    entityPackage,
                    tableName,
                    schema,
                    domainType,
                    idModel,
                    properties,
                    relationships,
                    enableAuditing,
                    enableSoftDelete,
                    enableOptimisticLocking);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return entity model builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
