package com.iflytek.skillhub.domain.skill.metadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/**
 * Structured skill manifest (skill.yaml) — the canonical declaration of a skill.
 * Replaces free-form skill.md with machine-readable inputs/outputs,
 * resource constraints, and lifecycle configuration.
 */
public class SkillManifest {

    // --- Core identity ---
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{1,63}$")
    private String name;

    @NotBlank @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(-[a-zA-Z0-9._+]+)?$")
    private String version;

    private String description;
    private List<String> authors;
    private String license = "Apache-2.0";
    private List<String> keywords;

    // --- Interface contract ---
    private InputSpec inputs;
    private OutputSpec outputs;

    // --- Runtime constraints ---
    private ResourceSpec resources;
    private LifecycleSpec lifecycle;

    // --- Composition ---
    private Map<String, String> dependencies;  // skill-name -> version constraint
    private List<String> agentPlatforms;        // ["claude-code", "openclaw", "astronclaw"]

    // --- Quality gates ---
    private EvalSpec eval;

    // --- Semver compatibility ---
    /**
     * Semver range indicating which skillhub CLI/server versions
     * this skill is compatible with. Example: ">=1.0.0 <2.0.0"
     */
    private String compatibleWith;

    /**
     * If true, this skill version is deprecated and should not
     * be installed. The deprecationMessage explains why and what
     * to use instead.
     */
    private boolean deprecated;

    private String deprecationMessage;

    /**
     * Fully-qualified skill names (namespace/name) that this
     * skill supersedes. When resolving dependencies, a reference
     * to a replaced skill should redirect to this one.
     */
    private List<String> replaces;

    // Getters/Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public InputSpec getInputs() { return inputs; }
    public void setInputs(InputSpec inputs) { this.inputs = inputs; }
    public OutputSpec getOutputs() { return outputs; }
    public void setOutputs(OutputSpec outputs) { this.outputs = outputs; }
    public ResourceSpec getResources() { return resources; }
    public void setResources(ResourceSpec resources) { this.resources = resources; }
    public LifecycleSpec getLifecycle() { return lifecycle; }
    public void setLifecycle(LifecycleSpec lifecycle) { this.lifecycle = lifecycle; }
    public Map<String, String> getDependencies() { return dependencies; }
    public void setDependencies(Map<String, String> dependencies) { this.dependencies = dependencies; }
    public List<String> getAgentPlatforms() { return agentPlatforms; }
    public void setAgentPlatforms(List<String> agentPlatforms) { this.agentPlatforms = agentPlatforms; }
    public EvalSpec getEval() { return eval; }
    public void setEval(EvalSpec eval) { this.eval = eval; }

    public String getCompatibleWith() { return compatibleWith; }
    public void setCompatibleWith(String compatibleWith) { this.compatibleWith = compatibleWith; }
    public boolean isDeprecated() { return deprecated; }
    public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
    public String getDeprecationMessage() { return deprecationMessage; }
    public void setDeprecationMessage(String deprecationMessage) { this.deprecationMessage = deprecationMessage; }
    public List<String> getReplaces() { return replaces; }
    public void setReplaces(List<String> replaces) { this.replaces = replaces; }

    // --- Inner types ---

    public static class InputSpec {
        private Map<String, ParameterSpec> parameters;
        private List<String> required;

        public Map<String, ParameterSpec> getParameters() { return parameters; }
        public void setParameters(Map<String, ParameterSpec> p) { this.parameters = p; }
        public List<String> getRequired() { return required; }
        public void setRequired(List<String> required) { this.required = required; }
    }

    public static class ParameterSpec {
        private ParamType type;        // string | number | boolean | array | object | file
        private String description;
        private Object defaultVal;
        private List<Object> enumValues;
        private String pattern;     // regex for string validation
        private Number minimum;
        private Number maximum;
        private String jsonSchema;  // full JSON Schema override

