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
package io.hexaglue.plugin.jpa.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.diagnostics.JpaDiagnosticCodes;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.types.TypeRef;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IdStrategyValidator}.
 *
 * <p>Tests validation of ID generation strategy compatibility with ID types.
 *
 * @since 0.4.0
 */
class IdStrategyValidatorTest {

    private static final String PLUGIN_ID = "io.hexaglue.plugin.jpa.test";
    private static final String CONTEXT_NAME = "TestRepository";

    private IdStrategyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IdStrategyValidator(PLUGIN_ID);
    }

    @Nested
    @DisplayName("IDENTITY strategy validation")
    class IdentityStrategyTests {

        @Test
        @DisplayName("should accept Long type with IDENTITY strategy")
        void shouldAcceptLongWithIdentity() {
            // Given
            TypeRef idType = mockTypeRef("java.lang.Long");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Long should be compatible with IDENTITY");
        }

        @Test
        @DisplayName("should accept Integer type with IDENTITY strategy")
        void shouldAcceptIntegerWithIdentity() {
            // Given
            TypeRef idType = mockTypeRef("java.lang.Integer");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Integer should be compatible with IDENTITY");
        }

        @Test
        @DisplayName("should accept primitive long with IDENTITY strategy")
        void shouldAcceptPrimitiveLongWithIdentity() {
            // Given
            TypeRef idType = mockTypeRef("long");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "primitive long should be compatible with IDENTITY");
        }

        @Test
        @DisplayName("should reject String type with IDENTITY strategy")
        void shouldRejectStringWithIdentity() {
            // Given
            TypeRef idType = mockTypeRef("java.lang.String");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isPresent(), "String should be incompatible with IDENTITY");
            Diagnostic diagnostic = result.get();
            assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
            assertEquals(JpaDiagnosticCodes.INCOMPATIBLE_ID_STRATEGY, diagnostic.code());
            assertTrue(diagnostic.message().contains("IDENTITY"));
            assertTrue(diagnostic.message().contains("String"));
        }

        @Test
        @DisplayName("should reject UUID type with IDENTITY strategy")
        void shouldRejectUuidWithIdentity() {
            // Given
            TypeRef idType = mockTypeRef("java.util.UUID");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isPresent(), "UUID should be incompatible with IDENTITY");
        }
    }

    @Nested
    @DisplayName("SEQUENCE strategy validation")
    class SequenceStrategyTests {

        @Test
        @DisplayName("should accept Long type with SEQUENCE strategy")
        void shouldAcceptLongWithSequence() {
            // Given
            TypeRef idType = mockTypeRef("Long");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.SEQUENCE;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Long should be compatible with SEQUENCE");
        }

        @Test
        @DisplayName("should reject String type with SEQUENCE strategy")
        void shouldRejectStringWithSequence() {
            // Given
            TypeRef idType = mockTypeRef("String");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.SEQUENCE;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isPresent(), "String should be incompatible with SEQUENCE");
            Diagnostic diagnostic = result.get();
            assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        }
    }

    @Nested
    @DisplayName("AUTO strategy validation")
    class AutoStrategyTests {

        @Test
        @DisplayName("should accept Integer type with AUTO strategy")
        void shouldAcceptIntegerWithAuto() {
            // Given
            TypeRef idType = mockTypeRef("Integer");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.AUTO;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Integer should be compatible with AUTO");
        }

        @Test
        @DisplayName("should reject String type with AUTO strategy")
        void shouldRejectStringWithAuto() {
            // Given
            TypeRef idType = mockTypeRef("String");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.AUTO;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isPresent(), "String should be incompatible with AUTO");
        }
    }

    @Nested
    @DisplayName("UUID strategy validation")
    class UuidStrategyTests {

        @Test
        @DisplayName("should accept String type with UUID strategy")
        void shouldAcceptStringWithUuid() {
            // Given
            TypeRef idType = mockTypeRef("java.lang.String");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.UUID;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "String should be compatible with UUID");
        }

        @Test
        @DisplayName("should accept UUID type with UUID strategy")
        void shouldAcceptUuidWithUuid() {
            // Given
            TypeRef idType = mockTypeRef("java.util.UUID");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.UUID;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "UUID should be compatible with UUID strategy");
        }

        @Test
        @DisplayName("should warn for Long type with UUID strategy")
        void shouldWarnForLongWithUuid() {
            // Given
            TypeRef idType = mockTypeRef("Long");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.UUID;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isPresent(), "Long with UUID strategy should produce warning");
            Diagnostic diagnostic = result.get();
            assertEquals(DiagnosticSeverity.WARNING, diagnostic.severity());
        }
    }

    @Nested
    @DisplayName("ASSIGNED strategy validation")
    class AssignedStrategyTests {

        @Test
        @DisplayName("should accept String type with ASSIGNED strategy")
        void shouldAcceptStringWithAssigned() {
            // Given
            TypeRef idType = mockTypeRef("String");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.ASSIGNED;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "ASSIGNED should accept any type");
        }

        @Test
        @DisplayName("should accept Long type with ASSIGNED strategy")
        void shouldAcceptLongWithAssigned() {
            // Given
            TypeRef idType = mockTypeRef("Long");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.ASSIGNED;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "ASSIGNED should accept any type");
        }

        @Test
        @DisplayName("should accept UUID type with ASSIGNED strategy")
        void shouldAcceptUuidWithAssigned() {
            // Given
            TypeRef idType = mockTypeRef("java.util.UUID");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.ASSIGNED;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "ASSIGNED should accept any type");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should accept Short type with numeric strategies")
        void shouldAcceptShortWithNumericStrategy() {
            // Given
            TypeRef idType = mockTypeRef("Short");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.IDENTITY;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Short should be compatible with IDENTITY");
        }

        @Test
        @DisplayName("should accept Byte type with numeric strategies")
        void shouldAcceptByteWithNumericStrategy() {
            // Given
            TypeRef idType = mockTypeRef("Byte");
            JpaPluginOptions.IdGenerationStrategy strategy = JpaPluginOptions.IdGenerationStrategy.SEQUENCE;

            // When
            Optional<Diagnostic> result = validator.validate(idType, strategy, CONTEXT_NAME);

            // Then
            assertTrue(result.isEmpty(), "Byte should be compatible with SEQUENCE");
        }
    }

    // Helper method to create mock TypeRef
    private TypeRef mockTypeRef(String typeName) {
        return new TypeRef() {
            @Override
            public io.hexaglue.spi.types.TypeKind kind() {
                return io.hexaglue.spi.types.TypeKind.CLASS;
            }

            @Override
            public io.hexaglue.spi.types.Nullability nullability() {
                return io.hexaglue.spi.types.Nullability.UNSPECIFIED;
            }

            @Override
            public io.hexaglue.spi.types.TypeName name() {
                return io.hexaglue.spi.types.TypeName.of(typeName);
            }

            @Override
            public TypeRef withNullability(io.hexaglue.spi.types.Nullability nullability) {
                return this;
            }

            @Override
            public String render() {
                return typeName;
            }

            @Override
            public java.util.Optional<io.hexaglue.spi.types.CollectionMetadata> collectionMetadata() {
                return java.util.Optional.empty();
            }
        };
    }
}
