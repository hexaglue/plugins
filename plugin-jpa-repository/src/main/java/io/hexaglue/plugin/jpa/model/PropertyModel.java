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
 * Model representing JPA entity property metadata.
 *
 * <p>This model captures all information needed to generate a JPA entity field:</p>
 * <ul>
 *   <li><strong>Name</strong>: Java property name (camelCase)</li>
 *   <li><strong>Type</strong>: Property type reference</li>
 *   <li><strong>Column name</strong>: Database column name (snake_case)</li>
 *   <li><strong>Column metadata</strong>: Length, nullable, unique constraints</li>
 *   <li><strong>Special annotations</strong>: @Lob, @Enumerated, @Temporal, etc.</li>
 * </ul>
 *
 * <h2>Property Categories</h2>
 * <ul>
 *   <li><strong>Simple</strong>: Primitives, wrappers, String, enums, dates</li>
 *   <li><strong>Embedded</strong>: Value objects mapped with @Embedded</li>
 *   <li><strong>Collection</strong>: Lists, sets, maps (handled separately)</li>
 *   <li><strong>Relationship</strong>: References to other entities (@ManyToOne, etc.)</li>
 * </ul>
 *
 * @param name property name (camelCase, e.g., "customerName")
 * @param type property type reference
 * @param columnName database column name (snake_case, e.g., "customer_name")
 * @param length column length for String types (null if not applicable)
 * @param nullable true if column allows NULL values
 * @param unique true if column has unique constraint
 * @param lob true if property should use @Lob (large text/binary)
 * @param enumerated true if property is an enum type
 * @param temporal true if property is a temporal type (Date, LocalDate, etc.)
 * @param embedded true if property is a multi-field Value Object mapped with @Embedded
 * @since 0.4.0
 */
public record PropertyModel(
        String name,
        TypeRef type,
        String columnName,
        Integer length,
        boolean nullable,
        boolean unique,
        boolean lob,
        boolean enumerated,
        boolean temporal,
        boolean embedded) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if name, type, or columnName is null
     */
    public PropertyModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(columnName, "columnName");
    }

    /**
     * Gets the column length if applicable.
     *
     * @return column length or empty if not applicable
     */
    public Optional<Integer> lengthIfPresent() {
        return Optional.ofNullable(length);
    }

    /**
     * Checks if this property requires special handling.
     *
     * @return true if lob, enumerated, temporal, or embedded
     */
    public boolean requiresSpecialAnnotation() {
        return lob || enumerated || temporal || embedded;
    }

    /**
     * Builder for constructing PropertyModel instances.
     */
    public static class Builder {
        private String name;
        private TypeRef type;
        private String columnName;
        private Integer length;
        private boolean nullable = true; // JPA default
        private boolean unique = false;
        private boolean lob = false;
        private boolean enumerated = false;
        private boolean temporal = false;
        private boolean embedded = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder length(Integer length) {
            this.length = length;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder lob(boolean lob) {
            this.lob = lob;
            return this;
        }

        public Builder enumerated(boolean enumerated) {
            this.enumerated = enumerated;
            return this;
        }

        public Builder temporal(boolean temporal) {
            this.temporal = temporal;
            return this;
        }

        public Builder embedded(boolean embedded) {
            this.embedded = embedded;
            return this;
        }

        public PropertyModel build() {
            return new PropertyModel(
                    name, type, columnName, length, nullable, unique, lob, enumerated, temporal, embedded);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return property model builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
