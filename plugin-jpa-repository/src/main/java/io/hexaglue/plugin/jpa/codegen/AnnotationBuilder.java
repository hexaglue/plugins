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
package io.hexaglue.plugin.jpa.codegen;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.model.PropertyModel;
import io.hexaglue.plugin.jpa.model.RelationshipModel;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory for creating common JPA annotation specs.
 *
 * <p>This builder centralizes the creation of JPA annotations to reduce
 * code duplication across generators.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Instead of:
 * AnnotationSpec column = AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Column"))
 *     .addMember("name", "$S", "customer_name")
 *     .addMember("nullable", "$L", false)
 *     .addMember("length", "$L", 100)
 *     .build();
 *
 * // Use:
 * AnnotationSpec column = AnnotationBuilder.column("customer_name", false, 100, null);
 * }</pre>
 *
 * @since 0.4.0
 */
public final class AnnotationBuilder {

    private AnnotationBuilder() {
        // Prevent instantiation
    }

    /**
     * Builds @Entity annotation.
     *
     * @return @Entity annotation spec
     */
    public static AnnotationSpec entity() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Entity"))
                .build();
    }

    /**
     * Builds @Table annotation.
     *
     * @param tableName table name
     * @param schema optional schema name
     * @return @Table annotation spec
     */
    public static AnnotationSpec table(String tableName, Optional<String> schema) {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(schema, "schema");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Table"));
        builder.addMember("name", "$S", tableName);

        schema.ifPresent(s -> builder.addMember("schema", "$S", s));

        return builder.build();
    }

    /**
     * Builds @Id annotation.
     *
     * @return @Id annotation spec
     */
    public static AnnotationSpec id() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Id"))
                .build();
    }

    /**
     * Builds @EmbeddedId annotation.
     *
     * @return @EmbeddedId annotation spec
     */
    public static AnnotationSpec embeddedId() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "EmbeddedId"))
                .build();
    }

    /**
     * Builds @GeneratedValue annotation.
     *
     * @param strategy ID generation strategy
     * @param sequenceName optional sequence name for SEQUENCE strategy
     * @return @GeneratedValue annotation spec
     */
    public static AnnotationSpec generatedValue(
            JpaPluginOptions.IdGenerationStrategy strategy, Optional<String> sequenceName) {

        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(sequenceName, "sequenceName");

        ClassName generatedValueClass = ClassName.get("jakarta.persistence", "GeneratedValue");
        ClassName generationTypeClass = ClassName.get("jakarta.persistence", "GenerationType");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(generatedValueClass);

        switch (strategy) {
            case IDENTITY:
                builder.addMember("strategy", "$T.IDENTITY", generationTypeClass);
                break;
            case SEQUENCE:
                builder.addMember("strategy", "$T.SEQUENCE", generationTypeClass);
                sequenceName.ifPresent(seqName -> builder.addMember("generator", "$S", "seq"));
                break;
            case AUTO:
                builder.addMember("strategy", "$T.AUTO", generationTypeClass);
                break;
            default:
                // UUID and ASSIGNED don't use @GeneratedValue
                break;
        }

        return builder.build();
    }

    /**
     * Builds @Column annotation.
     *
     * @param columnName column name
     * @param nullable whether column is nullable
     * @param length max length (null for no limit)
     * @param unique whether column is unique
     * @return @Column annotation spec
     */
    public static AnnotationSpec column(String columnName, boolean nullable, Integer length, Boolean unique) {

        Objects.requireNonNull(columnName, "columnName");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Column"));

        builder.addMember("name", "$S", columnName);
        builder.addMember("nullable", "$L", nullable);

        if (length != null && length > 0) {
            builder.addMember("length", "$L", length);
        }

        if (unique != null && unique) {
            builder.addMember("unique", "$L", true);
        }

        return builder.build();
    }

    /**
     * Builds @Column annotation from PropertyModel.
     *
     * @param property property model
     * @return @Column annotation spec
     */
    public static AnnotationSpec column(PropertyModel property) {
        Objects.requireNonNull(property, "property");

        return column(property.columnName(), property.nullable(), property.length(), property.unique());
    }

    /**
     * Builds @Enumerated annotation.
     *
     * @param useOrdinal true for ORDINAL, false for STRING
     * @return @Enumerated annotation spec
     */
    public static AnnotationSpec enumerated(boolean useOrdinal) {
        ClassName enumeratedClass = ClassName.get("jakarta.persistence", "Enumerated");
        ClassName enumTypeClass = ClassName.get("jakarta.persistence", "EnumType");

        return AnnotationSpec.builder(enumeratedClass)
                .addMember("value", "$T.$L", enumTypeClass, useOrdinal ? "ORDINAL" : "STRING")
                .build();
    }

    /**
     * Builds @Version annotation for optimistic locking.
     *
     * @return @Version annotation spec
     */
    public static AnnotationSpec version() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Version"))
                .build();
    }

    /**
     * Builds @Embedded annotation.
     *
     * @return @Embedded annotation spec
     */
    public static AnnotationSpec embedded() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Embedded"))
                .build();
    }

    /**
     * Builds @Embeddable annotation.
     *
     * @return @Embeddable annotation spec
     */
    public static AnnotationSpec embeddable() {
        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Embeddable"))
                .build();
    }

    /**
     * Builds @OneToMany annotation.
     *
     * @param relationship relationship model
     * @return @OneToMany annotation spec
     */
    public static AnnotationSpec oneToMany(RelationshipModel relationship) {
        Objects.requireNonNull(relationship, "relationship");

        ClassName oneToManyClass = ClassName.get("jakarta.persistence", "OneToMany");
        ClassName cascadeTypeClass = ClassName.get("jakarta.persistence", "CascadeType");
        ClassName fetchTypeClass = ClassName.get("jakarta.persistence", "FetchType");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(oneToManyClass);

        // Add cascade
        if (relationship.cascade().length > 0) {
            String cascadeValues = buildCascadeValues(relationship.cascade(), cascadeTypeClass);
            builder.addMember("cascade", cascadeValues);
        }

        // Add fetch
        builder.addMember("fetch", "$T." + relationship.fetch().name(), fetchTypeClass);

        // Add orphanRemoval
        if (relationship.orphanRemoval()) {
            builder.addMember("orphanRemoval", "$L", true);
        }

        // Add mappedBy if present
        relationship.mappedByIfPresent().ifPresent(mappedBy -> builder.addMember("mappedBy", "$S", mappedBy));

        return builder.build();
    }

    /**
     * Builds @ManyToOne annotation.
     *
     * @param relationship relationship model
     * @return @ManyToOne annotation spec
     */
    public static AnnotationSpec manyToOne(RelationshipModel relationship) {
        Objects.requireNonNull(relationship, "relationship");

        ClassName manyToOneClass = ClassName.get("jakarta.persistence", "ManyToOne");
        ClassName fetchTypeClass = ClassName.get("jakarta.persistence", "FetchType");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(manyToOneClass);
        builder.addMember("fetch", "$T." + relationship.fetch().name(), fetchTypeClass);

        return builder.build();
    }

    /**
     * Builds @ElementCollection annotation.
     *
     * @param relationship relationship model
     * @return @ElementCollection annotation spec
     */
    public static AnnotationSpec elementCollection(RelationshipModel relationship) {
        Objects.requireNonNull(relationship, "relationship");

        ClassName elementCollectionClass = ClassName.get("jakarta.persistence", "ElementCollection");
        ClassName fetchTypeClass = ClassName.get("jakarta.persistence", "FetchType");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(elementCollectionClass);
        builder.addMember("fetch", "$T." + relationship.fetch().name(), fetchTypeClass);

        return builder.build();
    }

    /**
     * Builds @JoinColumn annotation.
     *
     * @param columnName join column name
     * @return @JoinColumn annotation spec
     */
    public static AnnotationSpec joinColumn(String columnName) {
        Objects.requireNonNull(columnName, "columnName");

        return AnnotationSpec.builder(ClassName.get("jakarta.persistence", "JoinColumn"))
                .addMember("name", "$S", columnName)
                .build();
    }

    /**
     * Builds cascade values string for annotation.
     *
     * @param cascadeTypes array of cascade types
     * @param cascadeTypeClass CascadeType class reference
     * @return formatted cascade values string
     */
    private static String buildCascadeValues(RelationshipModel.CascadeType[] cascadeTypes, ClassName cascadeTypeClass) {

        if (cascadeTypes.length == 1) {
            return "$T." + cascadeTypes[0].name();
        } else {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < cascadeTypes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("$T.").append(cascadeTypes[i].name());
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
