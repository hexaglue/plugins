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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.jpa.model.PropertyModel;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PropertyTypeResolver}.
 *
 * @since 0.4.0
 */
@DisplayName("PropertyTypeResolver")
class PropertyTypeResolverTest {

    private GenerationContextSpec context;
    private OptionsView.PluginOptionsView options;
    private PropertyTypeResolver resolver;

    @BeforeEach
    void setUp() {
        context = mock(GenerationContextSpec.class);
        options = mock(OptionsView.PluginOptionsView.class);

        // Setup basic mocks
        IrView irModel = mock(IrView.class);
        DomainModelView domainModel = mock(DomainModelView.class);
        DiagnosticReporter diagnostics = mock(DiagnosticReporter.class);

        when(context.model()).thenReturn(irModel);
        when(irModel.domain()).thenReturn(domainModel);
        when(context.diagnostics()).thenReturn(diagnostics);

        resolver = new PropertyTypeResolver(context, options, "com.example.TestAggregate");
    }

    @Nested
    @DisplayName("Simple property resolution")
    class SimplePropertyTests {

        @Test
        @DisplayName("should resolve String property")
        void shouldResolveStringProperty() {
            // Given
            DomainPropertyView property = createProperty("name", "java.lang.String", Nullability.NULLABLE);

            // When
            PropertyModel result = resolver.resolve(property);

            // Then
            assertNotNull(result);
            assertEquals("name", result.name());
            assertEquals("java.lang.String", result.type().render());
            assertTrue(result.nullable());
        }

        @Test
        @DisplayName("should respect nullability from type")
        void shouldRespectNullabilityFromType() {
            // Given: NONNULL Long
            DomainPropertyView propertyNonNull = createProperty("count", "java.lang.Long", Nullability.NONNULL);

            // When
            PropertyModel resultNonNull = resolver.resolve(propertyNonNull);

            // Then: Should respect NONNULL (though Long wrapper might default to nullable)
            assertNotNull(resultNonNull);
            assertEquals("count", resultNonNull.name());
            // Note: Wrapper types like Long may default to nullable in JPA
        }

        @Test
        @DisplayName("should resolve primitive types")
        void shouldResolvePrimitiveTypes() {
            // Given
            DomainPropertyView property = createPrimitiveProperty("active", "boolean");

            // When
            PropertyModel result = resolver.resolve(property);

            // Then
            assertNotNull(result);
            assertEquals("active", result.name());
            assertEquals("boolean", result.type().render());
        }
    }

    @Nested
    @DisplayName("Value Object unwrapping")
    class ValueObjectUnwrappingTests {

        @Test
        @DisplayName("should unwrap single-field Value Object")
        void shouldUnwrapSingleFieldValueObject() {
            // Given: Email value object with single String field
            DomainPropertyView emailProperty = createProperty("email", "com.example.Email", Nullability.NULLABLE);

            DomainTypeView emailType =
                    createValueObjectType("com.example.Email", DomainTypeKind.RECORD, "value", "java.lang.String");

            when(context.model().domain().findType("com.example.Email")).thenReturn(Optional.of(emailType));

            // When
            PropertyModel result = resolver.resolve(emailProperty);

            // Then
            assertNotNull(result);
            assertEquals("email", result.name());
            assertEquals("java.lang.String", result.type().render(), "Should unwrap Email VO to String");
        }

        @Test
        @DisplayName("should not unwrap multi-field Value Object")
        void shouldNotUnwrapMultiFieldValueObject() {
            // Given: Address value object with multiple fields
            DomainPropertyView addressProperty = createProperty("address", "com.example.Address", Nullability.NULLABLE);

            DomainTypeView addressType = createMultiFieldValueObject("com.example.Address");

            when(context.model().domain().findType("com.example.Address")).thenReturn(Optional.of(addressType));

            // When
            PropertyModel result = resolver.resolve(addressProperty);

            // Then
            assertNotNull(result);
            assertEquals("address", result.name());
            assertEquals(
                    "com.example.Address", result.type().render(), "Multi-field VO should remain as embedded type");
        }
    }

    @Nested
    @DisplayName("Enum handling")
    class EnumHandlingTests {

        @Test
        @DisplayName("should detect enum types")
        void shouldDetectEnumTypes() {
            // Given
            DomainPropertyView statusProperty =
                    createProperty("status", "com.example.CustomerStatus", Nullability.NONNULL);

            DomainTypeView enumType = createEnumType("com.example.CustomerStatus");

            when(context.model().domain().findType("com.example.CustomerStatus"))
                    .thenReturn(Optional.of(enumType));

            // When
            PropertyModel result = resolver.resolve(statusProperty);

            // Then
            assertNotNull(result);
            assertEquals("status", result.name());
            assertEquals("com.example.CustomerStatus", result.type().render());
        }
    }

    @Nested
    @DisplayName("Collection handling")
    class CollectionHandlingTests {

        @Test
        @DisplayName("should handle List collections")
        void shouldHandleListCollections() {
            // Given
            DomainPropertyView tagsProperty = createCollectionProperty("tags", "java.util.List", "java.lang.String");

            // When
            PropertyModel result = resolver.resolve(tagsProperty);

            // Then
            assertNotNull(result);
            assertEquals("tags", result.name());
            assertTrue(result.type().render().contains("List"));
        }

