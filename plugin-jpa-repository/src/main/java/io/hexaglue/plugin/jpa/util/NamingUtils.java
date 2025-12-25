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

import io.hexaglue.spi.util.Strings;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for JPA naming conventions.
 *
 * <p>Handles conversion between different naming styles commonly used in JPA:</p>
 * <ul>
 *   <li><strong>camelCase</strong> → Java property names</li>
 *   <li><strong>PascalCase</strong> → Java class names</li>
 *   <li><strong>snake_case</strong> → Database table/column names</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Convert to snake_case
 * NamingUtils.toSnakeCase("customerOrder") // → "customer_order"
 * NamingUtils.toSnakeCase("OrderItem")     // → "order_item"
 *
 * // Pluralization
 * NamingUtils.pluralize("customer")  // → "customers"
 * NamingUtils.pluralize("order")     // → "orders"
 * NamingUtils.pluralize("address")   // → "addresses"
 *
 * // Entity name inference
 * NamingUtils.inferEntityName("CustomerRepository") // → "Customer"
 * NamingUtils.inferEntityName("OrderRepo")          // → "Order"
 * }</pre>
 *
 * @since 0.4.0
 */
public final class NamingUtils {

    private NamingUtils() {
        // Prevent instantiation
    }

    /**
     * Converts a camelCase or PascalCase string to snake_case.
     *
     * <p>Examples:</p>
     * <pre>
     * customerName     → customer_name
     * OrderItem        → order_item
     * HTTPSConnection  → https_connection
     * </pre>
     *
     * @param str string to convert (must not be null)
     * @return snake_case version of the string
     * @throws NullPointerException if str is null
     */
    public static String toSnakeCase(String str) {
        Objects.requireNonNull(str, "str");

        if (str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // Insert underscore before uppercase letters (except at start)
            if (Character.isUpperCase(c) && i > 0) {
                // Don't add underscore if previous char was also uppercase (acronym handling)
                char prev = str.charAt(i - 1);
                if (!Character.isUpperCase(prev)) {
                    result.append('_');
                }
            }

            result.append(Character.toLowerCase(c));
        }

        return result.toString();
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str string to capitalize
     * @return capitalized string, or empty string if input is null/empty
     */
    public static String capitalize(String str) {
        if (Strings.isBlank(str)) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Pluralizes a singular noun using simple English rules.
     *
     * <p>Handles common patterns:</p>
     * <ul>
     *   <li>Words ending in 's', 'x', 'z', 'ch', 'sh' → add 'es'</li>
     *   <li>Words ending in consonant + 'y' → 'ies'</li>
     *   <li>Most other words → add 's'</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This is a simple implementation. Complex irregular plurals
     * (person→people, mouse→mice) are not handled. Use YAML configuration for custom table names.</p>
     *
     * @param singular singular noun
     * @return pluralized form
     * @throws NullPointerException if singular is null
     */
    public static String pluralize(String singular) {
        Objects.requireNonNull(singular, "singular");

        if (singular.isEmpty()) {
            return singular;
        }

        String lower = singular.toLowerCase(Locale.ROOT);

        // Already plural
        if (lower.endsWith("s")) {
            return singular;
        }

        // Words ending in s, x, z, ch, sh → add 'es'
        if (lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch") || lower.endsWith("sh")) {
            return singular + "es";
        }

        // Words ending in consonant + y → replace y with 'ies'
        if (lower.endsWith("y") && singular.length() > 1) {
            char beforeY = lower.charAt(lower.length() - 2);
            if (!isVowel(beforeY)) {
                return singular.substring(0, singular.length() - 1) + "ies";
            }
        }

        // Default: add 's'
        return singular + "s";
    }

    /**
     * Infers entity name from a repository interface name.
     *
     * <p>Strips common suffixes like "Repository", "Repo", "Store":</p>
     * <pre>
     * CustomerRepository → Customer
     * OrderRepo          → Order
     * ProductStore       → Product
     * </pre>
     *
     * @param repositoryName repository interface simple name
     * @return inferred entity name, or "Entity" if empty
     * @throws NullPointerException if repositoryName is null
     */
    public static String inferEntityName(String repositoryName) {
        Objects.requireNonNull(repositoryName, "repositoryName");

        String name = repositoryName;

        // Remove common suffixes
        if (name.endsWith("Repository")) {
            name = name.substring(0, name.length() - "Repository".length());
        } else if (name.endsWith("Repo")) {
            name = name.substring(0, name.length() - "Repo".length());
        } else if (name.endsWith("Store")) {
            name = name.substring(0, name.length() - "Store".length());
        }

        // Fallback if name is empty
        return Strings.isBlank(name) ? "Entity" : name;
    }

    /**
     * Generates table name from entity name using snake_case + pluralization.
     *
     * <p>Example:</p>
     * <pre>
     * Customer     → customers
     * OrderItem    → order_items
     * ProductPhoto → product_photos
     * </pre>
     *
     * @param entityName entity name (PascalCase)
     * @return table name (snake_case, plural)
     * @throws NullPointerException if entityName is null
     */
    public static String toTableName(String entityName) {
        Objects.requireNonNull(entityName, "entityName");

        String snakeCase = toSnakeCase(entityName);
        return pluralize(snakeCase);
    }

    /**
     * Generates column name from property name using snake_case.
     *
     * @param propertyName property name (camelCase)
     * @return column name (snake_case)
     * @throws NullPointerException if propertyName is null
     */
    public static String toColumnName(String propertyName) {
        return toSnakeCase(propertyName);
    }

    /**
     * Checks if a character is a vowel.
     *
     * @param c character to check
     * @return true if vowel (a, e, i, o, u), false otherwise
     */
    private static boolean isVowel(char c) {
        char lower = Character.toLowerCase(c);
        return lower == 'a' || lower == 'e' || lower == 'i' || lower == 'o' || lower == 'u';
    }
}
