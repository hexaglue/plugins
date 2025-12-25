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
 * Model for Spring Data JPA derived query methods.
 *
 * <p>This class represents a query method that Spring Data JPA will implement automatically
 * based on the method name and signature.</p>
 *
 * <h2>Supported Query Patterns</h2>
 * <ul>
 *   <li><strong>findBy</strong>: Returns entities matching criteria</li>
 *   <li><strong>existsBy</strong>: Checks if matching entities exist</li>
 *   <li><strong>countBy</strong>: Counts matching entities</li>
 *   <li><strong>deleteBy</strong>: Deletes matching entities</li>
 * </ul>
 *
 * <h2>Query Examples</h2>
 * <pre>{@code
 * // Simple property query
 * Optional<Customer> findByEmail(String email);
 *
 * // Multiple properties
 * List<Customer> findByNameAndStatus(String name, CustomerStatus status);
 *
 * // With pagination
 * Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
 *
 * // Existence check
 * boolean existsByEmail(String email);
 *
 * // Count
 * long countByStatus(CustomerStatus status);
 * }</pre>
 *
 * @since 0.4.0
 */
public record QueryMethodModel(
        String methodName,
        QueryType queryType,
        List<QueryParameter> parameters,
        TypeRef returnType,
        boolean returnsOptional,
        boolean returnsList,
        boolean returnsPage,
        boolean hasPagination) {

    /**
     * Query method type based on method name prefix.
     */
    public enum QueryType {
        /** Finds entities matching criteria (findBy...) */
        FIND_BY,

        /** Checks existence of entities (existsBy...) */
        EXISTS_BY,

        /** Counts matching entities (countBy...) */
        COUNT_BY,

        /** Deletes matching entities (deleteBy...) */
        DELETE_BY,

        /** findAll with pagination support */
        FIND_ALL
    }

    /**
     * Query parameter representing a filter criterion.
     */
    public record QueryParameter(String name, TypeRef type, String propertyPath) {

        public QueryParameter {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(propertyPath, "propertyPath");
        }

        /**
         * Checks if this is a Pageable parameter.
         */
        public boolean isPageable() {
            return type.render().equals("org.springframework.data.domain.Pageable")
                    || type.render().equals("Pageable");
        }

        /**
         * Checks if this is a Sort parameter.
         */
        public boolean isSort() {
            return type.render().equals("org.springframework.data.domain.Sort")
                    || type.render().equals("Sort");
        }
    }

    public QueryMethodModel {
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(queryType, "queryType");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(returnType, "returnType");
        parameters = List.copyOf(parameters);
    }

    /**
     * Gets the filter parameters (excluding Pageable/Sort).
     */
    public List<QueryParameter> filterParameters() {
        return parameters.stream().filter(p -> !p.isPageable() && !p.isSort()).toList();
    }

    /**
     * Checks if this method has a Pageable parameter.
     */
    public boolean hasPageableParameter() {
        return parameters.stream().anyMatch(QueryParameter::isPageable);
    }

    /**
     * Gets the Pageable parameter if present.
     */
    public Optional<QueryParameter> pageableParameter() {
        return parameters.stream().filter(QueryParameter::isPageable).findFirst();
    }

    /**
     * Checks if this is a simple single-result query (Optional or single entity).
     */
    public boolean isSingleResult() {
        return returnsOptional || (!returnsList && !returnsPage);
    }

    /**
     * Checks if this method returns a collection (List or Page).
     */
    public boolean returnsCollection() {
        return returnsList || returnsPage;
    }

    /**
     * Gets the query predicate string for Spring Data (e.g., "ByEmailAndStatus").
     */
    public String queryPredicate() {
        if (queryType == QueryType.FIND_ALL) {
            return "";
        }

        // Extract predicate from method name
        String predicate = methodName;
        if (methodName.startsWith("findBy")) {
            predicate = methodName.substring("findBy".length());
        } else if (methodName.startsWith("existsBy")) {
            predicate = methodName.substring("existsBy".length());
        } else if (methodName.startsWith("countBy")) {
            predicate = methodName.substring("countBy".length());
        } else if (methodName.startsWith("deleteBy")) {
            predicate = methodName.substring("deleteBy".length());
        }

        return predicate;
    }

    /**
     * Gets the derived query method signature for Spring Data repository.
     */
    public String repositoryMethodSignature() {
        StringBuilder signature = new StringBuilder();

        // Return type
        if (returnsPage) {
            signature.append("Page<");
        } else if (returnsList) {
            signature.append("List<");
        } else if (returnsOptional) {
            signature.append("Optional<");
        } else if (queryType == QueryType.EXISTS_BY) {
            signature.append("boolean");
        } else if (queryType == QueryType.COUNT_BY) {
            signature.append("long");
        } else if (queryType == QueryType.DELETE_BY) {
            signature.append("void");
        }

        // Entity type (for collection returns)
        if (returnsPage || returnsList || returnsOptional) {
            signature.append("Entity");
        }

        // Close generic brackets
        if (returnsPage || returnsList || returnsOptional) {
            signature.append(">");
        }

        signature.append(" ").append(methodName).append("(");

        // Parameters
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                signature.append(", ");
            }
            QueryParameter param = parameters.get(i);
            signature.append(param.type().render()).append(" ").append(param.name());
        }

        signature.append(")");

        return signature.toString();
    }
}
