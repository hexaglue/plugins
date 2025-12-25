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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.model.PropertyModel;
import java.util.Objects;
import javax.lang.model.element.Modifier;

/**
 * Templates for generating common field patterns.
 *
 * <p>This class provides factory methods for generating JPA entity fields
 * with appropriate annotations and modifiers.</p>
 *
 * <h2>Common Patterns</h2>
 * <ul>
 *   <li><strong>ID fields</strong>: @Id with @GeneratedValue</li>
 *   <li><strong>Version fields</strong>: @Version for optimistic locking</li>
 *   <li><strong>Audit fields</strong>: createdAt, updatedAt</li>
 *   <li><strong>Property fields</strong>: @Column with metadata</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class FieldTemplates {

    private FieldTemplates() {
        // Prevent instantiation
    }

    /**
     * Builds a version field for optimistic locking.
     *
     * @return version field spec with @Version annotation
     */
    public static FieldSpec versionField() {
        return FieldSpec.builder(TypeName.LONG.box(), "version", Modifier.PRIVATE)
                .addAnnotation(AnnotationBuilder.version())
                .addAnnotation(AnnotationBuilder.column("version", false, null, null))
                .build();
    }

    /**
     * Builds a createdAt audit field.
     *
     * @return createdAt field spec
     */
    public static FieldSpec createdAtField() {
        ClassName instantClass = ClassName.get("java.time", "Instant");

        return FieldSpec.builder(instantClass, "createdAt", Modifier.PRIVATE)
                .addAnnotation(AnnotationBuilder.column("created_at", false, null, null))
                .build();
    }

    /**
     * Builds an updatedAt audit field.
     *
     * @return updatedAt field spec
     */
    public static FieldSpec updatedAtField() {
        ClassName instantClass = ClassName.get("java.time", "Instant");

        return FieldSpec.builder(instantClass, "updatedAt", Modifier.PRIVATE)
                .addAnnotation(AnnotationBuilder.column("updated_at", false, null, null))
                .build();
    }

    /**
     * Builds a deletedAt soft delete field.
     *
     * @return deletedAt field spec
     */
    public static FieldSpec deletedAtField() {
        ClassName instantClass = ClassName.get("java.time", "Instant");

        return FieldSpec.builder(instantClass, "deletedAt", Modifier.PRIVATE)
                .addAnnotation(AnnotationBuilder.column("deleted_at", true, null, null))
                .build();
    }

    /**
     * Builds a property field with @Column annotation.
     *
     * @param property property model
     * @param fieldType field type
     * @return property field spec
     */
    public static FieldSpec propertyField(PropertyModel property, TypeName fieldType) {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(fieldType, "fieldType");

        FieldSpec.Builder builder = FieldSpec.builder(fieldType, property.name(), Modifier.PRIVATE);

        // Add @Column annotation
        builder.addAnnotation(AnnotationBuilder.column(property));

        // Add @Enumerated if it's an enum
        if (property.enumerated()) {
            builder.addAnnotation(AnnotationBuilder.enumerated(false)); // Use STRING by default
        }

        return builder.build();
    }

    /**
     * Builds a field with specified modifiers.
     *
     * @param type field type
     * @param name field name
     * @param modifiers field modifiers
     * @return field spec
     */
    public static FieldSpec simpleField(TypeName type, String name, Modifier... modifiers) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");

        return FieldSpec.builder(type, name, modifiers).build();
    }

    /**
     * Builds a field with initializer.
     *
     * @param type field type
     * @param name field name
     * @param initializer initializer code
     * @param modifiers field modifiers
     * @return field spec with initializer
     */
    public static FieldSpec fieldWithInitializer(
            TypeName type, String name, String initializer, Modifier... modifiers) {

        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(initializer, "initializer");

        return FieldSpec.builder(type, name, modifiers).initializer(initializer).build();
    }
}