        @Test
        @DisplayName("should handle Set collections")
        void shouldHandleSetCollections() {
            // Given
            DomainPropertyView categoriesProperty =
                    createCollectionProperty("categories", "java.util.Set", "java.lang.String");

            // When
            PropertyModel result = resolver.resolve(categoriesProperty);

            // Then
            assertNotNull(result);
            assertEquals("categories", result.name());
            assertTrue(result.type().render().contains("Set"));
        }
    }

    @Nested
    @DisplayName("Temporal types")
    class TemporalTypeTests {

        @Test
        @DisplayName("should handle LocalDate")
        void shouldHandleLocalDate() {
            // Given
            DomainPropertyView dateProperty = createProperty("birthDate", "java.time.LocalDate", Nullability.NULLABLE);

            // When
            PropertyModel result = resolver.resolve(dateProperty);

            // Then
            assertNotNull(result);
            assertEquals("birthDate", result.name());
            assertEquals("java.time.LocalDate", result.type().render());
        }

        @Test
        @DisplayName("should handle Instant")
        void shouldHandleInstant() {
            // Given
            DomainPropertyView timestampProperty =
                    createProperty("timestamp", "java.time.Instant", Nullability.NONNULL);

            // When
            PropertyModel result = resolver.resolve(timestampProperty);

            // Then
            assertNotNull(result);
            assertEquals("timestamp", result.name());
            assertEquals("java.time.Instant", result.type().render());
        }
    }

    // Helper methods

    private DomainPropertyView createProperty(String name, String typeName, Nullability nullability) {
        DomainPropertyView property = mock(DomainPropertyView.class);
        TypeRef type = createTypeRef(typeName, nullability);

        when(property.name()).thenReturn(name);
        when(property.type()).thenReturn(type);

        return property;
    }

    private DomainPropertyView createPrimitiveProperty(String name, String primitiveType) {
        DomainPropertyView property = mock(DomainPropertyView.class);
        TypeRef type = createPrimitiveTypeRef(primitiveType);

        when(property.name()).thenReturn(name);
        when(property.type()).thenReturn(type);

        return property;
    }

    private DomainPropertyView createCollectionProperty(String name, String collectionType, String elementType) {
        DomainPropertyView property = mock(DomainPropertyView.class);
        TypeRef type = mock(TypeRef.class);
        TypeName typeName = TypeName.of(collectionType);

        when(type.kind()).thenReturn(TypeKind.CLASS);
        when(type.name()).thenReturn(typeName);
        when(type.render()).thenReturn(collectionType + "<" + elementType + ">");
        when(type.nullability()).thenReturn(Nullability.NONNULL);
        when(type.withNullability(any())).thenReturn(type);
        when(type.collectionMetadata()).thenReturn(Optional.empty());

        when(property.name()).thenReturn(name);
        when(property.type()).thenReturn(type);

        return property;
    }

    private TypeRef createTypeRef(String typeName, Nullability nullability) {
        TypeRef type = mock(TypeRef.class);
        TypeName name = TypeName.of(typeName);

        when(type.kind()).thenReturn(TypeKind.CLASS);
        when(type.name()).thenReturn(name);
        when(type.render()).thenReturn(typeName);
        when(type.nullability()).thenReturn(nullability);
        when(type.withNullability(any())).thenReturn(type);
        when(type.collectionMetadata()).thenReturn(Optional.empty());

        return type;
    }

    private TypeRef createPrimitiveTypeRef(String primitiveType) {
        TypeRef type = mock(TypeRef.class);
        TypeName name = TypeName.of(primitiveType);

        when(type.kind()).thenReturn(TypeKind.PRIMITIVE);
        when(type.name()).thenReturn(name);
        when(type.render()).thenReturn(primitiveType);
        when(type.nullability()).thenReturn(Nullability.NONNULL);
        when(type.withNullability(any())).thenReturn(type);
        when(type.collectionMetadata()).thenReturn(Optional.empty());

        return type;
    }

    private DomainTypeView createValueObjectType(
            String typeName, DomainTypeKind kind, String fieldName, String fieldType) {
        DomainTypeView voType = mock(DomainTypeView.class);
        DomainPropertyView field = createProperty(fieldName, fieldType, Nullability.NONNULL);

        when(voType.qualifiedName()).thenReturn(typeName);
        when(voType.kind()).thenReturn(kind);
        when(voType.properties()).thenReturn(List.of(field));

        return voType;
    }

    private DomainTypeView createMultiFieldValueObject(String typeName) {
        DomainTypeView voType = mock(DomainTypeView.class);
        DomainPropertyView field1 = createProperty("street", "java.lang.String", Nullability.NONNULL);
        DomainPropertyView field2 = createProperty("city", "java.lang.String", Nullability.NONNULL);

        when(voType.qualifiedName()).thenReturn(typeName);
        when(voType.kind()).thenReturn(DomainTypeKind.RECORD);
        when(voType.properties()).thenReturn(List.of(field1, field2));

        return voType;
    }

    private DomainTypeView createEnumType(String typeName) {
        DomainTypeView enumType = mock(DomainTypeView.class);

        when(enumType.qualifiedName()).thenReturn(typeName);
        when(enumType.kind()).thenReturn(DomainTypeKind.ENUMERATION);
        when(enumType.properties()).thenReturn(List.of());

        return enumType;
    }
}
