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
package io.hexaglue.plugin.jpa.heuristics;

import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.types.TypeRef;
import java.util.Optional;
import java.util.Set;

/**
 * JPA-specific property heuristics implementation.
 *
 * <p>This class combines multiple heuristic strategies:
 * <ul>
 *   <li><strong>Naming Convention</strong>: Analyzes property names (e.g., "email", "description")</li>
 *   <li><strong>Type Pattern</strong>: Analyzes property types (e.g., primitives, collections)</li>
 *   <li><strong>Structure</strong>: Analyzes property characteristics (e.g., ID properties)</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class JpaPropertyHeuristics implements PropertyHeuristicsDetector {

    private static final Set<String> TEXT_FIELD_KEYWORDS =
            Set.of("description", "comment", "notes", "content", "body", "text", "message");

    private static final Set<String> UNIQUE_FIELD_KEYWORDS = Set.of("email", "username", "code", "reference", "slug");

    private static final Set<String> TEMPORAL_TYPES =
            Set.of("java.time.LocalDateTime", "java.time.Instant", "java.time.ZonedDateTime", "java.util.Date");

    @Override
    public Optional<String> detectColumnType(DomainPropertyView property) {
        String lowerName = property.name().toLowerCase();
        TypeRef type = property.type();

        // Check if it's a String type first
        if (!isStringType(type)) {
            return Optional.empty();
        }

        // Text fields (CLOB)
        if (TEXT_FIELD_KEYWORDS.stream().anyMatch(lowerName::contains)) {
            return Optional.of("TEXT");
        }

        // Email addresses (RFC 5321 max length)
        if (lowerName.contains("email")) {
            return Optional.of("VARCHAR(320)");
        }

        // URLs and links
        if (lowerName.contains("url") || lowerName.contains("link") || lowerName.contains("website")) {
            return Optional.of("VARCHAR(2048)");
        }

        // Default: standard VARCHAR
        return Optional.of("VARCHAR(255)");
    }

    @Override
    public Optional<Boolean> detectNullability(DomainPropertyView property) {
        TypeRef type = property.type();
        String lowerName = property.name().toLowerCase();

        // Primitive types are always NOT NULL
        if (isPrimitiveType(type)) {
            return Optional.of(false);
        }

        // Collections should be NOT NULL (use empty collection instead of null)
        if (type.collectionMetadata().isPresent()) {
            return Optional.of(false);
        }

        // Temporal types (dates/timestamps) for auditing are usually NOT NULL
        if (isTemporalType(type)) {
            return Optional.of(false);
        }

        // ID properties are NOT NULL
        if (lowerName.contains("id") || lowerName.endsWith("id")) {
            return Optional.of(false);
        }

        // Email and username should be NOT NULL for business reasons
        if (lowerName.contains("email") || lowerName.contains("username")) {
            return Optional.of(false);
        }

        // No specific heuristic applies - let default handle it
        return Optional.empty();
    }

    @Override
    public Optional<Integer> detectMaxLength(DomainPropertyView property) {
        TypeRef type = property.type();
        String lowerName = property.name().toLowerCase();

        // Only apply to String types
        if (!isStringType(type)) {
            return Optional.empty();
        }

        // Codes and references are usually short
        if (lowerName.contains("code") || lowerName.contains("reference") || lowerName.contains("slug")) {
            return Optional.of(50);
        }

        // Names (but not filenames which can be longer)
        if (lowerName.contains("name") && !lowerName.contains("filename")) {
            return Optional.of(100);
        }

        // Titles
        if (lowerName.contains("title")) {
            return Optional.of(200);
        }

        // Email addresses (RFC 5321)
        if (lowerName.contains("email")) {
            return Optional.of(320);
        }

        // URLs
        if (lowerName.contains("url") || lowerName.contains("link")) {
            return Optional.of(2048);
        }

        // No specific heuristic applies - let default handle it
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> detectUniqueness(DomainPropertyView property) {
        String lowerName = property.name().toLowerCase();

        // Check if property name contains any unique field keyword
        // (email, username, code, reference, slug are typically unique identifiers)
        if (UNIQUE_FIELD_KEYWORDS.stream().anyMatch(lowerName::contains)) {
            return Optional.of(true);
        }

        // Default: not unique
        return Optional.of(false);
    }

    /**
     * Checks if a type is a String type.
     */
    private boolean isStringType(TypeRef type) {
        String typeName = type.render();
        return "java.lang.String".equals(typeName) || "String".equals(typeName);
    }

    /**
     * Checks if a type is a primitive type.
     */
    private boolean isPrimitiveType(TypeRef type) {
        String typeName = type.render();
        return Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char")
                .contains(typeName);
    }

    /**
     * Checks if a type is a temporal type (date/time).
     */
    private boolean isTemporalType(TypeRef type) {
        return TEMPORAL_TYPES.contains(type.render());
    }
}
