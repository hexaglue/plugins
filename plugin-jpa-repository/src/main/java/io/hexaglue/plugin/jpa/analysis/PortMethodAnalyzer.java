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

import io.hexaglue.plugin.jpa.model.QueryMethodModel;
import io.hexaglue.plugin.jpa.model.QueryMethodModel.QueryParameter;
import io.hexaglue.plugin.jpa.model.QueryMethodModel.QueryType;
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes port methods to detect Spring Data JPA query patterns.
 *
 * <p>This analyzer examines port method signatures to identify derived query methods
 * that can be automatically implemented by Spring Data JPA.</p>
 *
 * <h2>Detected Patterns</h2>
 * <ul>
 *   <li><strong>findByX</strong>: Finds entities by property X</li>
 *   <li><strong>findByXAndY</strong>: Finds entities by properties X and Y</li>
 *   <li><strong>existsByX</strong>: Checks if entity exists with property X</li>
 *   <li><strong>countByX</strong>: Counts entities with property X</li>
 *   <li><strong>deleteByX</strong>: Deletes entities with property X</li>
 *   <li><strong>findAll(Pageable)</strong>: Pagination support</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PortMethodAnalyzer analyzer = new PortMethodAnalyzer();
 * Optional<QueryMethodModel> queryMethod = analyzer.analyzeMethod(portMethod);
 *
 * if (queryMethod.isPresent()) {
 *     // Generate Spring Data query method
 * }
 * }</pre>
 *
 * @since 0.4.0
 */
public final class PortMethodAnalyzer {

    // Patterns for query method detection
    private static final Pattern FIND_BY_PATTERN = Pattern.compile("^findBy([A-Z].*)$");
    private static final Pattern EXISTS_BY_PATTERN = Pattern.compile("^existsBy([A-Z].*)$");
    private static final Pattern COUNT_BY_PATTERN = Pattern.compile("^countBy([A-Z].*)$");
    private static final Pattern DELETE_BY_PATTERN = Pattern.compile("^deleteBy([A-Z].*)$");

    // Property separator pattern (And, Or)
    private static final Pattern PROPERTY_SEPARATOR = Pattern.compile("(And|Or)");

    /**
     * Analyzes a port method to detect if it's a query method.
     *
     * @param portMethod the port method to analyze
     * @return QueryMethodModel if it's a query method, empty otherwise
     */
    public Optional<QueryMethodModel> analyzeMethod(PortMethodView portMethod) {
        Objects.requireNonNull(portMethod, "portMethod");

        String methodName = portMethod.name();
        TypeRef returnType = portMethod.returnType();
        List<PortParameterView> parameters = portMethod.parameters();

        // Check for findAll pattern with pagination (before checking basic CRUD)
        if (methodName.equals("findAll") && hasPageableParameter(parameters)) {
            return Optional.of(buildFindAllMethod(methodName, returnType, parameters));
        }

        // Skip basic CRUD methods already in JpaRepository
        if (isBasicCrudMethod(methodName)) {
            return Optional.empty();
        }

        // Check for findBy pattern
        Matcher findByMatcher = FIND_BY_PATTERN.matcher(methodName);
        if (findByMatcher.matches()) {
            return Optional.of(buildFindByMethod(methodName, returnType, parameters, findByMatcher.group(1)));
        }

        // Check for existsBy pattern
        Matcher existsByMatcher = EXISTS_BY_PATTERN.matcher(methodName);
        if (existsByMatcher.matches()) {
            return Optional.of(buildExistsByMethod(methodName, returnType, parameters, existsByMatcher.group(1)));
        }

        // Check for countBy pattern
        Matcher countByMatcher = COUNT_BY_PATTERN.matcher(methodName);
        if (countByMatcher.matches()) {
            return Optional.of(buildCountByMethod(methodName, returnType, parameters, countByMatcher.group(1)));
        }

        // Check for deleteBy pattern
        Matcher deleteByMatcher = DELETE_BY_PATTERN.matcher(methodName);
        if (deleteByMatcher.matches()) {
            return Optional.of(buildDeleteByMethod(methodName, returnType, parameters, deleteByMatcher.group(1)));
        }

        // Not a recognized query pattern
        return Optional.empty();
    }

    /**
     * Checks if a method is a basic CRUD method already provided by JpaRepository.
     */
    private boolean isBasicCrudMethod(String methodName) {
        return methodName.equals("save")
                || methodName.equals("saveAll")
                || methodName.equals("findById")
                || methodName.equals("findAll")
                || methodName.equals("findAllById")
                || methodName.equals("count")
                || methodName.equals("existsById")
                || methodName.equals("deleteById")
                || methodName.equals("delete")
                || methodName.equals("deleteAll")
                || methodName.equals("deleteAllById");
    }

    /**
     * Builds a findAll query method model.
     */
    private QueryMethodModel buildFindAllMethod(
            String methodName, TypeRef returnType, List<PortParameterView> parameters) {

        List<QueryParameter> queryParams = buildQueryParameters(parameters, List.of());
        boolean hasPageable = hasPageableParameter(parameters);
        boolean returnsPage = isPageReturnType(returnType);

        return new QueryMethodModel(
                methodName,
                QueryType.FIND_ALL,
                queryParams,
                returnType,
                false, // returnsOptional
                false, // returnsList (Page is different)
                returnsPage,
                hasPageable);
    }

