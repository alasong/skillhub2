package com.iflytek.skillhub.domain.skill.compat;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClaudeCodeAdapter implements AgentPlatformAdapter {

    @Override
    public String platformId() { return "claude-code"; }

    @Override
    public String generateInvocation(String skillName, Map<String, Object> inputs) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(skillName).append("\n");
        sb.append("description: Auto-generated from skill.yaml\n");
        sb.append("---\n\n");
        sb.append("## Inputs\n\n");
        inputs.forEach((key, value) ->
            sb.append("- `").append(key).append("`: ").append(value).append("\n")
        );
        sb.append("\n## Instructions\n\n");
        sb.append("Execute the skill with the provided inputs and return structured output.\n");
        return sb.toString();
    }

    @Override
    public String generateInstall(String skillName, String skillVersion) {
        return "skillhub install " + skillName + "@" + skillVersion + " --agent claude-code";
    }

    @Override
    public boolean isCompatible(Map<String, Object> inputs, Map<String, Object> outputs) {
        // Claude Code can handle any JSON in/out via skill system
        return true;
    }
}
