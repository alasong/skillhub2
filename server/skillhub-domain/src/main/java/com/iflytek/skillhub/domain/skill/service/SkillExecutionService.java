package com.iflytek.skillhub.domain.skill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Invokes a skill with the given inputs and returns the output.
 *
 * <p>This service abstracts the execution of a skill, whether through the
 * pipeline DAG runtime, direct LLM API call, or any other execution strategy.
 * The actual execution logic is not yet implemented.
 */
@Service
public class SkillExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutionService.class);

    /**
     * Executes a skill and returns its output as a string.
     *
     * @param skillName      the skill name to execute
     * @param skillVersion   the skill version to execute
     * @param inputs         the input parameters for this execution
     * @param timeoutSeconds maximum time to wait for execution
     * @return the skill output as a string
     * @throws UnsupportedOperationException until the skill execution engine is connected
     */
    public String execute(String skillName, String skillVersion,
                          Map<String, Object> inputs, int timeoutSeconds) {
        // TODO: implement when skill execution engine is available
        // 1. Load the skill manifest for parameter mapping
        // 2. Build the execution context from inputs
        // 3. Invoke the skill (direct LLM call, pipeline execution, etc.)
        // 4. Capture and return the output
        log.warn("Skill execution not yet implemented for {}:{} with inputs {}",
                skillName, skillVersion, inputs);
        throw new UnsupportedOperationException(
                "Skill execution not yet implemented for " + skillName + ":" + skillVersion);
    }
}
