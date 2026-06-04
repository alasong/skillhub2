/**
 * skillhub init — scaffold a new skill from a template.
 * Usage: skillhub init <skill-name> [--template basic|advanced|pipeline]
 */
import { input, select, confirm } from '@inquirer/prompts';
import * as fs from 'fs';
import * as path from 'path';
import * as yaml from 'js-yaml';

const TEMPLATES: Record<string, string> = {
  basic: `name: {{name}}
version: 0.1.0
description: "{{description}}"
authors: ["{{author}}"]
license: Apache-2.0
keywords: []

inputs:
  parameters: {}
  required: []

outputs:
  type: text

resources:
  maxTokens: 4096
  timeoutSeconds: 30
  maxRetries: 2

lifecycle: {}

eval:
  framework: pass@k
  k: 1
  minPassRate: 70
  cases: []
`,

  advanced: `name: {{name}}
version: 0.1.0
description: "{{description}}"
authors: ["{{author}}"]
license: Apache-2.0
keywords: []
agentPlatforms: ["claude-code"]

inputs:
  parameters:
    input_text:
      type: string
      description: "The text to process"
    model:
      type: string
      description: "LLM model to use"
      default: "claude-sonnet-4-6"
      enumValues: ["claude-sonnet-4-6", "claude-opus-4-7", "claude-haiku-4-5"]
  required: ["input_text"]

outputs:
  type: json
  jsonSchema: >
    {"type": "object", "properties": {"result": {"type": "string"}, "confidence": {"type": "number"}}}

resources:
  maxTokens: 8192
  timeoutSeconds: 60
  maxRetries: 3

dependencies: {}

lifecycle:
  healthCheck: "echo 'ok'"

eval:
  framework: pass@k
  k: 1
  minPassRate: 80
  cases:
    - name: "basic_smoke"
      input: {"input_text": "Hello"}
      expectedOutput: {"result": "..."}
      validator: "contains"
`,

  pipeline: `name: {{name}}
version: 0.1.0
description: "{{description}} — DAG pipeline"
authors: ["{{author}}"]

# Pipeline definition (not a single skill)
kind: pipeline
nodes:
  - id: validate
    skill: input-validator
    onFailure: STOP
  - id: process
    skill: text-processor
    dependsOn: [validate]
    parameterMapping:
      text: "$.validate.output.validatedText"
    onFailure: RETRY
    retryCount: 2
  - id: format
    skill: output-formatter
    dependsOn: [process]
    parameterMapping:
      raw: "$.process.output.result"
    onFailure: CONTINUE

edges:
  - source: validate
    target: process
    condition: "$.validate.output.valid == true"
  - source: process
    target: format
`
};

export async function initCommand() {
  console.log('skillhub init — scaffold a new skill\n');

  const name = await input({ message: 'Skill name (lowercase, dashes):', validate: (v: string) =>
    /^[a-z0-9][a-z0-9._-]+$/.test(v) || 'Invalid name — use lowercase, digits, dots, dashes'
  });

  const template = await select({
    message: 'Choose a template:',
    choices: [
      { name: 'Basic — minimal skill with defaults', value: 'basic' },
      { name: 'Advanced — structured with JSON Schema, eval cases, dependencies', value: 'advanced' },
      { name: 'Pipeline — DAG workflow chaining multiple skills', value: 'pipeline' },
    ]
  });

  const description = await input({ message: 'Short description:', default: 'A skill for...' });
  const author = await input({ message: 'Author:', default: process.env.USER || 'developer' });

  const confirmed = await confirm({ message: `Create skill "${name}" with ${template} template?`, default: true });
  if (!confirmed) {
    console.log('Cancelled.');
    return;
  }

  const dir = path.join(process.cwd(), name);
  fs.mkdirSync(dir, { recursive: true });

  let content = TEMPLATES[template]
    .replace(/\{\{name\}\}/g, name)
    .replace(/\{\{description\}\}/g, description)
    .replace(/\{\{author\}\}/g, author);

  fs.writeFileSync(path.join(dir, 'skill.yaml'), content);
  fs.writeFileSync(path.join(dir, 'README.md'), `# ${name}\n\n${description}\n`);
  fs.writeFileSync(path.join(dir, '.gitignore'), '*.log\nnode_modules/\n');

  console.log(`\nCreated ${name}/`);
  console.log(`  skill.yaml  (${template} template)`);
  console.log(`  README.md`);
  console.log(`  .gitignore`);
  console.log(`\nNext: cd ${name} && edit skill.yaml to define your inputs/outputs`);
}
