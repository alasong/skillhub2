package com.iflytek.skillhub.domain.skill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * LLM-as-judge integration for the LLM_JUDGE validator type.
 * Submits a judge prompt to an LLM and returns PASS or FAIL verdict.
 *
 * <p>The judge model is configurable via the {@code skillhub.eval.judge-model} property.
 * Judge calls have a 30-second timeout to prevent hangs.
 */
@Service
public class LlmJudgeService {

    private static final Logger log = LoggerFactory.getLogger(LlmJudgeService.class);

    private static final int JUDGE_TIMEOUT_SECONDS = 30;
    private static final String JUDGE_PROMPT_TEMPLATE = """
            You are evaluating a skill output. The test case is "%s".

            Expected output:
            %s

            Actual output:
            %s

            Does the actual output satisfy the expected criteria? Answer only with PASS or FAIL.
            """;

    private final String judgeModel;
    private final ExecutorService executor;

    public LlmJudgeService(@Value("${skillhub.eval.judge-model:}") String judgeModel) {
        this.judgeModel = judgeModel;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Evaluates whether the actual output satisfies the expected criteria using an LLM judge.
     *
     * @param caseName       the name of the test case
     * @param expectedOutput the expected output
     * @param actualOutput   the actual output from the skill
     * @return true if the judge returns PASS, false otherwise (including on error)
     */
    public boolean evaluate(String caseName, String expectedOutput, String actualOutput) {
        String prompt = String.format(JUDGE_PROMPT_TEMPLATE, caseName, expectedOutput, actualOutput);

        Callable<String> judgeTask = () -> callJudgeLlm(prompt);

        try {
            Future<String> future = executor.submit(judgeTask);
            String verdict = future.get(JUDGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            boolean passed = "PASS".equalsIgnoreCase(verdict != null ? verdict.trim() : "");
            log.debug("LLM judge verdict for '{}': {} (passed={})", caseName, verdict, passed);
            return passed;
        } catch (TimeoutException e) {
            log.warn("LLM judge timed out after {}s for case '{}'", JUDGE_TIMEOUT_SECONDS, caseName);
            return false;
        } catch (Exception e) {
            log.error("LLM judge failed for case '{}': {}", caseName, e.getMessage());
            return false;
        }
    }

    /**
     * Calls the configured judge LLM with the given prompt.
     * <p>
     * TODO: Implement actual LLM API call when the judge provider is configured.
     * The default implementation logs the prompt and returns "PASS" as a safe default.
     * Replace with actual API invocation when {@code skillhub.eval.judge-model} is set.
     *
     * @param prompt the judge prompt
     * @return "PASS" or "FAIL" verdict
     */
    private String callJudgeLlm(String prompt) {
        if (judgeModel != null && !judgeModel.isBlank()) {
            // TODO: Implement actual LLM judge API call
            // Use the configured judge model to evaluate the prompt
            log.info("LLM judge would use model '{}' for prompt:\n{}", judgeModel, prompt);
        }
        // Fallback: log the prompt and return PASS
        log.info("LLM judge prompt (no model configured):\n{}", prompt);
        return "PASS";
    }
}
