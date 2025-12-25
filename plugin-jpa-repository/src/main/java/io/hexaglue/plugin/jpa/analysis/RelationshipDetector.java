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
package io.hexaglue.plugin.jpa.analysis;

import io.hexaglue.plugin.jpa.model.RelationshipModel;
import io.hexaglue.plugin.jpa.util.NamingUtils;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.types.CollectionMetadata;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Detects and analyzes JPA relationships in domain models.
 *
 * <p>This analyzer identifies different types of relationships and determines
 * how they should be mapped to JPA annotations:</p>
 *
 * <h2>Detection Rules</h2>
 * <ul>
 *   <li><strong>@Embedded</strong>: Multi-field Value Objects (Address, Money)</li>
 *   <li><strong>@ElementCollection</strong>: Collections of simple types or embeddables</li>
 *   <li><strong>@OneToMany</strong>: Collections of entities within the same aggregate</li>
 *   <li><strong>@ManyToOne</strong>: Entity references (WARNING for inter-aggregate)</li>
 *   <li><strong>Simple FK</strong>: ID references between aggregates (recommended for DDD)</li>
 * </ul>
 *
 * <h2>DDD Principles</h2>
 * <p>The detector enforces Domain-Driven Design principles:</p>
 * <ul>
 *   <li><strong>Intra-aggregate</strong>: Use composition (@OneToMany, @Embedded) with orphanRemoval</li>
 *   <li><strong>Inter-aggregate</strong>: Use ID-only references (String customerId) instead of @ManyToOne</li>
 *   <li><strong>Aggregate boundaries</strong>: Respect aggregate root boundaries</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * RelationshipDetector detector = new RelationshipDetector(context, "com.example.domain.Order");
 *
 * // Detect relationships for a property
 * Optional<RelationshipModel> relationship = detector.detectRelationship(orderItemsProperty);
 *
 * if (relationship.isPresent()) {
 *     RelationshipModel rel = relationship.get();
 *     System.out.println("Type: " + rel.relationshipType());
 *     System.out.println("Scope: " + rel.scope());
 * }
 * }</pre>
 *
 * @since 0.4.0
 */
public final class RelationshipDetector {

    private final GenerationContextSpec context;
    private final String owningDomainTypeName;

    public RelationshipDetector(GenerationContextSpec context, String owningDomainTypeName) {
        this.context = Objects.requireNonNull(context, "context");
        this.owningDomainTypeName = Objects.requireNonNull(owningDomainTypeName, "owningDomainTypeName");
    }

    /**
     * Detects all relationships for a domain type.
     *
     * @param domainType domain type to analyze
     * @return list of detected relationships
     */
    public List<RelationshipModel> detectRelationships(DomainTypeView domainType) {
        Objects.requireNonNull(domainType, "domainType");

        List<RelationshipModel> relationships = new ArrayList<>();

        for (DomainPropertyView property : domainType.properties()) {
            // Skip ID property
            if ("id".equalsIgnoreCase(property.name())) {
                continue;
            }

            detectRelationship(property).ifPresent(relationships::add);
        }

        return relationships;
    }

    /**
     * Detects the relationship type for a single property.
     *
     * <p>Detection logic:</p>
     * <ol>
     *   <li>Check if property is a collection</li>
     *   <li>If collection, analyze element type</li>
     *   <li>If not collection, analyze property type</li>
     *   <li>Determine relationship scope (intra vs inter-aggregate)</li>
     * </ol>
     *
     * @param property domain property to analyze
     * @return detected relationship or empty if not a relationship
     */
    public Optional<RelationshipModel> detectRelationship(DomainPropertyView property) {
        Objects.requireNonNull(property, "property");

        TypeRef propertyType = property.type();

        // Check if it's a collection
        Optional<CollectionMetadata> collectionMeta = propertyType.collectionMetadata();

        if (collectionMeta.isPresent()) {
            return detectCollectionRelationship(property, collectionMeta.get());
        } else {
            return detectSingleValueRelationship(property, propertyType);
        }
    }

