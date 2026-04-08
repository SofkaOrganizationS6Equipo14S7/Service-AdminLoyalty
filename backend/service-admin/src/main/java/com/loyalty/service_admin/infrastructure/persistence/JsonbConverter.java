package com.loyalty.service_admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Converter
@RequiredArgsConstructor
public class JsonbConverter implements AttributeConverter<Object, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        return Optional.ofNullable(dbData)
                .map(data -> {
                    try {
                        return objectMapper.readValue(data, Object.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error reading JSON", e);
                    }
                })
                .orElse(null);
    }
}