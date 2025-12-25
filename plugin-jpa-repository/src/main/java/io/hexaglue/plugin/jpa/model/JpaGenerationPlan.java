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

import io.hexaglue.spi.ir.ports.PortView;
import java.util.List;
import java.util.Objects;

/**
 * Complete generation plan for a JPA repository port.
 *
 * <p>This model represents the complete plan for generating all 4 artifacts:</p>
 * <ol>
 *   <li><strong>JPA Entity</strong>: @Entity class with persistence metadata</li>
 *   <li><strong>Spring Data Repository</strong>: JpaRepository interface</li>
 *   <li><strong>MapStruct Mapper</strong>: Domain ↔ Entity conversion</li>
 *   <li><strong>Adapter</strong>: Port implementation delegating to repository</li>
 * </ol>
 *
 * <h2>Generation Flow</h2>
 * <pre>
 * Port (analyzed) → JpaGenerationPlan (created) → 4 Artifacts (generated)
 *
 * 1. Analyze port methods and domain types
 * 2. Build EntityModel with all metadata
 * 3. Generate qualified names for all artifacts
 * 4. Generators use this plan to produce code
 * </pre>
 *
 * <h2>Artifact Naming</h2>
 * <p>Example for {@code CustomerRepository} port:</p>
 * <ul>
 *   <li>Entity: {@code com.example.persistence.entity.CustomerEntity}</li>
 *   <li>Spring Data Repo: {@code com.example.persistence.springdata.CustomerJpaRepository}</li>
 *   <li>Mapper: {@code com.example.persistence.mapper.CustomerMapper}</li>
 *   <li>Adapter: {@code com.example.persistence.adapter.CustomerAdapter}</li>
 * </ul>
 *
 * @param port the port being implemented
 * @param entityModel entity metadata model
 * @param entityQualifiedName fully qualified entity class name
 * @param springDataRepoQualifiedName fully qualified Spring Data repository interface name
 * @param mapperQualifiedName fully qualified MapStruct mapper interface name
 * @param adapterQualifiedName fully qualified adapter class name
 * @param queryMethods list of derived query methods to generate
 * @since 0.4.0
 */
public record JpaGenerationPlan(
        PortView port,
        EntityModel entityModel,
        String entityQualifiedName,
        String springDataRepoQualifiedName,
        String mapperQualifiedName,
        String adapterQualifiedName,
        List<QueryMethodModel> queryMethods) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public JpaGenerationPlan {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(entityModel, "entityModel");
        Objects.requireNonNull(entityQualifiedName, "entityQualifiedName");
        Objects.requireNonNull(springDataRepoQualifiedName, "springDataRepoQualifiedName");
        Objects.requireNonNull(mapperQualifiedName, "mapperQualifiedName");
        Objects.requireNonNull(adapterQualifiedName, "adapterQualifiedName");
        Objects.requireNonNull(queryMethods, "queryMethods");
        queryMethods = List.copyOf(queryMethods);
    }

    /**
     * Gets the port qualified name.
     *
     * @return port qualified name (e.g., "com.example.ports.CustomerRepository")
     */
    public String portQualifiedName() {
        return port.qualifiedName();
    }

    /**
     * Gets the port simple name.
     *
     * @return port simple name (e.g., "CustomerRepository")
     */
    public String portSimpleName() {
        return port.simpleName();
    }

    /**
     * Gets the entity simple class name.
     *
     * @return entity simple name (e.g., "CustomerEntity")
     */
    public String entitySimpleName() {
        return entityModel.entityClassName();
    }

    /**
     * Builder for constructing JpaGenerationPlan instances.
     */
    public static class Builder {
        private PortView port;
        private EntityModel entityModel;
        private String entityQualifiedName;
        private String springDataRepoQualifiedName;
        private String mapperQualifiedName;
        private String adapterQualifiedName;
        private List<QueryMethodModel> queryMethods = List.of();

        public Builder port(PortView port) {
            this.port = port;
            return this;
        }

        public Builder entityModel(EntityModel entityModel) {
            this.entityModel = entityModel;
            return this;
        }

        public Builder entityQualifiedName(String entityQualifiedName) {
            this.entityQualifiedName = entityQualifiedName;
            return this;
        }

        public Builder springDataRepoQualifiedName(String springDataRepoQualifiedName) {
            this.springDataRepoQualifiedName = springDataRepoQualifiedName;
            return this;
        }

        public Builder mapperQualifiedName(String mapperQualifiedName) {
            this.mapperQualifiedName = mapperQualifiedName;
            return this;
        }

        public Builder adapterQualifiedName(String adapterQualifiedName) {
            this.adapterQualifiedName = adapterQualifiedName;
            return this;
        }

        public Builder queryMethods(List<QueryMethodModel> queryMethods) {
            this.queryMethods = queryMethods;
            return this;
        }

        public JpaGenerationPlan build() {
            return new JpaGenerationPlan(
                    port,
                    entityModel,
                    entityQualifiedName,
                    springDataRepoQualifiedName,
                    mapperQualifiedName,
                    adapterQualifiedName,
                    queryMethods);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return generation plan builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
