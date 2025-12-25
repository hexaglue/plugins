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
import java.util.Objects;
import java.util.Optional;

/**
 * Model representing a JPA relationship between entities or value objects.
 *
 * <p>This model captures all metadata needed to generate JPA relationship annotations:</p>
 * <ul>
 *   <li><strong>Type</strong>: ONE_TO_MANY, MANY_TO_ONE, EMBEDDED, ELEMENT_COLLECTION, etc.</li>
 *   <li><strong>Target type</strong>: The related entity or value object type</li>
 *   <li><strong>Cascade operations</strong>: PERSIST, MERGE, REMOVE, etc.</li>
 *   <li><strong>Fetch strategy</strong>: LAZY or EAGER</li>
 *   <li><strong>Ownership</strong>: mappedBy, orphanRemoval, joinColumn</li>
 * </ul>
 *
 * <h2>Relationship Types</h2>
 * <ul>
 *   <li><strong>ONE_TO_MANY</strong>: Parent-child composition (intra-aggregate)</li>
 *   <li><strong>MANY_TO_ONE</strong>: Child reference to parent (should be ID-only for inter-aggregate)</li>
 *   <li><strong>EMBEDDED</strong>: Multi-field Value Object embedded in entity</li>
 *   <li><strong>ELEMENT_COLLECTION</strong>: Collection of simple types or embeddables</li>
 *   <li><strong>EMBEDDED_ID</strong>: Composite primary key</li>
 * </ul>
 *
 * <h2>DDD Rules</h2>
 * <ul>
 *   <li><strong>Intra-aggregate</strong>: Use @OneToMany, @Embedded, @ElementCollection with orphanRemoval=true</li>
 *   <li><strong>Inter-aggregate</strong>: Use simple FK (String customerId) instead of @ManyToOne</li>
 *   <li><strong>Aggregate Root</strong>: Only aggregate roots have repositories</li>
 * </ul>
 *
 * @param propertyName name of the property in the entity (e.g., "orderItems")
 * @param relationshipType type of JPA relationship
 * @param targetType type reference of the related entity/value object
 * @param targetEntityName simple name of the target JPA entity (if applicable)
 * @param collectionType type of collection (List, Set, Map) or null if not a collection
 * @param cascade cascade operations (PERSIST, MERGE, REMOVE, etc.)
 * @param fetch fetch strategy (LAZY or EAGER)
 * @param orphanRemoval true if orphanRemoval should be enabled
 * @param mappedBy property name in the target entity that owns the relationship (for bidirectional)
 * @param joinColumnName name of the foreign key column (for unidirectional)
 * @param scope relationship scope (INTRA_AGGREGATE or INTER_AGGREGATE)
 * @since 0.4.0
 */
