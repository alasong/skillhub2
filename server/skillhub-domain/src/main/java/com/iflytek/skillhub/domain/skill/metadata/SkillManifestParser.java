package com.iflytek.skillhub.domain.skill.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class SkillManifestParser {
    private static final Logger log = LoggerFactory.getLogger(SkillManifestParser.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public SkillManifest parse(InputStream yamlStream) {
        try {
            return yamlMapper.readValue(yamlStream, SkillManifest.class);
        } catch (Exception e) {
            log.error("Failed to parse skill.yaml", e);
            throw new IllegalArgumentException("Invalid skill.yaml: " + e.getMessage(), e);
        }
    }
}
