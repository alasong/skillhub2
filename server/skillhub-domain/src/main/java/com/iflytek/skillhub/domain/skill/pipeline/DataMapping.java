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
 * Typed value object for pipeline edge data flow mapping.
 * Maps source output fields to target input fields via JSONPath expressions.
 */
public class DataMapping {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> mappings; // "source.output.field" -> "target.input.field"

    public DataMapping() {
        this.mappings = new LinkedHashMap<>();
    }

    public DataMapping(Map<String, String> mappings) {
        this.mappings = new LinkedHashMap<>(mappings);
    }

    public static DataMapping fromJson(String json) {
        if (json == null || json.isBlank()) return new DataMapping();
        try {
            Map<String, String> map = mapper.readValue(json, new TypeReference<>() {});
            return new DataMapping(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid data mapping JSON: " + json, e);
        }
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(mappings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize data mapping", e);
        }
    }

    public void put(String sourceOutputField, String targetInputField) {
        mappings.put(sourceOutputField, targetInputField);
    }

    public String get(String sourceOutputField) {
        return mappings.get(sourceOutputField);
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
        if (!(o instanceof DataMapping that)) return false;
        return mappings.equals(that.mappings);
    }

    @Override
    public int hashCode() { return mappings.hashCode(); }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<DataMapping, String> {
        @Override
        public String convertToDatabaseColumn(DataMapping attr) {
            return attr == null ? null : attr.toJson();
        }
        @Override
        public DataMapping convertToEntityAttribute(String dbData) {
            return DataMapping.fromJson(dbData);
        }
    }
}