public record RelationshipModel(
        String propertyName,
        RelationshipType relationshipType,
        TypeRef targetType,
        String targetEntityName,
        CollectionType collectionType,
        CascadeType[] cascade,
        FetchType fetch,
        boolean orphanRemoval,
        String mappedBy,
        String joinColumnName,
        RelationshipScope scope) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if required fields are null
     */
    public RelationshipModel {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(relationshipType, "relationshipType");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(fetch, "fetch");
        Objects.requireNonNull(scope, "scope");

        // Default empty array for cascade if null
        if (cascade == null) {
            cascade = new CascadeType[0];
        }
    }

    /**
     * Gets the mapped by property if present.
     *
     * @return mapped by property or empty if not bidirectional
     */
    public Optional<String> mappedByIfPresent() {
        return Optional.ofNullable(mappedBy);
    }

    /**
     * Gets the join column name if present.
     *
     * @return join column name or empty if not applicable
     */
    public Optional<String> joinColumnIfPresent() {
        return Optional.ofNullable(joinColumnName);
    }

    /**
     * Gets the target entity name if present.
     *
     * @return target entity name or empty if not applicable
     */
    public Optional<String> targetEntityNameIfPresent() {
        return Optional.ofNullable(targetEntityName);
    }

    /**
     * Gets the collection type if present.
     *
     * @return collection type or empty if not a collection
     */
    public Optional<CollectionType> collectionTypeIfPresent() {
        return Optional.ofNullable(collectionType);
    }

    /**
     * Checks if this is a bidirectional relationship.
     *
     * @return true if mappedBy is set
     */
    public boolean isBidirectional() {
        return mappedBy != null && !mappedBy.isBlank();
    }

    /**
     * Checks if this is an intra-aggregate relationship.
     *
     * @return true if scope is INTRA_AGGREGATE
     */
    public boolean isIntraAggregate() {
        return scope == RelationshipScope.INTRA_AGGREGATE;
    }

    /**
     * Checks if this is an inter-aggregate relationship.
     *
     * @return true if scope is INTER_AGGREGATE
     */
    public boolean isInterAggregate() {
        return scope == RelationshipScope.INTER_AGGREGATE;
    }

    /**
     * Builder for constructing RelationshipModel instances.
     */
    public static class Builder {
        private String propertyName;
        private RelationshipType relationshipType;
        private TypeRef targetType;
        private String targetEntityName;
        private CollectionType collectionType;
        private CascadeType[] cascade = new CascadeType[0];
        private FetchType fetch = FetchType.LAZY;
        private boolean orphanRemoval = false;
        private String mappedBy;
        private String joinColumnName;
        private RelationshipScope scope = RelationshipScope.INTRA_AGGREGATE;

        public Builder propertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public Builder relationshipType(RelationshipType relationshipType) {
            this.relationshipType = relationshipType;
            return this;
        }

        public Builder targetType(TypeRef targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder targetEntityName(String targetEntityName) {
            this.targetEntityName = targetEntityName;
            return this;
        }

        public Builder collectionType(CollectionType collectionType) {
            this.collectionType = collectionType;
            return this;
        }

        public Builder cascade(CascadeType... cascade) {
            this.cascade = cascade;
            return this;
        }

        public Builder fetch(FetchType fetch) {
            this.fetch = fetch;
            return this;
        }

        public Builder orphanRemoval(boolean orphanRemoval) {
            this.orphanRemoval = orphanRemoval;
            return this;
        }

        public Builder mappedBy(String mappedBy) {
            this.mappedBy = mappedBy;
            return this;
        }

        public Builder joinColumnName(String joinColumnName) {
            this.joinColumnName = joinColumnName;
            return this;
        }

        public Builder scope(RelationshipScope scope) {
            this.scope = scope;
            return this;
        }

        public RelationshipModel build() {
            return new RelationshipModel(
                    propertyName,
                    relationshipType,
                    targetType,
                    targetEntityName,
                    collectionType,
                    cascade,
                    fetch,
                    orphanRemoval,
                    mappedBy,
                    joinColumnName,
                    scope);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return relationship model builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Type of JPA relationship.
     */
    public enum RelationshipType {
        /**
         * One-to-many relationship (parent owns collection of children).
         */
        ONE_TO_MANY,

        /**
         * Many-to-one relationship (child references parent).
         */
        MANY_TO_ONE,

        /**
         * Embedded value object (multi-field VO).
         */
        EMBEDDED,

        /**
         * Collection of simple types or embeddables.
         */
        ELEMENT_COLLECTION,

        /**
         * Composite primary key.
         */
        EMBEDDED_ID
    }

    /**
     * Type of collection for relationships.
     */
    public enum CollectionType {
        /** java.util.List */
        LIST,
        /** java.util.Set */
        SET,
        /** java.util.Map */
        MAP
    }

    /**
     * JPA cascade operations.
     */
    public enum CascadeType {
        /** Cascade persist operations */
        PERSIST,
        /** Cascade merge operations */
        MERGE,
        /** Cascade remove operations */
        REMOVE,
        /** Cascade refresh operations */
        REFRESH,
        /** Cascade detach operations */
        DETACH,
        /** Cascade all operations */
        ALL
    }

    /**
     * JPA fetch strategy.
     */
    public enum FetchType {
        /** Lazy loading (default for collections) */
        LAZY,
        /** Eager loading (fetch with parent) */
        EAGER
    }

    /**
     * Relationship scope in DDD context.
     */
    public enum RelationshipScope {
        /**
         * Relationship within the same aggregate (composition).
         * Should use @OneToMany, @Embedded, @ElementCollection with orphanRemoval=true.
         */
        INTRA_AGGREGATE,

        /**
         * Relationship between different aggregates (reference).
         * Should use simple FK (String customerId) instead of @ManyToOne.
         */
        INTER_AGGREGATE
    }
}
