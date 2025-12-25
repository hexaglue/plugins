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
package io.hexaglue.plugin.jpa.util;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;

/**
 * Utility class for type conversions between HexaGlue SPI and JavaPoet.
 *
 * <p>This class handles the conversion from {@link TypeRef} (HexaGlue's type representation)
 * to JavaPoet's {@link TypeName} for code generation.</p>
 *
 * <h2>Supported Conversions</h2>
 * <ul>
 *   <li>Primitive types (int, long, boolean, etc.)</li>
 *   <li>Common wrapper types (Integer, Long, String, etc.)</li>
 *   <li>Generic types (Optional&lt;T&gt;, List&lt;T&gt;, etc.)</li>
 *   <li>Custom domain types</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class TypeUtils {

    private TypeUtils() {
        // Prevent instantiation
    }

    /**
     * Converts a TypeRef to a JavaPoet TypeName.
     *
     * <p>This method handles:</p>
     * <ul>
     *   <li>Primitive types (int → TypeName.INT)</li>
     *   <li>Common generic types (Optional&lt;X&gt; → ParameterizedTypeName)</li>
     *   <li>Qualified class names (com.example.Customer → ClassName)</li>
     *   <li>Null types → Object.class fallback</li>
     * </ul>
     *
     * @param typeRef type reference to convert (may be null)
     * @return JavaPoet TypeName (never null)
     */
    public static TypeName toTypeName(TypeRef typeRef) {
        if (typeRef == null) {
            return ClassName.get(Object.class);
        }

        return parseTypeName(typeRef.render());
    }

    /**
     * Parses a type string into a JavaPoet TypeName.
     *
     * <p>Handles primitive types, generic types, and qualified class names.</p>
     *
     * @param typeString type string from TypeRef.render()
     * @return JavaPoet TypeName
     */
    private static TypeName parseTypeName(String typeString) {
        Objects.requireNonNull(typeString, "typeString");

        // Handle primitive types
        switch (typeString) {
            case "void":
                return TypeName.VOID;
            case "boolean":
                return TypeName.BOOLEAN;
            case "byte":
                return TypeName.BYTE;
            case "short":
                return TypeName.SHORT;
            case "int":
                return TypeName.INT;
            case "long":
                return TypeName.LONG;
            case "char":
                return TypeName.CHAR;
            case "float":
                return TypeName.FLOAT;
            case "double":
                return TypeName.DOUBLE;
        }

        // Handle common generic types
        if (typeString.startsWith("java.util.Optional<")) {
            return parseParameterizedType("java.util.Optional", typeString);
        }

        if (typeString.startsWith("java.util.List<")) {
            return parseParameterizedType("java.util.List", typeString);
        }

        if (typeString.startsWith("java.util.Set<")) {
            return parseParameterizedType("java.util.Set", typeString);
        }

        if (typeString.startsWith("java.util.Map<")) {
            return parseMapType(typeString);
        }

        // Default: use bestGuess for qualified class names
        try {
            return ClassName.bestGuess(typeString);
        } catch (IllegalArgumentException e) {
            // Fallback to Object if we can't guess
            return ClassName.get(Object.class);
        }
    }

    /**
     * Parses a parameterized type with a single type argument.
     *
     * <p>Example: {@code Optional<Customer>} → {@code ParameterizedTypeName}</p>
     *
     * @param rawType raw type name (e.g., "java.util.Optional")
     * @param typeString full type string
     * @return parameterized TypeName
     */
    private static TypeName parseParameterizedType(String rawType, String typeString) {
        // Extract type parameter: "java.util.Optional<X>" → "X"
        int start = typeString.indexOf('<') + 1;
        int end = typeString.lastIndexOf('>');

        if (start > 0 && end > start) {
            String innerType = typeString.substring(start, end);
            TypeName innerTypeName = parseTypeName(innerType);

            String[] parts = rawType.split("\\.");
            String packageName = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
            String simpleName = parts[parts.length - 1];

            return ParameterizedTypeName.get(ClassName.get(packageName, simpleName), innerTypeName);
        }

        // Fallback: raw type
        return ClassName.bestGuess(rawType);
    }

    /**
     * Parses a Map type with two type arguments.
     *
     * <p>Example: {@code Map<String, Customer>} → {@code ParameterizedTypeName}</p>
     *
     * @param typeString full type string
     * @return parameterized TypeName
     */
    private static TypeName parseMapType(String typeString) {
        // Extract type parameters: "java.util.Map<K, V>" → "K", "V"
        int start = typeString.indexOf('<') + 1;
        int end = typeString.lastIndexOf('>');

        if (start > 0 && end > start) {
            String params = typeString.substring(start, end);
            String[] parts = params.split(",\\s*", 2);

            if (parts.length == 2) {
                TypeName keyType = parseTypeName(parts[0].trim());
                TypeName valueType = parseTypeName(parts[1].trim());

                return ParameterizedTypeName.get(ClassName.get("java.util", "Map"), keyType, valueType);
            }
        }

        // Fallback: raw Map type
        return ClassName.get("java.util", "Map");
    }

    /**
     * Checks if a type is a String type.
     *
     * @param typeRef type reference to check
     * @return true if String, false otherwise
     */
    public static boolean isStringType(TypeRef typeRef) {
        if (typeRef == null) {
            return false;
        }

        String rendered = typeRef.render();
        return "java.lang.String".equals(rendered) || "String".equals(rendered);
    }

    /**
     * Checks if a type is a primitive type.
     *
     * @param typeRef type reference to check
     * @return true if primitive, false otherwise
     */
    public static boolean isPrimitiveType(TypeRef typeRef) {
        if (typeRef == null) {
            return false;
        }

        String rendered = typeRef.render();
        return "boolean".equals(rendered)
                || "byte".equals(rendered)
                || "short".equals(rendered)
                || "int".equals(rendered)
                || "long".equals(rendered)
                || "float".equals(rendered)
                || "double".equals(rendered)
                || "char".equals(rendered);
    }
}
