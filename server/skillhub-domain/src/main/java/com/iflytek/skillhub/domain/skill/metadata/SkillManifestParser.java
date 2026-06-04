package com.iflytek.skillhub.domain.skill.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Component
public class SkillManifestParser {
    private static final Logger log = LoggerFactory.getLogger(SkillManifestParser.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public SkillManifest parse(InputStream yamlStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(yamlStream);
            return mapper.convertValue(raw, SkillManifest.class);
        } catch (Exception e) {
            log.error("Failed to parse skill.yaml", e);
            throw new IllegalArgumentException("Invalid skill.yaml: " + e.getMessage(), e);
        }
    }
}
