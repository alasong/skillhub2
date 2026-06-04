package com.iflytek.skillhub.domain.skill.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed value object for pipeline node parameter mapping.
 * Encapsulates JSON serialization so callers never touch raw strings.
 */
public class ParameterMapping {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> mappings; // inputParam -> "$.source.output.field"

    public ParameterMapping() {
        this.mappings = new LinkedHashMap<>();
    }

    public ParameterMapping(Map<String, String> mappings) {
        this.mappings = new LinkedHashMap<>(mappings);
    }

    public static ParameterMapping fromJson(String json) {
        if (json == null || json.isBlank()) return new ParameterMapping();
        try {
            Map<String, String> map = mapper.readValue(json, new TypeReference<>() {});
            return new ParameterMapping(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid parameter mapping JSON: " + json, e);
        }
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(mappings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize parameter mapping", e);
        }
    }

    public void put(String inputParam, String jsonPath) {
        mappings.put(inputParam, jsonPath);
    }

    public String get(String inputParam) {
        return mappings.get(inputParam);
    }

    public Map<String, String> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public boolean isEmpty() { return mappings.isEmpty(); }
    public int size() { return mappings.size(); }

    @Override
    public String toString() { return toJson(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterMapping that)) return false;
        return mappings.equals(that.mappings);
    }

    @Override
    public int hashCode() { return mappings.hashCode(); }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<ParameterMapping, String> {
        @Override
        public String convertToDatabaseColumn(ParameterMapping attr) {
            return attr == null ? null : attr.toJson();
        }
        @Override
        public ParameterMapping convertToEntityAttribute(String dbData) {
            return ParameterMapping.fromJson(dbData);
        }
    }
}
