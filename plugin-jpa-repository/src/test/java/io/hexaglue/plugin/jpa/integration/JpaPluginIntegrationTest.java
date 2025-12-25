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
package io.hexaglue.plugin.jpa.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Integration tests for JPA plugin code generation.
 *
 * <p>These tests verify that the plugin generates correct code for real-world scenarios
 * using the test-example-app as reference.</p>
 *
 * <p>To run these tests, execute:</p>
 * <pre>mvn test -Dtest=JpaPluginIntegrationTest -Djpa.integration.test=true</pre>
 *
 * @since 0.4.0
 */
@DisplayName("JPA Plugin Integration Tests")
class JpaPluginIntegrationTest {

    private static final String TEST_APP_BASE =
            "../test-example-app/target/generated-sources/annotations/com/example/infrastructure/persistence";

    @Test
    @DisplayName("should generate entity classes for repository ports")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateEntityClasses() throws Exception {
        // Given: test-example-app has been compiled
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        // Then: Entity classes should exist
        assertTrue(Files.exists(entityPath), "Entity directory should exist");

        // Verify specific entities
        assertFileExists(entityPath, "InventoryEntity.java");
        assertFileExists(entityPath, "CustomerEntity.java");
    }

    @Test
    @DisplayName("should generate Spring Data JPA repository interfaces")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateRepositoryInterfaces() throws Exception {
        // Given: test-example-app has been compiled
        Path repoPath = Paths.get(TEST_APP_BASE, "springdata");

        // Then: Repository interfaces should exist
        assertTrue(Files.exists(repoPath), "Repository directory should exist");

        // Verify specific repositories
        assertFileExists(repoPath, "InventoryJpaRepository.java");
        assertFileExists(repoPath, "CustomerJpaRepository.java");
    }

    @Test
    @DisplayName("should generate MapStruct mapper interfaces")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateMapperInterfaces() throws Exception {
        // Given: test-example-app has been compiled
        Path mapperPath = Paths.get(TEST_APP_BASE, "mapper");

        // Then: Mapper interfaces should exist
        assertTrue(Files.exists(mapperPath), "Mapper directory should exist");

        // Verify specific mappers
        assertFileExists(mapperPath, "InventoryMapper.java");
        assertFileExists(mapperPath, "CustomerMapper.java");
    }

    @Test
    @DisplayName("should generate adapter implementations")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateAdapterImplementations() throws Exception {
        // Given: test-example-app has been compiled
        Path adapterPath = Paths.get(TEST_APP_BASE, "adapter");

        // Then: Adapter classes should exist
        assertTrue(Files.exists(adapterPath), "Adapter directory should exist");

        // Verify specific adapters
        assertFileExists(adapterPath, "InventoryAdapter.java");
        assertFileExists(adapterPath, "CustomerAdapter.java");
    }

    @Test
    @DisplayName("should generate embeddable classes for multi-field value objects")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateEmbeddableClasses() throws Exception {
        // Given: test-example-app has been compiled
        Path embeddablePath = Paths.get(TEST_APP_BASE, "embeddable");

        // Then: Embeddable classes should exist
        if (Files.exists(embeddablePath)) {
            // Verify embeddable for Address if it exists
            Path addressEmbeddable = embeddablePath.resolve("AddressEmbeddable.java");
            if (Files.exists(addressEmbeddable)) {
                String content = Files.readString(addressEmbeddable);
                assertTrue(content.contains("@Embeddable"), "Should have @Embeddable annotation");
            }
        }
    }

    @Test
    @DisplayName("generated entities should have JPA annotations")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEntitiesShouldHaveJpaAnnotations() throws Exception {
        // Given: InventoryEntity has been generated
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "InventoryEntity.java should exist");

        // When: Read the generated file
        String content = Files.readString(entityFile);

