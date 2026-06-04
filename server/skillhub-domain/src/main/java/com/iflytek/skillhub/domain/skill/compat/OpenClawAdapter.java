package com.iflytek.skillhub.domain.skill.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenClawAdapter implements AgentPlatformAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String platformId() { return "openclaw"; }

    @Override
    public String generateInvocation(String skillName, Map<String, Object> inputs) {
        return "{ \"skill\": \"" + skillName + "\", \"inputs\": " + toJson(inputs) + " }";
    }

    @Override
    public String generateInstall(String skillName, String skillVersion) {
        return "skillhub install " + skillName + "@" + skillVersion + " --agent openclaw";
    }

    @Override
    public boolean isCompatible(Map<String, Object> inputs, Map<String, Object> outputs) {
        // OpenClaw requires all inputs to be JSON-serializable primitives
        return inputs.values().stream().noneMatch(v ->
            v instanceof Map || v instanceof Iterable || v.getClass().isArray());
    }

    private String toJson(Map<String, Object> map) {
        try { return objectMapper.writeValueAsString(map); } catch (Exception e) { return "{}"; }
    }
}
