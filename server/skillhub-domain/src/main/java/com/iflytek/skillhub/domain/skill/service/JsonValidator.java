package com.iflytek.skillhub.domain.skill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON Schema validation utility for the JSON_SCHEMA validator type.
 * Uses the everit-org/json-schema library to validate actual output
 * against a JSON Schema document stored as the expected output.
 */
@Component
public class JsonValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonValidator.class);

    private final ObjectMapper mapper;

    public JsonValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Validates an actual JSON string against a JSON Schema document.
     *
     * @param actualJson the actual output as JSON string
     * @param schemaJson the JSON Schema document as JSON string
     * @return validation result with pass/fail and error messages
     */
    public ValidationResult validate(String actualJson, String schemaJson) {
        try {
            JsonNode schemaNode = mapper.readTree(schemaJson);
            JsonNode actualNode = mapper.readTree(actualJson);
            Schema schema = SchemaLoader.load(schemaNode);
            try {
                schema.validate(actualNode);
                return ValidationResult.passed();
            } catch (ValidationException e) {
                return ValidationResult.failed(e.getAllMessages());
            }
        } catch (Exception e) {
            log.warn("JSON Schema validation error: {}", e.getMessage());
            return ValidationResult.failed(List.of("Schema validation error: " + e.getMessage()));
        }
    }

    /**
     * Result of a JSON Schema validation operation.
     */
    public record ValidationResult(boolean passed, List<String> errors) {
        public static ValidationResult passed() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failed(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}
