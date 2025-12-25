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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.jpa.config.JpaPluginOptions;
import io.hexaglue.plugin.jpa.model.IdModel;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IdTypeResolver}.
 *
 * @since 0.4.0
 */
@DisplayName("IdTypeResolver")
class IdTypeResolverTest {

    private GenerationContextSpec context;
    private JpaPluginOptions options;
    private IdTypeResolver resolver;

    @BeforeEach
    void setUp() {
        context = mock(GenerationContextSpec.class);
        options = mock(JpaPluginOptions.class);

        // Setup basic mocks
        IrView irModel = mock(IrView.class);
        DomainModelView domainModel = mock(DomainModelView.class);
        DiagnosticReporter diagnostics = mock(DiagnosticReporter.class);
        TypeSystemSpec typeSystem = mock(TypeSystemSpec.class);

        when(context.model()).thenReturn(irModel);
        when(irModel.domain()).thenReturn(domainModel);
        when(context.diagnostics()).thenReturn(diagnostics);
        when(context.types()).thenReturn(typeSystem);
        when(options.idStrategy()).thenReturn(JpaPluginOptions.IdGenerationStrategy.IDENTITY);
        when(options.sequenceName()).thenReturn("");

        // Setup typeSystem to return ClassRefs for fallback cases
        when(typeSystem.classRef(any(String.class))).thenAnswer(invocation -> {
            String className = invocation.getArgument(0);
            return ClassRef.of(className);
        });

        resolver = new IdTypeResolver(context, options);
    }

    @Nested
    @DisplayName("Simple ID types")
    class SimpleIdTypesTests {

        @Test
        @DisplayName("should resolve Long ID from findById method")
        void shouldResolveLongIdFromFindById() {
            // Given: Port with findById(Long) method
            PortView port = createPortWithFindById("java.lang.Long");

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertFalse(result.isComposite());
            assertEquals("java.lang.Long", result.unwrappedType().render());
            assertEquals(JpaPluginOptions.IdGenerationStrategy.IDENTITY, result.strategy());
        }

        @Test
        @DisplayName("should resolve String ID from findById method")
        void shouldResolveStringIdFromFindById() {
            // Given: Port with findById(String) method
            PortView port = createPortWithFindById("java.lang.String");
            when(options.idStrategy()).thenReturn(JpaPluginOptions.IdGenerationStrategy.ASSIGNED);

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertFalse(result.isComposite());
            assertEquals("java.lang.String", result.unwrappedType().render());
        }

        @Test
        @DisplayName("should resolve UUID ID from findById method")
        void shouldResolveUuidIdFromFindById() {
            // Given: Port with findById(UUID) method
            PortView port = createPortWithFindById("java.util.UUID");
            when(options.idStrategy()).thenReturn(JpaPluginOptions.IdGenerationStrategy.UUID);

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertFalse(result.isComposite());
            assertEquals("java.util.UUID", result.unwrappedType().render());
        }
    }

    @Nested
    @DisplayName("Value Object ID unwrapping")
    class ValueObjectIdUnwrappingTests {

        @Test
        @DisplayName("should unwrap single-field Value Object ID")
        void shouldUnwrapSingleFieldValueObjectId() {
            // Given: CustomerId value object with single Long field
            PortView port = createPortWithFindById("com.example.CustomerId");

            DomainTypeView customerIdType = createSingleFieldValueObject(
                    "com.example.CustomerId", DomainTypeKind.IDENTIFIER, "value", "java.lang.Long");

            when(context.model().domain().findType("com.example.CustomerId")).thenReturn(Optional.of(customerIdType));

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertFalse(result.isComposite());
            assertEquals("java.lang.Long", result.unwrappedType().render(), "Should unwrap CustomerId to Long");
            assertEquals("com.example.CustomerId", result.originalType().render(), "Should keep original type");
        }

        @Test
        @DisplayName("should unwrap Email to String")
        void shouldUnwrapEmailToString() {
            // Given: Email value object with single String field
            PortView port = createPortWithFindById("com.example.Email");

            DomainTypeView emailType = createSingleFieldValueObject(
                    "com.example.Email", DomainTypeKind.RECORD, "value", "java.lang.String");

            when(context.model().domain().findType("com.example.Email")).thenReturn(Optional.of(emailType));

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals("java.lang.String", result.unwrappedType().render());
        }
    }

    @Nested
    @DisplayName("Composite ID detection")
    class CompositeIdTests {

        @Test
        @DisplayName("should detect composite ID from multi-field Value Object")
        void shouldDetectCompositeIdFromMultiFieldValueObject() {
            // Given: OrderId with customerId and orderNumber fields
            PortView port = createPortWithFindById("com.example.OrderId");

            DomainTypeView orderIdType = createMultiFieldValueObject("com.example.OrderId");

            when(context.model().domain().findType("com.example.OrderId")).thenReturn(Optional.of(orderIdType));

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertTrue(result.isComposite(), "Multi-field VO should be composite ID");
            assertEquals("com.example.OrderId", result.unwrappedType().render());
        }
    }

