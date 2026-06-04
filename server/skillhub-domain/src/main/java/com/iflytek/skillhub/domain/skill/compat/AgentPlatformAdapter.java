package com.iflytek.skillhub.domain.skill.compat;

import java.util.Map;

/**
 * Semantic adaptation layer: translates a skill's interface contract
 * into the runtime conventions of a specific agent platform.
 *
 * Each platform has different ways of invoking skills:
 *   - Claude Code: file-based skill.md with @tool annotations
 *   - OpenClaw: REST API with JSON payload
 *   - AstronClaw: gRPC with protobuf
 *   - LangChain: Python function with pydantic schema
 *
 * This adapter generates the platform-specific artifact from the
 * canonical skill.yaml manifest.
 */
public interface AgentPlatformAdapter {

    /** Platform identifier (e.g., "claude-code", "openclaw", "astronclaw", "langchain") */
    String platformId();

    /** Generate platform-specific invocation code from the canonical manifest */
    String generateInvocation(String skillName, Map<String, Object> inputs);

    /** Generate platform-specific installation artifact */
    String generateInstall(String skillName, String skillVersion);

    /** Validate that this skill's interface is compatible with this platform */
    boolean isCompatible(Map<String, Object> manifestInputs, Map<String, Object> manifestOutputs);
}
