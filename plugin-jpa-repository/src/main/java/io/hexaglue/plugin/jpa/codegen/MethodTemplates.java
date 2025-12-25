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
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;

/**
 * Templates for generating common method patterns.
 *
 * <p>This class provides factory methods for generating standard Java patterns:</p>
 * <ul>
 *   <li><strong>Getters/Setters</strong>: Standard JavaBean accessors</li>
 *   <li><strong>equals/hashCode</strong>: Based on specified fields</li>
 *   <li><strong>No-arg constructors</strong>: Required by JPA</li>
 *   <li><strong>toString</strong>: For debugging</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Instead of manually building getters:
 * MethodSpec getter = MethodSpec.methodBuilder("getName")
 *     .addModifiers(Modifier.PUBLIC)
 *     .returns(String.class)
 *     .addStatement("return name")
 *     .build();
 *
 * // Use template:
 * MethodSpec getter = MethodTemplates.getter("name", TypeName.get(String.class));
 * }</pre>
 *
 * @since 0.4.0
 */
public final class MethodTemplates {

    private MethodTemplates() {
        // Prevent instantiation
    }

    /**
     * Builds a getter method.
     *
     * @param fieldName field name (e.g., "name")
     * @param fieldType field type
     * @return getter method spec
     */
    public static MethodSpec getter(String fieldName, TypeName fieldType) {
        Objects.requireNonNull(fieldName, "fieldName");
        Objects.requireNonNull(fieldType, "fieldType");

        String methodName = "get" + capitalize(fieldName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return $L", fieldName)
                .build();
    }

    /**
     * Builds a setter method.
     *
     * @param fieldName field name (e.g., "name")
     * @param fieldType field type
     * @return setter method spec
     */
    public static MethodSpec setter(String fieldName, TypeName fieldType) {
        Objects.requireNonNull(fieldName, "fieldName");
        Objects.requireNonNull(fieldType, "fieldType");

        String methodName = "set" + capitalize(fieldName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$L = $L", fieldName, fieldName)
                .build();
    }

    /**
     * Builds a no-arg constructor.
     *
     * @param javadoc optional Javadoc comment
     * @return no-arg constructor spec
     */
    public static MethodSpec noArgConstructor(String javadoc) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        if (javadoc != null && !javadoc.isBlank()) {
            builder.addJavadoc(javadoc);
        }

        return builder.build();
    }

    /**
     * Builds a no-arg constructor with default JPA Javadoc.
     *
     * @return no-arg constructor spec
     */
    public static MethodSpec noArgConstructor() {
        return noArgConstructor("No-arg constructor required by JPA.\n");
    }

    /**
     * Builds an equals method based on specified fields.
     *
     * @param className class name for casting
     * @param fieldNames fields to compare
     * @return equals method spec
     */
    public static MethodSpec equals(String className, List<String> fieldNames) {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(fieldNames, "fieldNames");

        if (fieldNames.isEmpty()) {
            throw new IllegalArgumentException("fieldNames cannot be empty");
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(ClassName.get("java.lang", "Object"), "o")
                .addStatement("if (this == o) return true")
                .addStatement("if (o == null || getClass() != o.getClass()) return false")
                .addStatement("$L that = ($L) o", className, className);

        // Build comparison
        if (fieldNames.size() == 1) {
            String field = fieldNames.get(0);
            builder.addStatement("return $T.equals($L, that.$L)", ClassName.get("java.util", "Objects"), field, field);
        } else {
            StringBuilder comparison = new StringBuilder("return ");
            for (int i = 0; i < fieldNames.size(); i++) {
                if (i > 0) {
                    comparison.append(" && ");
                }
                String field = fieldNames.get(i);
                comparison
                        .append("$T.equals(")
                        .append(field)
                        .append(", that.")
                        .append(field)
                        .append(")");
            }
            builder.addStatement(comparison.toString(), ClassName.get("java.util", "Objects"));
        }

        return builder.build();
    }

    /**
     * Builds a hashCode method based on specified fields.
     *
     * @param fieldNames fields to hash
     * @return hashCode method spec
     */
    public static MethodSpec hashCode(List<String> fieldNames) {
        Objects.requireNonNull(fieldNames, "fieldNames");

        if (fieldNames.isEmpty()) {
            throw new IllegalArgumentException("fieldNames cannot be empty");
        }

        String hashParams = String.join(", ", fieldNames);

        return MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return $T.hash($L)", ClassName.get("java.util", "Objects"), hashParams)
                .build();
    }

    /**
     * Builds a toString method with specified fields.
     *
     * @param className class name
     * @param fieldNames fields to include
     * @return toString method spec
     */
    public static MethodSpec toString(String className, List<String> fieldNames) {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(fieldNames, "fieldNames");

        StringBuilder format = new StringBuilder(className).append("{");
        for (int i = 0; i < fieldNames.size(); i++) {
            if (i > 0) format.append(", ");
            format.append(fieldNames.get(i)).append("=%s");
        }
        format.append("}");

        // Build arguments list
        String args = fieldNames.stream().map(name -> ", " + name).reduce("", String::concat);

        return MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("java.lang", "String"))
                .addStatement("return $T.format($S$L)", ClassName.get("java.lang", "String"), format.toString(), args)
                .build();
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str string to capitalize
     * @return capitalized string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