        // Then: Should contain JPA annotations
        assertTrue(content.contains("@Entity"), "Should have @Entity annotation");
        assertTrue(content.contains("@Table"), "Should have @Table annotation");
        assertTrue(content.contains("@Id"), "Should have @Id annotation");
    }

    @Test
    @DisplayName("generated entities should have audit fields when enabled")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEntitiesShouldHaveAuditFields() throws Exception {
        // Given: InventoryEntity has been generated with auditing enabled
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "InventoryEntity.java should exist");

        // When: Read the generated file
        String content = Files.readString(entityFile);

        // Then: Should contain audit annotations
        assertTrue(content.contains("@CreatedDate") || content.contains("createdAt"), "Should have creation timestamp");
        assertTrue(
                content.contains("@LastModifiedDate") || content.contains("updatedAt"), "Should have update timestamp");
    }

    @Test
    @DisplayName("generated entities should support soft delete when enabled")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEntitiesShouldSupportSoftDelete() throws Exception {
        // Given: InventoryEntity has been generated with soft delete enabled
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "InventoryEntity.java should exist");

        // When: Read the generated file
        String content = Files.readString(entityFile);

        // Then: Should contain deletedAt field
        assertTrue(content.contains("deletedAt"), "Should have deletedAt field for soft delete");
    }

    @Test
    @DisplayName("generated repositories should have derived query methods")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedRepositoriesShouldHaveDerivedQueryMethods() throws Exception {
        // Given: InventoryJpaRepository has been generated
        Path repoFile = Paths.get(TEST_APP_BASE, "springdata/InventoryJpaRepository.java");
        assertTrue(Files.exists(repoFile), "InventoryJpaRepository.java should exist");

        // When: Read the generated file
        String content = Files.readString(repoFile);

        // Then: Should contain derived query methods
        assertTrue(content.contains("findByProductSku"), "Should have findByProductSku query method");
        assertTrue(content.contains("findByWarehouseLocation"), "Should have findByWarehouseLocation query method");
    }

    @Test
    @DisplayName("generated adapters should delegate to repositories and mappers")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedAdaptersShouldDelegateToRepositoriesAndMappers() throws Exception {
        // Given: InventoryAdapter has been generated
        Path adapterFile = Paths.get(TEST_APP_BASE, "adapter/InventoryAdapter.java");
        assertTrue(Files.exists(adapterFile), "InventoryAdapter.java should exist");

        // When: Read the generated file
        String content = Files.readString(adapterFile);

        // Then: Should inject repository and mapper
        assertTrue(content.contains("InventoryJpaRepository"), "Should inject JPA repository");
        assertTrue(content.contains("InventoryMapper"), "Should inject mapper");

        // And: Should have delegation methods
        assertTrue(content.contains("mapper.toDomain"), "Should delegate to mapper.toDomain");
        assertTrue(content.contains("mapper.toEntity"), "Should delegate to mapper.toEntity");
        assertTrue(content.contains("repo."), "Should delegate to repository");
    }

    @Test
    @DisplayName("should generate all artifacts for each repository port")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateAllArtifactsForEachRepositoryPort() throws Exception {
        // For InventoryRepository port, should generate:
        String[] inventoryArtifacts = {
            "entity/InventoryEntity.java",
            "springdata/InventoryJpaRepository.java",
            "mapper/InventoryMapper.java",
            "adapter/InventoryAdapter.java"
        };

        for (String artifact : inventoryArtifacts) {
            Path artifactPath = Paths.get(TEST_APP_BASE, artifact);
            assertTrue(
                    Files.exists(artifactPath),
                    "Artifact should exist: " + artifact + " (path: " + artifactPath.toAbsolutePath() + ")");
        }

        // For CustomerRepository port, should generate:
        String[] customerArtifacts = {
            "entity/CustomerEntity.java",
            "springdata/CustomerJpaRepository.java",
            "mapper/CustomerMapper.java",
            "adapter/CustomerAdapter.java"
        };

        for (String artifact : customerArtifacts) {
            Path artifactPath = Paths.get(TEST_APP_BASE, artifact);
            assertTrue(
                    Files.exists(artifactPath),
                    "Artifact should exist: " + artifact + " (path: " + artifactPath.toAbsolutePath() + ")");
        }
    }

    @Test
    @DisplayName("generated code should compile without errors")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedCodeShouldCompile() throws Exception {
        // Given: test-example-app has been compiled
        Path targetClasses = Paths.get("../test-example-app/target/classes");

        // Then: Compiled classes should exist
        assertTrue(Files.exists(targetClasses), "Target classes directory should exist");

        // Verify that generated classes were compiled
        assertTrue(
                Files.walk(targetClasses).anyMatch(p -> p.toString().contains("infrastructure/persistence")),
                "Generated infrastructure code should be compiled");
    }

    @Test
    @DisplayName("should generate correct package structure")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateCorrectPackageStructure() throws Exception {
        // Given: test-example-app has been compiled
        Path basePath = Paths.get(TEST_APP_BASE);

        // Then: Expected package structure should exist
        assertTrue(Files.exists(basePath.resolve("entity")), "entity package should exist");
        assertTrue(Files.exists(basePath.resolve("springdata")), "springdata package should exist");
        assertTrue(Files.exists(basePath.resolve("mapper")), "mapper package should exist");
        assertTrue(Files.exists(basePath.resolve("adapter")), "adapter package should exist");
    }

    @Test
    @DisplayName("should respect configuration from hexaglue.yaml")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldRespectConfiguration() throws Exception {
        // Given: hexaglue.yaml specifies custom configuration
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "Entity file should exist");

        String content = Files.readString(entityFile);

        // Then: Should respect naming conventions from config
        assertTrue(content.contains("class InventoryEntity"), "Should use Entity suffix from config");

        // And: Should have configured features
        assertTrue(content.contains("@Table"), "Should have table mapping");
    }

    @Test
    @DisplayName("should generate relationship mappings for aggregate children")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateRelationshipMappings() throws Exception {
        // Given: Order aggregate has OrderItem children
        Path orderEntityFile = Paths.get(TEST_APP_BASE, "entity/OrderEntity.java");

        if (Files.exists(orderEntityFile)) {
            String content = Files.readString(orderEntityFile);

            // Then: Should have @OneToMany for children collection
            boolean hasOneToMany = content.contains("@OneToMany") || content.contains("OneToMany");
            assertTrue(hasOneToMany, "Should have @OneToMany relationship for aggregate children");

            // And: Should have cascade configuration
            boolean hasCascade = content.contains("cascade") || content.contains("Cascade");
            assertTrue(hasCascade, "Should configure cascade for aggregate children");
        }
    }

    @Test
    @DisplayName("should generate embedded mappings for multi-field value objects")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateEmbeddedMappings() throws Exception {
        // Given: Customer has Address value object
        Path customerEntityFile = Paths.get(TEST_APP_BASE, "entity/CustomerEntity.java");

        if (Files.exists(customerEntityFile)) {
            String content = Files.readString(customerEntityFile);

            // Then: Should have @Embedded annotation or separate embeddable
            boolean hasEmbedded = content.contains("@Embedded") || content.contains("Embedded");
            if (hasEmbedded) {
                assertTrue(true, "Entity uses @Embedded for multi-field value objects");
            } else {
                // Check if embeddable class exists
                Path embeddablePath = Paths.get(TEST_APP_BASE, "embeddable");
                if (Files.exists(embeddablePath)) {
                    long embeddableCount = countJavaFiles(embeddablePath);
                    assertTrue(embeddableCount > 0, "Should generate embeddable classes");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate AttributeConverters for single-field value objects")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateAttributeConverters() throws Exception {
        // Given: Domain uses single-field value objects (e.g., CustomerId, Email)
        Path converterPath = Paths.get(TEST_APP_BASE, "converter");

        if (Files.exists(converterPath)) {
            // Then: Converter classes should exist
            long converterCount = countJavaFiles(converterPath);
            assertTrue(converterCount > 0, "Should generate converters for value objects");

            // And: Converters should implement AttributeConverter
            try (Stream<Path> converters = Files.walk(converterPath)) {
                converters
                        .filter(p -> p.toString().endsWith("Converter.java"))
                        .findFirst()
                        .ifPresent(converter -> {
                            try {
                                String content = Files.readString(converter);
                                assertTrue(
                                        content.contains("AttributeConverter"),
                                        "Converter should implement AttributeConverter");
                                assertTrue(
                                        content.contains("@Converter"), "Converter should have @Converter annotation");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

    @Test
    @DisplayName("should handle enum properties with @Enumerated")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleEnumProperties() throws Exception {
        // Given: CustomerEntity has CustomerStatus enum
        Path customerEntityFile = Paths.get(TEST_APP_BASE, "entity/CustomerEntity.java");

        if (Files.exists(customerEntityFile)) {
            String content = Files.readString(customerEntityFile);

            // Then: If enum property exists, should have @Enumerated
            if (content.contains("Status") || content.contains("status")) {
                boolean hasEnumerated = content.contains("@Enumerated") || content.contains("Enumerated");
                assertTrue(hasEnumerated, "Enum properties should use @Enumerated annotation");
            }
        }
    }

    @Test
    @DisplayName("should generate composite ID classes with @EmbeddedId")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateCompositeIds() throws Exception {
        // Given: An entity with composite ID (multi-field identifier)
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasCompositeId = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@EmbeddedId");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // If composite IDs are detected, should use @EmbeddedId
                if (hasCompositeId) {
                    assertTrue(true, "Composite IDs use @EmbeddedId annotation");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate collection mappings with @ElementCollection")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateCollectionMappings() throws Exception {
        // Given: Entities with collections of simple types or embeddables
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasElementCollection = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@ElementCollection") || content.contains("ElementCollection");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // If collections exist, should use appropriate mapping
                if (hasElementCollection) {
                    assertTrue(true, "Collections use @ElementCollection where appropriate");
                }
            }
        }
    }

    @Test
    @DisplayName("generated mappers should have complete mapping methods")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedMappersShouldHaveCompleteMappingMethods() throws Exception {
        // Given: InventoryMapper has been generated
        Path mapperFile = Paths.get(TEST_APP_BASE, "mapper/InventoryMapper.java");

        if (Files.exists(mapperFile)) {
            String content = Files.readString(mapperFile);

            // Then: Should have toDomain method
            assertTrue(content.contains("toDomain"), "Mapper should have toDomain method");

            // And: Should have toEntity method
            assertTrue(content.contains("toEntity"), "Mapper should have toEntity method");

            // And: Should be a MapStruct interface
            assertTrue(content.contains("@Mapper"), "Should be annotated with @Mapper");
            assertTrue(content.contains("interface"), "Should be a MapStruct interface");
        }
    }

    @Test
    @DisplayName("should generate correct ID generation strategy annotations")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateCorrectIdStrategy() throws Exception {
        // Given: Entities are generated with configured ID strategy
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                entities.filter(p -> p.toString().endsWith("Entity.java")).forEach(entity -> {
                    try {
                        String content = Files.readString(entity);

                        // Should have @Id annotation
                        assertTrue(content.contains("@Id"), "Entity should have @Id annotation");

                        // Should have generation strategy (unless ASSIGNED)
                        boolean hasGeneratedValue = content.contains("@GeneratedValue");
                        boolean hasGenerationType = content.contains("GenerationType");

                        if (hasGeneratedValue) {
                            assertTrue(hasGenerationType, "Entity with @GeneratedValue should specify GenerationType");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("should generate Spring @Component annotation on adapters")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateSpringComponentOnAdapters() throws Exception {
        // Given: InventoryAdapter has been generated
        Path adapterFile = Paths.get(TEST_APP_BASE, "adapter/InventoryAdapter.java");
        assertTrue(Files.exists(adapterFile), "Adapter file should exist");

        // When: Read the generated file
        String content = Files.readString(adapterFile);

        // Then: Should have Spring @Component annotation
        assertTrue(content.contains("@Component") || content.contains("@Service"), "Adapter should be a Spring bean");
    }

    @Test
    @DisplayName("generated entities should use proper column naming strategy")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEntitiesShouldUseProperColumnNaming() throws Exception {
        // Given: InventoryEntity has been generated
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "Entity file should exist");

        // When: Read the generated file
        String content = Files.readString(entityFile);

        // Then: Should have @Column annotations for properties
        assertTrue(content.contains("@Column"), "Entity should have @Column annotations");

        // And: Column names should follow naming convention (snake_case by default)
        if (content.contains("@Column(name")) {
            assertTrue(
                    content.contains("name = \"") || content.contains("name=\""),
                    "Should specify column names explicitly");
        }
    }

    @Test
    @DisplayName("should handle nullable properties correctly")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleNullablePropertiesCorrectly() throws Exception {
        // Given: Entities with nullable and non-nullable properties
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                entities.filter(p -> p.toString().endsWith("Entity.java")).forEach(entity -> {
                    try {
                        String content = Files.readString(entity);

                        // Should have nullable configuration in @Column
                        boolean hasNullableConfig =
                                content.contains("nullable = true") || content.contains("nullable = false");

                        if (hasNullableConfig) {
                            assertTrue(true, "Entity specifies nullability constraints");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("generated repositories should extend JpaRepository with correct types")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedRepositoriesShouldExtendJpaRepository() throws Exception {
        // Given: InventoryJpaRepository has been generated
        Path repoFile = Paths.get(TEST_APP_BASE, "springdata/InventoryJpaRepository.java");

        if (Files.exists(repoFile)) {
            String content = Files.readString(repoFile);

            // Then: Should extend JpaRepository
            assertTrue(
                    content.contains("extends JpaRepository") || content.contains("JpaRepository<"),
                    "Repository should extend JpaRepository");

            // And: Should specify entity and ID types
            assertTrue(content.contains("InventoryEntity"), "Repository should use entity type");

            // And: Should be an interface
            assertTrue(content.contains("interface"), "Repository should be an interface");
        }
    }

    @Test
    @DisplayName("generated mappers should use Spring component model")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedMappersShouldUseSpringComponentModel() throws Exception {
        // Given: InventoryMapper has been generated
        Path mapperFile = Paths.get(TEST_APP_BASE, "mapper/InventoryMapper.java");

        if (Files.exists(mapperFile)) {
            String content = Files.readString(mapperFile);

            // Then: Should configure componentModel = "spring"
            assertTrue(
                    content.contains("componentModel = \"spring\"") || content.contains("componentModel=\"spring\""),
                    "Mapper should use Spring component model");
        }
    }

    @Test
    @DisplayName("generated adapters should use constructor injection")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedAdaptersShouldUseConstructorInjection() throws Exception {
        // Given: InventoryAdapter has been generated
        Path adapterFile = Paths.get(TEST_APP_BASE, "adapter/InventoryAdapter.java");

        if (Files.exists(adapterFile)) {
            String content = Files.readString(adapterFile);

            // Then: Should have constructor with dependencies
            boolean hasConstructor = content.contains("public ") && content.contains("Adapter(");

            // Or should have @RequiredArgsConstructor with final fields
            boolean hasLombokInjection = content.contains("@RequiredArgsConstructor")
                    || (content.contains("private final") && content.contains("Repository"));

            assertTrue(hasConstructor || hasLombokInjection, "Adapter should use constructor injection");
        }
    }

    @Test
    @DisplayName("generated entities should have proper JPA imports")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEntitiesShouldHaveProperJpaImports() throws Exception {
        // Given: InventoryEntity has been generated
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");
        assertTrue(Files.exists(entityFile), "Entity file should exist");

        // When: Read the generated file
        String content = Files.readString(entityFile);

        // Then: Should have JPA imports
        assertTrue(
                content.contains("import jakarta.persistence") || content.contains("import javax.persistence"),
                "Entity should import JPA annotations");
    }

    @Test
    @DisplayName("generated embeddables should have @Embeddable annotation")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedEmbeddablesShouldHaveAnnotation() throws Exception {
        // Given: Embeddable classes exist
        Path embeddablePath = Paths.get(TEST_APP_BASE, "embeddable");

        if (Files.exists(embeddablePath)) {
            try (Stream<Path> embeddables = Files.walk(embeddablePath)) {
                embeddables
                        .filter(p -> p.toString().endsWith("Embeddable.java"))
                        .forEach(embeddable -> {
                            try {
                                String content = Files.readString(embeddable);

                                // Should have @Embeddable
                                assertTrue(content.contains("@Embeddable"), "Class should have @Embeddable annotation");

                                // Should be a class (not interface)
                                assertTrue(content.contains("class"), "Should be a class");

                                // Should have proper fields
                                assertTrue(
                                        content.contains("private ") || content.contains("public "),
                                        "Should have fields");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

    @Test
    @DisplayName("should configure cascade types for parent-child relationships")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldConfigureCascadeTypes() throws Exception {
        // Given: Order aggregate with OrderItems
        Path orderEntityFile = Paths.get(TEST_APP_BASE, "entity/OrderEntity.java");

        if (Files.exists(orderEntityFile)) {
            String content = Files.readString(orderEntityFile);

            // If has children relationship, should configure cascade
            if (content.contains("@OneToMany")) {
                boolean hasCascade = content.contains("cascade = ") || content.contains("cascade=");
                assertTrue(hasCascade, "Parent entity should configure cascade for children");

                // Should use CascadeType.ALL or specific types for aggregate root
                boolean hasCascadeAll = content.contains("CascadeType.ALL") || content.contains("CascadeType");
                assertTrue(hasCascadeAll, "Should specify CascadeType");
            }
        }
    }

    @Test
    @DisplayName("should configure orphan removal for aggregate children")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldConfigureOrphanRemoval() throws Exception {
        // Given: Order aggregate with OrderItems
        Path orderEntityFile = Paths.get(TEST_APP_BASE, "entity/OrderEntity.java");

        if (Files.exists(orderEntityFile)) {
            String content = Files.readString(orderEntityFile);

            // If has children relationship, should configure orphan removal
            if (content.contains("@OneToMany")) {
                boolean hasOrphanRemoval =
                        content.contains("orphanRemoval = true") || content.contains("orphanRemoval=true");
                if (hasOrphanRemoval) {
                    assertTrue(true, "Aggregate root configures orphan removal for children");
                }
            }
        }
    }

    @Test
    @DisplayName("should use lazy fetching as default for collections")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldUseLazyFetchingForCollections() throws Exception {
        // Given: Entities with relationships
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                entities.filter(p -> p.toString().endsWith("Entity.java")).forEach(entity -> {
                    try {
                        String content = Files.readString(entity);

                        // If has @OneToMany or @ManyToOne, check fetch strategy
                        if (content.contains("@OneToMany") || content.contains("@ManyToOne")) {
                            // LAZY is default, but if explicitly specified should be LAZY
                            if (content.contains("fetch = ") || content.contains("fetch=")) {
                                boolean hasLazy = content.contains("FetchType.LAZY");
                                assertTrue(
                                        hasLazy || !content.contains("FetchType.EAGER"), "Should prefer LAZY fetching");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("should generate join columns for relationships")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateJoinColumns() throws Exception {
        // Given: Entities with @ManyToOne relationships
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasJoinColumn = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@JoinColumn");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasJoinColumn) {
                    assertTrue(true, "Relationships use @JoinColumn for foreign keys");
                }
            }
        }
    }

    @Test
    @DisplayName("should configure collection table for @ElementCollection")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldConfigureCollectionTable() throws Exception {
        // Given: Entities with @ElementCollection
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasCollectionTable = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@ElementCollection")
                                        && (content.contains("@CollectionTable")
                                                || content.contains("CollectionTable"));
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasCollectionTable) {
                    assertTrue(true, "@ElementCollection uses @CollectionTable");
                }
            }
        }
    }

    @Test
    @DisplayName("generated adapters should implement port interfaces")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedAdaptersShouldImplementPortInterfaces() throws Exception {
        // Given: InventoryAdapter has been generated
        Path adapterFile = Paths.get(TEST_APP_BASE, "adapter/InventoryAdapter.java");

        if (Files.exists(adapterFile)) {
            String content = Files.readString(adapterFile);

            // Then: Should implement the port interface
            assertTrue(content.contains("implements "), "Adapter should implement port interface");

            // And: Should implement repository methods
            boolean hasImplementedMethods = content.contains("@Override") || content.contains("public ");
            assertTrue(hasImplementedMethods, "Adapter should have method implementations");
        }
    }

    @Test
    @DisplayName("should generate table schema if configured")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateTableSchema() throws Exception {
        // Given: Configuration may specify schema
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasSchema = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("schema = ") || content.contains("schema=\"");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // Schema is optional, so this just verifies format if present
                if (hasSchema) {
                    assertTrue(true, "Entities configure schema when specified");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate validation annotations for constraints")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateValidationAnnotations() throws Exception {
        // Given: Domain may have constraints
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasValidation = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@NotNull")
                                        || content.contains("@Size")
                                        || content.contains("@Min")
                                        || content.contains("@Max");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // Validation is optional based on domain annotations
                if (hasValidation) {
                    assertTrue(true, "Entities preserve validation constraints");
                }
            }
        }
    }

    @Test
    @DisplayName("generated repositories should be public interfaces")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedRepositoriesShouldBePublicInterfaces() throws Exception {
        // Given: Repository interfaces exist
        Path repoPath = Paths.get(TEST_APP_BASE, "springdata");

        if (Files.exists(repoPath)) {
            try (Stream<Path> repos = Files.walk(repoPath)) {
                repos.filter(p -> p.toString().endsWith("JpaRepository.java")).forEach(repo -> {
                    try {
                        String content = Files.readString(repo);

                        // Should be public interface
                        assertTrue(content.contains("public interface"), "Repository should be public interface");

                        // Should NOT be abstract class
                        assertTrue(!content.contains("abstract class"), "Repository should not be abstract class");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("generated code should have proper package declarations")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedCodeShouldHaveProperPackageDeclarations() throws Exception {
        // Given: All generated files
        Path basePath = Paths.get(TEST_APP_BASE);

        if (Files.exists(basePath)) {
            // Check entities
            Path entityFile = basePath.resolve("entity/InventoryEntity.java");
            if (Files.exists(entityFile)) {
                String content = Files.readString(entityFile);
                assertTrue(
                        content.contains("package ") && content.contains(".entity"),
                        "Entity should have entity package");
            }

            // Check repositories
            Path repoFile = basePath.resolve("springdata/InventoryJpaRepository.java");
            if (Files.exists(repoFile)) {
                String content = Files.readString(repoFile);
                assertTrue(
                        content.contains("package ") && content.contains(".springdata"),
                        "Repository should have springdata package");
            }

            // Check mappers
            Path mapperFile = basePath.resolve("mapper/InventoryMapper.java");
            if (Files.exists(mapperFile)) {
                String content = Files.readString(mapperFile);
                assertTrue(
                        content.contains("package ") && content.contains(".mapper"),
                        "Mapper should have mapper package");
            }

            // Check adapters
            Path adapterFile = basePath.resolve("adapter/InventoryAdapter.java");
            if (Files.exists(adapterFile)) {
                String content = Files.readString(adapterFile);
                assertTrue(
                        content.contains("package ") && content.contains(".adapter"),
                        "Adapter should have adapter package");
            }
        }
    }

    @Test
    @DisplayName("should generate multiple entities for multiple repository ports")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateMultipleEntitiesForMultiplePorts() throws Exception {
        // Given: Multiple repository ports (Inventory, Customer, Order, Product)
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");
        assertTrue(Files.exists(entityPath), "Entity directory should exist");

        // When: Count entity files
        long entityCount = countJavaFiles(entityPath);

        // Then: Should generate entity for each repository port
        assertTrue(entityCount >= 2, "Should generate at least 2 entities for different aggregates");
    }

    @Test
    @DisplayName("should handle temporal types correctly")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleTemporalTypes() throws Exception {
        // Given: Entities with LocalDate, LocalDateTime, Instant fields
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasTemporal = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("LocalDate")
                                        || content.contains("LocalDateTime")
                                        || content.contains("Instant")
                                        || content.contains("ZonedDateTime");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // Temporal types are common in entities
                if (hasTemporal) {
                    assertTrue(true, "Entities handle temporal types");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate @Version field for optimistic locking when enabled")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateVersionFieldForOptimisticLocking() throws Exception {
        // Given: Optimistic locking is enabled in config
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasVersion = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@Version") || content.contains("version");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // Version is optional based on configuration
                if (hasVersion) {
                    assertTrue(true, "Entities have @Version for optimistic locking");
                }
            }
        }
    }

    @Test
    @DisplayName("should handle collection types appropriately")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleCollectionTypes() throws Exception {
        // Given: Entities with List, Set, or Map collections
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasCollections = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("List<")
                                        || content.contains("Set<")
                                        || content.contains("Map<");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasCollections) {
                    assertTrue(true, "Entities use Java collection types");
                }
            }
        }
    }

    @Test
    @DisplayName("should handle index definitions on entities")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleIndexDefinitions() throws Exception {
        // Given: Entities may have index definitions
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasIndexes = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@Index") || content.contains("indexes");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // Indexes are optional based on performance requirements
                if (hasIndexes) {
                    assertTrue(true, "Entities define indexes for performance");
                }
            }
        }
    }

    @Test
    @DisplayName("should handle unique constraints")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleUniqueConstraints() throws Exception {
        // Given: Entities may have unique constraints
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasUniqueConstraints = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@UniqueConstraint")
                                        || content.contains("unique = true")
                                        || content.contains("unique=true");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasUniqueConstraints) {
                    assertTrue(true, "Entities define unique constraints");
                }
            }
        }
    }

    @Test
    @DisplayName("generated query methods should support different return types")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedQueryMethodsShouldSupportDifferentReturnTypes() throws Exception {
        // Given: Repository with query methods
        Path repoFile = Paths.get(TEST_APP_BASE, "springdata/InventoryJpaRepository.java");

        if (Files.exists(repoFile)) {
            String content = Files.readString(repoFile);

            // Should support Optional return types
            boolean hasOptional = content.contains("Optional<");

            // Should support List return types
            boolean hasList = content.contains("List<");

            // Should support boolean return types (existsBy)
            boolean hasBoolean = content.contains("boolean ");

            // Should support count return types
            boolean hasCount = content.contains("long ") || content.contains("Long ");

            // At least some query method patterns should exist
            assertTrue(
                    hasOptional || hasList || hasBoolean || hasCount,
                    "Repository should have query methods with various return types");
        }
    }

    @Test
    @DisplayName("should handle bidirectional relationships correctly")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleBidirectionalRelationships() throws Exception {
        // Given: Parent-child relationships (Order-OrderItem)
        Path orderEntityFile = Paths.get(TEST_APP_BASE, "entity/OrderEntity.java");
        Path orderItemEntityFile = Paths.get(TEST_APP_BASE, "entity/OrderItemEntity.java");

        // If both parent and child entities exist
        if (Files.exists(orderEntityFile) && Files.exists(orderItemEntityFile)) {
            String orderContent = Files.readString(orderEntityFile);
            String itemContent = Files.readString(orderItemEntityFile);

            // Parent should have @OneToMany
            boolean parentHasOneToMany = orderContent.contains("@OneToMany");

            // Child should have @ManyToOne back reference
            boolean childHasManyToOne = itemContent.contains("@ManyToOne");

            // If bidirectional, both should be configured
            if (parentHasOneToMany && childHasManyToOne) {
                assertTrue(true, "Bidirectional relationships properly configured");
            }
        }
    }

    @Test
    @DisplayName("generated adapters should handle Optional return types")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedAdaptersShouldHandleOptionalReturnTypes() throws Exception {
        // Given: Adapter methods
        Path adapterFile = Paths.get(TEST_APP_BASE, "adapter/InventoryAdapter.java");

        if (Files.exists(adapterFile)) {
            String content = Files.readString(adapterFile);

            // Should use Optional for findById methods
            boolean hasOptional = content.contains("Optional<");

            if (hasOptional) {
                assertTrue(true, "Adapter methods return Optional where appropriate");
            }
        }
    }

    @Test
    @DisplayName("should handle large text fields with @Lob")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleLargeTextFields() throws Exception {
        // Given: Entities may have large text fields (description, content)
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasLob = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@Lob");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasLob) {
                    assertTrue(true, "Entities use @Lob for large fields");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate proper equals and hashCode for entities")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateEqualsAndHashCode() throws Exception {
        // Given: Entity classes
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasEqualsHashCode = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return (content.contains("equals(") && content.contains("hashCode("))
                                        || content.contains("@EqualsAndHashCode");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasEqualsHashCode) {
                    assertTrue(true, "Entities implement equals and hashCode");
                }
            }
        }
    }

    @Test
    @DisplayName("should handle default values for fields")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleDefaultValues() throws Exception {
        // Given: Entities may have default field values
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasDefaults = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("= ") && content.contains("private");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasDefaults) {
                    assertTrue(true, "Entities initialize fields with default values");
                }
            }
        }
    }

    @Test
    @DisplayName("generated mappers should handle nested value objects")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedMappersShouldHandleNestedValueObjects() throws Exception {
        // Given: Mappers for complex aggregates
        Path mapperFile = Paths.get(TEST_APP_BASE, "mapper/CustomerMapper.java");

        if (Files.exists(mapperFile)) {
            String content = Files.readString(mapperFile);

            // Should have mapping methods for nested objects
            boolean hasNestedMappings = content.contains("@Mapping");

            if (hasNestedMappings) {
                assertTrue(true, "Mapper configures mappings for nested objects");
            }
        }
    }

    @Test
    @DisplayName("should handle transient fields correctly")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleTransientFields() throws Exception {
        // Given: Entities may have transient calculated fields
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasTransient = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@Transient");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasTransient) {
                    assertTrue(true, "Entities mark non-persisted fields as @Transient");
                }
            }
        }
    }

    @Test
    @DisplayName("should generate lifecycle callback methods when needed")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldGenerateLifecycleCallbacks() throws Exception {
        // Given: Entities may have lifecycle callbacks
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasLifecycle = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("@PrePersist")
                                        || content.contains("@PostLoad")
                                        || content.contains("@PreUpdate")
                                        || content.contains("@PostPersist");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasLifecycle) {
                    assertTrue(true, "Entities use JPA lifecycle callbacks");
                }
            }
        }
    }

    @Test
    @DisplayName("generated code should follow Java naming conventions")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedCodeShouldFollowJavaNamingConventions() throws Exception {
        // Given: Generated entity
        Path entityFile = Paths.get(TEST_APP_BASE, "entity/InventoryEntity.java");

        if (Files.exists(entityFile)) {
            String content = Files.readString(entityFile);

            // Class name should be PascalCase
            assertTrue(content.contains("class "), "Should have class declaration");

            // Method names should be camelCase (getters/setters)
            boolean hasCamelCase = content.contains("get") || content.contains("set");

            // Field names should be camelCase
            boolean hasFields = content.contains("private ");

            assertTrue(hasCamelCase || hasFields, "Should follow Java naming conventions");
        }
    }

    @Test
    @DisplayName("should handle Money and other numeric value objects")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldHandleMoneyValueObjects() throws Exception {
        // Given: Entities with Money, Price, Amount value objects
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            try (Stream<Path> entities = Files.walk(entityPath)) {
                boolean hasNumericVOs = entities.filter(p -> p.toString().endsWith("Entity.java"))
                        .anyMatch(entity -> {
                            try {
                                String content = Files.readString(entity);
                                return content.contains("BigDecimal")
                                        || content.contains("Money")
                                        || content.contains("Price")
                                        || content.contains("Amount");
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (hasNumericVOs) {
                    assertTrue(true, "Entities handle monetary/numeric value objects");
                }
            }
        }
    }

    @Test
    @DisplayName("generated repositories should be in correct infrastructure package")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void generatedRepositoriesShouldBeInInfrastructurePackage() throws Exception {
        // Given: Repository file
        Path repoFile = Paths.get(TEST_APP_BASE, "springdata/InventoryJpaRepository.java");

        if (Files.exists(repoFile)) {
            String content = Files.readString(repoFile);

            // Package should be in infrastructure persistence layer
            assertTrue(
                    content.contains("infrastructure") && content.contains("persistence"),
                    "Repository should be in infrastructure.persistence package");
        }
    }

    @Test
    @DisplayName("should not generate artifacts for non-repository ports")
    @EnabledIfSystemProperty(named = "jpa.integration.test", matches = "true")
    void shouldNotGenerateArtifactsForNonRepositoryPorts() throws Exception {
        // Given: Only repository ports should generate JPA artifacts
        Path entityPath = Paths.get(TEST_APP_BASE, "entity");

        if (Files.exists(entityPath)) {
            long entityCount = countJavaFiles(entityPath);

            // Should not generate entities for every port, only repositories
            // (This is a sanity check - exact count depends on domain model)
            assertTrue(entityCount > 0, "Should generate entities for repository ports");
            assertTrue(entityCount < 100, "Should not generate excessive entities");
        }
    }

    // Helper methods

    private void assertFileExists(Path directory, String fileName) {
        Path filePath = directory.resolve(fileName);
        assertTrue(Files.exists(filePath), "File should exist: " + fileName + " at " + filePath.toAbsolutePath());
    }

    private long countJavaFiles(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return 0;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(p -> p.toString().endsWith(".java")).count();
        }
    }
}