    @Nested
    @DisplayName("ID inference from method names")
    class IdInferenceTests {

        @Test
        @DisplayName("should infer ID from deleteById method")
        void shouldInferIdFromDeleteById() {
            // Given: Port with deleteById(Long) method
            PortView port = createPortWithMethod("deleteById", "java.lang.Long");

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals("java.lang.Long", result.unwrappedType().render());
        }

        @Test
        @DisplayName("should infer ID from existsById method")
        void shouldInferIdFromExistsById() {
            // Given: Port with existsById(String) method
            PortView port = createPortWithMethod("existsById", "java.lang.String");

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals("java.lang.String", result.unwrappedType().render());
        }

        @Test
        @DisplayName("should infer ID from parameter named 'id'")
        void shouldInferIdFromParameterNamedId() {
            // Given: Port with method having parameter named "id"
            PortView port = createPortWithIdParameter("java.lang.Long");

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals("java.lang.Long", result.unwrappedType().render());
        }
    }

    @Nested
    @DisplayName("Fallback heuristics")
    class FallbackHeuristicsTests {

        @Test
        @DisplayName("should use String for XxxId types when not in domain model")
        void shouldUseStringForIdSuffixedTypes() {
            // Given: Port with ProductId that's not in domain model
            PortView port = createPortWithFindById("com.example.ProductId");

            when(context.model().domain().findType("com.example.ProductId")).thenReturn(Optional.empty());

            // When
            IdModel result = resolver.resolve(port);

            // Then
            assertNotNull(result);
            assertEquals(
                    "java.lang.String", result.unwrappedType().render(), "Should fallback to String for XxxId types");
        }
    }

    // Helper methods

    private PortView createPortWithFindById(String idType) {
        return createPortWithMethod("findById", idType);
    }

    private PortView createPortWithMethod(String methodName, String paramType) {
        PortView port = mock(PortView.class);
        PortMethodView method = mock(PortMethodView.class);
        PortParameterView parameter = mock(PortParameterView.class);
        TypeRef idTypeRef = createTypeRef(paramType);

        when(port.qualifiedName()).thenReturn("com.example.TestRepository");
        when(port.methods()).thenReturn(List.of(method));
        when(method.name()).thenReturn(methodName);
        when(method.parameters()).thenReturn(List.of(parameter));
        when(parameter.type()).thenReturn(idTypeRef);
        when(parameter.name()).thenReturn("id");

        return port;
    }

    private PortView createPortWithIdParameter(String idType) {
        PortView port = mock(PortView.class);
        PortMethodView method = mock(PortMethodView.class);
        PortParameterView parameter = mock(PortParameterView.class);
        TypeRef idTypeRef = createTypeRef(idType);

        when(port.qualifiedName()).thenReturn("com.example.TestRepository");
        when(port.methods()).thenReturn(List.of(method));
        when(method.name()).thenReturn("findByStatus");
        when(method.parameters()).thenReturn(List.of(parameter));
        when(parameter.type()).thenReturn(idTypeRef);
        when(parameter.name()).thenReturn("id");

        return port;
    }

    private TypeRef createTypeRef(String typeName) {
        TypeRef type = mock(TypeRef.class);
        TypeName name = TypeName.of(typeName);

        when(type.kind()).thenReturn(TypeKind.CLASS);
        when(type.name()).thenReturn(name);
        when(type.render()).thenReturn(typeName);
        when(type.nullability()).thenReturn(Nullability.NONNULL);
        when(type.withNullability(any())).thenReturn(type);
        when(type.collectionMetadata()).thenReturn(Optional.empty());

        return type;
    }

    private DomainTypeView createSingleFieldValueObject(
            String typeName, DomainTypeKind kind, String fieldName, String fieldType) {
        DomainTypeView voType = mock(DomainTypeView.class);
        DomainPropertyView field = createProperty(fieldName, fieldType);

        when(voType.qualifiedName()).thenReturn(typeName);
        when(voType.kind()).thenReturn(kind);
        when(voType.properties()).thenReturn(List.of(field));

        return voType;
    }

    private DomainTypeView createMultiFieldValueObject(String typeName) {
        DomainTypeView voType = mock(DomainTypeView.class);
        DomainPropertyView field1 = createProperty("customerId", "java.lang.Long");
        DomainPropertyView field2 = createProperty("orderNumber", "java.lang.String");

        when(voType.qualifiedName()).thenReturn(typeName);
        when(voType.kind()).thenReturn(DomainTypeKind.IDENTIFIER);
        when(voType.properties()).thenReturn(List.of(field1, field2));

        return voType;
    }

    private DomainPropertyView createProperty(String name, String typeName) {
        DomainPropertyView property = mock(DomainPropertyView.class);
        TypeRef type = createTypeRef(typeName);

        when(property.name()).thenReturn(name);
        when(property.type()).thenReturn(type);

        return property;
    }
}