    /**
     * Builds a findBy query method model.
     */
    private QueryMethodModel buildFindByMethod(
            String methodName, TypeRef returnType, List<PortParameterView> parameters, String propertyExpression) {

        List<String> propertyNames = extractPropertyNames(propertyExpression);
        List<QueryParameter> queryParams = buildQueryParameters(parameters, propertyNames);

        boolean returnsOptional = isOptionalReturnType(returnType);
        boolean returnsList = isListReturnType(returnType);
        boolean returnsPage = isPageReturnType(returnType);
        boolean hasPageable = hasPageableParameter(parameters);

        return new QueryMethodModel(
                methodName,
                QueryType.FIND_BY,
                queryParams,
                returnType,
                returnsOptional,
                returnsList,
                returnsPage,
                hasPageable);
    }

    /**
     * Builds an existsBy query method model.
     */
    private QueryMethodModel buildExistsByMethod(
            String methodName, TypeRef returnType, List<PortParameterView> parameters, String propertyExpression) {

        List<String> propertyNames = extractPropertyNames(propertyExpression);
        List<QueryParameter> queryParams = buildQueryParameters(parameters, propertyNames);

        return new QueryMethodModel(
                methodName,
                QueryType.EXISTS_BY,
                queryParams,
                returnType,
                false, // returnsOptional
                false, // returnsList
                false, // returnsPage
                false); // hasPagination (existsBy doesn't support pagination)
    }

    /**
     * Builds a countBy query method model.
     */
    private QueryMethodModel buildCountByMethod(
            String methodName, TypeRef returnType, List<PortParameterView> parameters, String propertyExpression) {

        List<String> propertyNames = extractPropertyNames(propertyExpression);
        List<QueryParameter> queryParams = buildQueryParameters(parameters, propertyNames);

        return new QueryMethodModel(
                methodName,
                QueryType.COUNT_BY,
                queryParams,
                returnType,
                false, // returnsOptional
                false, // returnsList
                false, // returnsPage
                false); // hasPagination (countBy doesn't support pagination)
    }

    /**
     * Builds a deleteBy query method model.
     */
    private QueryMethodModel buildDeleteByMethod(
            String methodName, TypeRef returnType, List<PortParameterView> parameters, String propertyExpression) {

        List<String> propertyNames = extractPropertyNames(propertyExpression);
        List<QueryParameter> queryParams = buildQueryParameters(parameters, propertyNames);

        return new QueryMethodModel(
                methodName,
                QueryType.DELETE_BY,
                queryParams,
                returnType,
                false, // returnsOptional
                false, // returnsList
                false, // returnsPage
                false); // hasPagination (deleteBy doesn't support pagination)
    }

    /**
     * Extracts property names from a property expression.
     *
     * <p>Example: "EmailAndStatus" → ["Email", "Status"]</p>
     */
    private List<String> extractPropertyNames(String propertyExpression) {
        // Split by And/Or
        String[] parts = PROPERTY_SEPARATOR.split(propertyExpression);

        List<String> propertyNames = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                // Convert to camelCase (Email → email)
                String propertyName = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                propertyNames.add(propertyName);
            }
        }

        return propertyNames;
    }

    /**
     * Builds query parameters from port parameters and property names.
     */
    private List<QueryParameter> buildQueryParameters(
            List<PortParameterView> portParameters, List<String> propertyNames) {

        List<QueryParameter> queryParams = new ArrayList<>();

        // Match property names with port parameters
        int propertyIndex = 0;
        for (PortParameterView portParam : portParameters) {
            String paramName = portParam.name();
            TypeRef paramType = portParam.type();

            // Check if it's a Pageable or Sort parameter
            if (isPageableType(paramType) || isSortType(paramType)) {
                queryParams.add(new QueryParameter(paramName, paramType, ""));
            } else if (propertyIndex < propertyNames.size()) {
                // Map to property name
                String propertyPath = propertyNames.get(propertyIndex);
                queryParams.add(new QueryParameter(paramName, paramType, propertyPath));
                propertyIndex++;
            } else {
                // Extra parameter - map to its own name
                queryParams.add(new QueryParameter(paramName, paramType, paramName));
            }
        }

        return queryParams;
    }

    /**
     * Checks if return type is Optional.
     */
    private boolean isOptionalReturnType(TypeRef returnType) {
        String typeName = returnType.render();
        return typeName.startsWith("Optional<") || typeName.startsWith("java.util.Optional<");
    }

    /**
     * Checks if return type is List or Collection.
     */
    private boolean isListReturnType(TypeRef returnType) {
        String typeName = returnType.render();
        return typeName.startsWith("List<")
                || typeName.startsWith("java.util.List<")
                || typeName.startsWith("Collection<")
                || typeName.startsWith("java.util.Collection<")
                || typeName.startsWith("Set<")
                || typeName.startsWith("java.util.Set<");
    }

    /**
     * Checks if return type is Page.
     */
    private boolean isPageReturnType(TypeRef returnType) {
        String typeName = returnType.render();
        return typeName.startsWith("Page<") || typeName.startsWith("org.springframework.data.domain.Page<");
    }

    /**
     * Checks if parameters include a Pageable parameter.
     */
    private boolean hasPageableParameter(List<PortParameterView> parameters) {
        return parameters.stream().anyMatch(p -> isPageableType(p.type()));
    }

    /**
     * Checks if type is Pageable.
     */
    private boolean isPageableType(TypeRef type) {
        String typeName = type.render();
        return typeName.equals("Pageable") || typeName.equals("org.springframework.data.domain.Pageable");
    }

    /**
     * Checks if type is Sort.
     */
    private boolean isSortType(TypeRef type) {
        String typeName = type.render();
        return typeName.equals("Sort") || typeName.equals("org.springframework.data.domain.Sort");
    }
}