        public ParamType getType() { return type; }
        public void setType(ParamType type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }
        public Object getDefaultVal() { return defaultVal; }
        public void setDefaultVal(Object d) { this.defaultVal = d; }
        public List<Object> getEnumValues() { return enumValues; }
        public void setEnumValues(List<Object> e) { this.enumValues = e; }
        public String getPattern() { return pattern; }
        public void setPattern(String p) { this.pattern = p; }
        public Number getMinimum() { return minimum; }
        public void setMinimum(Number n) { this.minimum = n; }
        public Number getMaximum() { return maximum; }
        public void setMaximum(Number n) { this.maximum = n; }
        public String getJsonSchema() { return jsonSchema; }
        public void setJsonSchema(String s) { this.jsonSchema = s; }
    }

    public static class OutputSpec {
        private OutputType type;         // json | text | stream | file | void
        private String jsonSchema;   // JSON Schema for structured output
        private List<OutputExample> examples;

        public OutputType getType() { return type; }
        public void setType(OutputType t) { this.type = t; }
        public String getJsonSchema() { return jsonSchema; }
        public void setJsonSchema(String s) { this.jsonSchema = s; }
        public List<OutputExample> getExamples() { return examples; }
        public void setExamples(List<OutputExample> e) { this.examples = e; }
    }

    public static class OutputExample {
        private String description;
        private Object value;
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }
        public Object getValue() { return value; }
        public void setValue(Object v) { this.value = v; }
    }

    public static class ResourceSpec {
        private int maxTokens = 4096;
        private int timeoutSeconds = 30;
        private int maxRetries = 2;
        private String requiredTool;  // "file_read" | "web_fetch" | "shell" — agent capability needed

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int m) { this.maxTokens = m; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int t) { this.timeoutSeconds = t; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int m) { this.maxRetries = m; }
        public String getRequiredTool() { return requiredTool; }
        public void setRequiredTool(String r) { this.requiredTool = r; }
    }

    public static class LifecycleSpec {
        private String onInstall;
        private String onUpgrade;
        private String onUninstall;
        private String healthCheck;  // endpoint or command to verify liveness

        public String getOnInstall() { return onInstall; }
        public void setOnInstall(String o) { this.onInstall = o; }
        public String getOnUpgrade() { return onUpgrade; }
        public void setOnUpgrade(String o) { this.onUpgrade = o; }
        public String getOnUninstall() { return onUninstall; }
        public void setOnUninstall(String o) { this.onUninstall = o; }
        public String getHealthCheck() { return healthCheck; }
        public void setHealthCheck(String h) { this.healthCheck = h; }
    }

    public static class EvalSpec {
        private String framework = "pass@k";
        private int k = 1;
        private int minPassRate = 70;  // percentage
        private List<EvalCase> cases;

        public String getFramework() { return framework; }
        public void setFramework(String f) { this.framework = f; }
        public int getK() { return k; }
        public void setK(int k) { this.k = k; }
        public int getMinPassRate() { return minPassRate; }
        public void setMinPassRate(int m) { this.minPassRate = m; }
        public List<EvalCase> getCases() { return cases; }
        public void setCases(List<EvalCase> c) { this.cases = c; }
    }

    public static class EvalCase {
        private String name;
        private Map<String, Object> input;
        private Object expectedOutput;
        private ValidatorType validator;  // "exact" | "contains" | "jsonSchema" | "llmJudge"

        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> i) { this.input = i; }
        public Object getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(Object e) { this.expectedOutput = e; }
        public ValidatorType getValidator() { return validator; }
        public void setValidator(ValidatorType v) { this.validator = v; }
    }

    public enum OutputType { JSON, TEXT, STREAM, FILE, VOID }
    public enum ParamType { STRING, NUMBER, BOOLEAN, ARRAY, OBJECT, FILE }
    public enum ValidatorType { EXACT, CONTAINS, JSON_SCHEMA, LLM_JUDGE }
}