    /**
     * Detects relationship for collection properties.
     *
     * <p>Possible outcomes:</p>
     * <ul>
     *   <li><strong>@ElementCollection</strong>: Collection of primitives, wrappers, enums, or embeddables</li>
     *   <li><strong>@OneToMany</strong>: Collection of entities (intra-aggregate)</li>
     *   <li><strong>No relationship</strong>: Collection should be ID-only (inter-aggregate)</li>
     * </ul>
     */
    private Optional<RelationshipModel> detectCollectionRelationship(
            DomainPropertyView property, CollectionMetadata collectionMeta) {

        TypeRef elementType = collectionMeta.elementType();
        String elementTypeName = elementType.render();

        // Look up element type in IR
        Optional<DomainTypeView> elementDomainType = context.model().domain().findType(elementTypeName);

        if (elementDomainType.isEmpty()) {
            // Simple type (String, Integer, etc.) or unknown type
            // Use @ElementCollection if it's a known simple type
            if (isSimpleType(elementType)) {
                return Optional.of(buildElementCollectionRelationship(property, elementType, collectionMeta));
            }
            return Optional.empty();
        }

        DomainTypeView elementTypeView = elementDomainType.get();

        // Determine relationship type based on element type kind
        switch (elementTypeView.kind()) {
            case RECORD:
            case IDENTIFIER:
                // Multi-field Value Object → @ElementCollection with @Embeddable
                if (elementTypeView.properties().size() > 1) {
                    return Optional.of(buildElementCollectionRelationship(property, elementType, collectionMeta));
                }
                // Single-field VO → @ElementCollection (JPA will use converter)
                return Optional.of(buildElementCollectionRelationship(property, elementType, collectionMeta));

            case ENUMERATION:
                // Enum collection → @ElementCollection
                return Optional.of(buildElementCollectionRelationship(property, elementType, collectionMeta));

            case AGGREGATE_ROOT:
            case ENTITY:
                // Entity collection → @OneToMany
                // Determine scope (intra vs inter-aggregate)
                RelationshipModel.RelationshipScope scope = determineScope(elementTypeView);
                return Optional.of(buildOneToManyRelationship(property, elementType, collectionMeta, scope));

            default:
                return Optional.empty();
        }
    }

    /**
     * Detects relationship for single-value properties.
     *
     * <p>Possible outcomes:</p>
     * <ul>
     *   <li><strong>@Embedded</strong>: Multi-field Value Object</li>
     *   <li><strong>@ManyToOne</strong>: Entity reference (with warning for inter-aggregate)</li>
     *   <li><strong>No relationship</strong>: Simple type, single-field VO, or ID-only reference</li>
     * </ul>
     */
    private Optional<RelationshipModel> detectSingleValueRelationship(
            DomainPropertyView property, TypeRef propertyType) {

        String typeName = propertyType.render();

        // Look up in IR
        Optional<DomainTypeView> domainType = context.model().domain().findType(typeName);

        if (domainType.isEmpty()) {
            // Not a domain type, no relationship
            return Optional.empty();
        }

        DomainTypeView typeView = domainType.get();

        switch (typeView.kind()) {
            case RECORD:
            case IDENTIFIER:
                // Multi-field Value Object → @Embedded
                if (typeView.properties().size() > 1) {
                    return Optional.of(buildEmbeddedRelationship(property, propertyType));
                }
                // Single-field VO → no relationship (will be unwrapped or use converter)
                return Optional.empty();

            case AGGREGATE_ROOT:
            case ENTITY:
                // Entity reference → @ManyToOne (or FK for inter-aggregate)
                RelationshipModel.RelationshipScope scope = determineScope(typeView);

                if (scope == RelationshipModel.RelationshipScope.INTER_AGGREGATE) {
                    // For inter-aggregate, we should NOT generate @ManyToOne
                    // The property should be an ID reference (handled elsewhere)
                    return Optional.empty();
                }

                return Optional.of(buildManyToOneRelationship(property, propertyType, scope));

            case ENUMERATION:
            default:
                // Enum or other type → no relationship
                return Optional.empty();
        }
    }

    /**
     * Builds a @OneToMany relationship model.
     */
    private RelationshipModel buildOneToManyRelationship(
            DomainPropertyView property,
            TypeRef targetType,
            CollectionMetadata collectionMeta,
            RelationshipModel.RelationshipScope scope) {

        String targetEntityName = extractSimpleName(targetType) + "Entity";

        RelationshipModel.CollectionType collectionType = mapCollectionType(collectionMeta.kind());

        // For intra-aggregate relationships, use orphanRemoval and cascade ALL
        boolean orphanRemoval = (scope == RelationshipModel.RelationshipScope.INTRA_AGGREGATE);
        RelationshipModel.CascadeType[] cascade = orphanRemoval
                ? new RelationshipModel.CascadeType[] {RelationshipModel.CascadeType.ALL}
                : new RelationshipModel.CascadeType[] {
                    RelationshipModel.CascadeType.PERSIST, RelationshipModel.CascadeType.MERGE
                };

        return RelationshipModel.builder()
                .propertyName(property.name())
                .relationshipType(RelationshipModel.RelationshipType.ONE_TO_MANY)
                .targetType(targetType)
                .targetEntityName(targetEntityName)
                .collectionType(collectionType)
                .cascade(cascade)
                .fetch(RelationshipModel.FetchType.LAZY)
                .orphanRemoval(orphanRemoval)
                .scope(scope)
                .build();
    }

