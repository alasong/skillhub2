package com.iflytek.skillhub.domain.security;

/**
 * Options controlling eval gate behavior during publish.
 *
 * @param skipEval if true, the eval gate check is skipped entirely
 */
public record EvalGateOptions(boolean skipEval) {
    public static final EvalGateOptions DEFAULT = new EvalGateOptions(false);

    public static EvalGateOptions skip() {
        return new EvalGateOptions(true);
    }
}
