package com.iflytek.skillhub.domain.skill.compat;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenClawAdapter implements AgentPlatformAdapter {

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
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(", ");
            sb.append("\"").append(k).append("\": ");
            if (v instanceof String) sb.append("\"").append(v).append("\"");
            else sb.append(v);
        });
        sb.append("}");
        return sb.toString();
    }
}