    /**
     * Builds a @ManyToOne relationship model.
     */
    private RelationshipModel buildManyToOneRelationship(
            DomainPropertyView property, TypeRef targetType, RelationshipModel.RelationshipScope scope) {

        String targetEntityName = extractSimpleName(targetType) + "Entity";
        String joinColumnName = NamingUtils.toColumnName(property.name() + "_id");

        return RelationshipModel.builder()
                .propertyName(property.name())
                .relationshipType(RelationshipModel.RelationshipType.MANY_TO_ONE)
                .targetType(targetType)
                .targetEntityName(targetEntityName)
                .fetch(RelationshipModel.FetchType.LAZY)
                .joinColumnName(joinColumnName)
                .scope(scope)
                .build();
    }

    /**
     * Builds an @Embedded relationship model.
     */
    private RelationshipModel buildEmbeddedRelationship(DomainPropertyView property, TypeRef targetType) {

        return RelationshipModel.builder()
                .propertyName(property.name())
                .relationshipType(RelationshipModel.RelationshipType.EMBEDDED)
                .targetType(targetType)
                .fetch(RelationshipModel.FetchType.EAGER) // Embedded is always eager
                .scope(RelationshipModel.RelationshipScope.INTRA_AGGREGATE)
                .build();
    }

    /**
     * Builds an @ElementCollection relationship model.
     */
    private RelationshipModel buildElementCollectionRelationship(
            DomainPropertyView property, TypeRef elementType, CollectionMetadata collectionMeta) {

        RelationshipModel.CollectionType collectionType = mapCollectionType(collectionMeta.kind());

        return RelationshipModel.builder()
                .propertyName(property.name())
                .relationshipType(RelationshipModel.RelationshipType.ELEMENT_COLLECTION)
                .targetType(elementType)
                .collectionType(collectionType)
                .fetch(RelationshipModel.FetchType.LAZY)
                .scope(RelationshipModel.RelationshipScope.INTRA_AGGREGATE)
                .build();
    }

    /**
     * Determines the relationship scope (intra vs inter-aggregate).
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>If target is in the same package → INTRA_AGGREGATE</li>
     *   <li>If target is AGGREGATE_ROOT → INTER_AGGREGATE</li>
     *   <li>Otherwise → INTRA_AGGREGATE (conservative)</li>
     * </ul>
     */
    private RelationshipModel.RelationshipScope determineScope(DomainTypeView targetType) {
        // If target is explicitly an aggregate root, it's inter-aggregate
        if (targetType.kind() == DomainTypeKind.AGGREGATE_ROOT) {
            return RelationshipModel.RelationshipScope.INTER_AGGREGATE;
        }

        // Check if target is in the same package as the owning type
        String owningPackage = extractPackageName(owningDomainTypeName);
        String targetPackage = extractPackageName(targetType.qualifiedName());

        if (owningPackage.equals(targetPackage)) {
            return RelationshipModel.RelationshipScope.INTRA_AGGREGATE;
        }

        // Different package → likely inter-aggregate
        return RelationshipModel.RelationshipScope.INTER_AGGREGATE;
    }

    /**
     * Checks if a type is a simple type (primitive, wrapper, String, enum, date).
     */
    private boolean isSimpleType(TypeRef type) {
        String typeName = type.render();

        // Primitives
        if (Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char")
                .contains(typeName)) {
            return true;
        }

        // Wrappers and String
        if (Set.of(
                        "java.lang.Boolean",
                        "java.lang.Byte",
                        "java.lang.Short",
                        "java.lang.Integer",
                        "java.lang.Long",
                        "java.lang.Float",
                        "java.lang.Double",
                        "java.lang.Character",
                        "java.lang.String",
                        "Boolean",
                        "Byte",
                        "Short",
                        "Integer",
                        "Long",
                        "Float",
                        "Double",
                        "Character",
                        "String")
                .contains(typeName)) {
            return true;
        }

        // Date/Time types
        if (Set.of(
                        "java.time.LocalDate",
                        "java.time.LocalDateTime",
                        "java.time.Instant",
                        "java.time.ZonedDateTime",
                        "java.util.Date")
                .contains(typeName)) {
            return true;
        }

        return false;
    }

    /**
     * Maps collection kind to JPA collection type.
     */
    private RelationshipModel.CollectionType mapCollectionType(io.hexaglue.spi.types.CollectionKind kind) {
        return switch (kind) {
            case LIST -> RelationshipModel.CollectionType.LIST;
            case SET -> RelationshipModel.CollectionType.SET;
            case MAP -> RelationshipModel.CollectionType.MAP;
            case COLLECTION -> RelationshipModel.CollectionType.LIST; // Default to LIST for generic collections
        };
    }

    /**
     * Extracts simple name from a type reference.
     */
    private String extractSimpleName(TypeRef type) {
        String qualifiedName = type.render();
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Extracts package name from a qualified type name.
     */
    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
