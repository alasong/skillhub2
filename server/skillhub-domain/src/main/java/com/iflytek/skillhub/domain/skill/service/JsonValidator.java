package com.iflytek.skillhub.domain.skill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class JsonValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonValidator.class);
    private final ObjectMapper mapper;

    public JsonValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ValidationResult validate(String actualJson, String schemaJson) {
        try {
            JsonNode actual = mapper.readTree(actualJson);
            JsonNode schema = mapper.readTree(schemaJson);
            List<String> errors = new ArrayList<>();
            validateNode(actual, schema, "$", errors);
            if (errors.isEmpty()) {
                return ValidationResult.ok();
            }
            return ValidationResult.fail(errors);
        } catch (Exception e) {
            log.warn("JSON Schema validation error: {}", e.getMessage());
            return ValidationResult.fail(List.of("Schema validation error: " + e.getMessage()));
        }
    }

    private void validateNode(JsonNode actual, JsonNode schema, String path, List<String> errors) {
        if (schema.has("type")) {
            String expectedType = schema.get("type").asText();
            if (!matchesType(actual, expectedType)) {
                errors.add(path + ": expected type " + expectedType + ", got " + actual.getNodeType());
                return;
            }
        }
        if (schema.has("properties") && actual.isObject()) {
            JsonNode props = schema.get("properties");
            Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode childSchema = field.getValue();
                if (actual.has(key)) {
                    validateNode(actual.get(key), childSchema, path + "." + key, errors);
                } else if (!childSchema.has("optional") || !childSchema.get("optional").asBoolean()) {
                    errors.add(path + "." + key + ": missing required field");
                }
            }
        }
        if (schema.has("items") && actual.isArray()) {
            JsonNode itemSchema = schema.get("items");
            for (int i = 0; i < actual.size(); i++) {
                validateNode(actual.get(i), itemSchema, path + "[" + i + "]", errors);
            }
        }
        if (schema.has("enum")) {
            boolean matched = false;
            for (JsonNode enumVal : schema.get("enum")) {
                if (actual.equals(enumVal)) { matched = true; break; }
            }
            if (!matched) errors.add(path + ": value not in enum");
        }
    }

    private boolean matchesType(JsonNode node, String type) {
        return switch (type) {
            case "string" -> node.isTextual();
            case "number", "integer" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "object" -> node.isObject();
            case "array" -> node.isArray();
            case "null" -> node.isNull();
            default -> true;
        };
    }

    public record ValidationResult(boolean passed, List<String> errors) {
        public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
        public static ValidationResult fail(List<String> errors) { return new ValidationResult(false, errors); }
    }
}
