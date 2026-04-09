package com.loyalty.service_admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonbConverter Unit Tests")
class JsonbConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonbConverter converter;

    @Test
    void testConvertToDatabaseColumn_nullInput_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToDatabaseColumn_validObject_returnsJson() throws Exception {
        Map<String, String> data = Map.of("key", "value");
        when(objectMapper.writeValueAsString(data)).thenReturn("{\"key\":\"value\"}");

        String result = converter.convertToDatabaseColumn(data);

        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    void testConvertToDatabaseColumn_serializationError_throwsRuntimeException() throws Exception {
        Object data = new Object();
        when(objectMapper.writeValueAsString(data)).thenThrow(new JsonProcessingException("error") {});

        assertThrows(RuntimeException.class, () -> converter.convertToDatabaseColumn(data));
    }

    @Test
    void testConvertToEntityAttribute_nullInput_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void testConvertToEntityAttribute_validJson_returnsObject() throws Exception {
        String json = "{\"key\":\"value\"}";
        Map<String, String> expected = Map.of("key", "value");
        when(objectMapper.readValue(json, Object.class)).thenReturn(expected);

        Object result = converter.convertToEntityAttribute(json);

        assertEquals(expected, result);
    }

    @Test
    void testConvertToEntityAttribute_invalidJson_throwsRuntimeException() throws Exception {
        String json = "invalid-json";
        when(objectMapper.readValue(json, Object.class)).thenThrow(new JsonProcessingException("parse error") {});

        assertThrows(RuntimeException.class, () -> converter.convertToEntityAttribute(json));
    }
}
