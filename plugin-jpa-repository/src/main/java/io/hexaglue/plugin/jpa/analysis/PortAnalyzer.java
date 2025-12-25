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

import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.TypeRef;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Analyzes ports to detect repository-like interfaces and extract metadata.
 *
 * <p>This analyzer identifies DRIVEN ports that look like persistence repositories
 * and extracts information needed for JPA generation:</p>
 *
 * <h2>Repository Detection Heuristics</h2>
 * <ul>
 *   <li><strong>Name patterns</strong>: Contains "Repository", "Repo", or "Store"</li>
 *   <li><strong>Method patterns</strong>: Has CRUD methods (save, find, delete, exists, count)</li>
 * </ul>
 *
 * <h2>Domain Type Inference</h2>
 * <p>Infers the managed domain type from method signatures:</p>
 * <ol>
 *   <li>Return type of {@code save(X)} method</li>
 *   <li>Parameter type of {@code save(X)} method</li>
 *   <li>Element type of {@code findAll()} return type</li>
 *   <li>First parameter type of any method</li>
 * </ol>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // CustomerRepository port
 * interface CustomerRepository {
 *     Customer save(Customer customer);      // Domain type: Customer
 *     Optional<Customer> findById(CustomerId id); // ID type: CustomerId
 *     List<Customer> findAll();              // Domain type: Customer
 *     boolean existsById(CustomerId id);
 *     void deleteById(CustomerId id);
 *     long count();
 * }
 * }</pre>
 *
 * @since 0.4.0
 */
public final class PortAnalyzer {

    private PortAnalyzer() {
        // Static utility class
    }

    /**
     * Checks if a port looks like a repository interface.
     *
     * <p>Uses name-based and method-based heuristics to identify repository ports.</p>
     *
     * <p><strong>IMPORTANT</strong>: Excludes generated Spring Data repositories to prevent
     * infinite recursion. Generated interfaces are in *.infrastructure.persistence.* packages.</p>
     *
     * @param port port to analyze
     * @return true if port appears to be a repository, false otherwise
     */
    public static boolean looksLikeRepository(PortView port) {
        Objects.requireNonNull(port, "port");

        // CRITICAL: Exclude generated artifacts to prevent infinite recursion
        String qualifiedName = port.qualifiedName();
        if (qualifiedName.contains(".infrastructure.persistence.")) {
            return false;
        }

        // Name-based heuristic
        String name = port.simpleName().toLowerCase(Locale.ROOT);
        if (name.contains("repository") || name.contains("repo") || name.contains("store")) {
            return true;
        }

        // Method-based heuristic: has CRUD methods
        for (PortMethodView method : port.methods()) {
            String methodName = method.name().toLowerCase(Locale.ROOT);
            if (methodName.startsWith("save")
                    || methodName.startsWith("find")
                    || methodName.startsWith("delete")
                    || methodName.startsWith("exists")
                    || methodName.equals("count")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Infers the domain type managed by a repository port.
     *
     * <p>Analyzes method signatures to determine the entity type:</p>
     * <ol>
     *   <li>Return type of {@code save(X)} → X</li>
     *   <li>Parameter type of {@code save(X)} → X</li>
     *   <li>Return type element of {@code findById(ID)} → Optional&lt;X&gt; → X</li>
     *   <li>Element type of {@code findAll()} → List&lt;X&gt; → X</li>
     *   <li>First parameter of any method as fallback</li>
     * </ol>
     *
     * @param port port to analyze
     * @return inferred domain type or empty if cannot determine
     */
    public static Optional<TypeRef> inferDomainType(PortView port) {
        Objects.requireNonNull(port, "port");

        // Strategy 1: Look for save(X) method
        for (PortMethodView method : port.methods()) {
            String name = method.name().toLowerCase(Locale.ROOT);
            if (name.equals("save") && !method.parameters().isEmpty()) {
                // save(Customer) -> Customer or save(Customer) -> Customer
                TypeRef returnType = method.returnType();
                TypeRef paramType = method.parameters().get(0).type();

                // Prefer return type if not void
                if (!returnType.render().equals("void")) {
                    return Optional.of(returnType);
                }
                return Optional.of(paramType);
            }
        }

        // Strategy 2: Look for findById(ID) -> Optional<X>
        for (PortMethodView method : port.methods()) {
            String name = method.name().toLowerCase(Locale.ROOT);
            if (name.equals("findbyid") && !method.parameters().isEmpty()) {
                TypeRef returnType = method.returnType();
                String rendered = returnType.render();

                // Extract X from Optional<X>
                if (rendered.startsWith("java.util.Optional<") && returnType instanceof ParameterizedRef paramRef) {
                    if (!paramRef.typeArguments().isEmpty()) {
                        // Return the domain type X, not Optional<X>
                        return Optional.of(paramRef.typeArguments().get(0));
                    }
                }
            }
        }

        // Strategy 3: Fallback - first parameter of any method
        for (PortMethodView method : port.methods()) {
            if (!method.parameters().isEmpty()) {
                return Optional.of(method.parameters().get(0).type());
            }
        }

        return Optional.empty();
    }
}
