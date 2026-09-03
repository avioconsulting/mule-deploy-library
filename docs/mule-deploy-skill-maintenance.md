# Mule Deploy Skill Maintenance

## Purpose

`.opencode/skills/mule-deploy/SKILL.md` is a project-local OpenCode skill for configuring and operating AVIO Mule Deploy from a consuming Mule application. It supports both the Maven plugin and the installed CLI, directs an agent to add the plugin only when the Maven runner is selected and it is absent, creates or updates `muleDeploy.groovy`, validates the DSL, performs an explicitly requested deployment, and diagnoses failures without exposing secrets or making unsafe deployment changes.

OpenCode discovers project skills recursively from `.opencode/skills/**/SKILL.md`. No `opencode.json` entry is required for this skill in this repository. To make it available in another workspace, place the `mule-deploy` directory under that workspace's `.opencode/skills/`, configure the workspace's `skills.paths`, or install it in the user's global OpenCode skills directory.

Restart OpenCode after changing the skill. Skills are loaded when OpenCode starts and are not hot-reloaded.

## Construction Steps

The skill was built from executable plugin behavior rather than documentation alone:

1. The Maven mojos were inspected to establish the goal names, property names, default DSL path, credential rules, dry-run modes, and how DSL parameters are populated.
2. The CLI command implementation, CLI README, and command tests were inspected to establish launcher behavior, required positional DSL input, credential flags, `-a` parameter mapping, CLI dry-run modes, and its parameter logging behavior.
3. The DSL context and deployment request models were inspected to establish the valid top-level deployment types, required deployment selection, feature defaults, and platform limitations.
4. The example scripts were inspected to establish current CloudHub 2, Runtime Fabric, CloudHub 1, on-premises, policy, API Manager, and multi-version syntax.
5. The repository READMEs were used to corroborate operational constraints: Maven versus CLI selection, JAR versus Exchange-based artifacts, permissions, and secret-handling expectations.
6. The resulting workflow was structured around runner selection, minimal configuration changes, offline validation, online validation, real deployment, and bounded error recovery.

## File Anatomy

`SKILL.md` contains these intentional sections. Its companion reference is `.opencode/skills/mule-deploy/references/dsl-reference.md`.

| Section | Responsibility | Change when |
| --- | --- | --- |
| YAML frontmatter | Registers the skill name, matching description, and ownership metadata. The folder name and `name` must stay `mule-deploy`. The description must front-load deployment terms and cover common user phrasing, including deploy, release, promote, command preparation, Anypoint targets, Maven plugin, CLI, and supported runtimes. | Skill naming, discovery keywords, ownership, or plugin coordinates change. |
| Guardrails | Sets secret, deployment authorization, and retry boundaries. | Safety policy or deployment approval rules change. |
| Required Discovery | Lists the facts an agent must collect before editing. | The plugin gains a deployment mode, a required input, or a new auth mechanism. |
| Select The Runner | Chooses Maven integration or an installed CLI without combining their argument models. | A runner is added, removed, or its selection criteria changes. |
| Configure The Maven Plugin | Defines idempotent POM insertion and lifecycle binding behavior. | Goal names, Maven coordinates, configuration fields, or recommended lifecycle changes. |
| MDX-Assisted Command Preparation | Resolves non-secret Anypoint deployment values with read-only MDX commands and returns an unexecuted PowerShell template with secret placeholders. | MDX command contracts, deployment-readiness rules, authentication properties, or output requirements change. |
| Configure The DSL | Directs the agent to preserve existing DSL and read the bundled reference before generation. | DSL workflow or reference location changes. |
| `references/dsl-reference.md` | Self-contained catalog of API, policy, feature, and all four deployment options for consumers who do not have this repository's examples. It is deliberately not a single deployable configuration. | Any DSL option, platform limitation, enum value, default, or secret-handling rule changes. |
| Configure And Run The CLI | Defines the launcher, flags, `-a` parameter behavior, and CLI validation/deployment sequence. | CLI packaging, option names, parameter behavior, or logging changes. |
| Validate And Deploy | Defines offline validation, online validation, and real deployment sequencing. | Dry-run modes, commands, or artifact preparation changes. |
| Failure Investigation And Safe Healing | Maps recognizable errors to bounded safe actions. | New predictable failures appear, recovery behavior changes, or additional evidence is available. |
| Reference Facts | Holds concise defaults that are useful during execution. | A plugin default changes. |

## Evidence Map

Maintain the skill against these source files. Update the skill in the same pull request whenever an observable behavior changes.

| Contract area | Primary source |
| --- | --- |
| Maven goals and deploy properties | `maven-plugin/src/main/java/com/avioconsulting/mule/maven/MuleDeployMojo.groovy` |
| Default DSL file, system-property-to-`params` mapping, Maven artifact properties | `maven-plugin/src/main/java/com/avioconsulting/mule/maven/BaseMojo.groovy` |
| Validation defaults and offline behavior | `maven-plugin/src/main/java/com/avioconsulting/mule/maven/ValidateMojo.groovy` |
| CLI flags, credential handling, modes, and DSL execution | `cli/src/main/java/com/avioconsulting/mule/cli/DeployerCommandLine.groovy` |
| CLI `-a`-to-`params` mapping | `cli/src/main/java/com/avioconsulting/mule/cli/ParamsWrapper.groovy` |
| CLI output behavior | `cli/src/main/java/com/avioconsulting/mule/cli/SimpleLogger.groovy` |
| CLI packaging and launcher name | `cli/pom.xml` |
| CLI installation and command examples | `cli/README.md` |
| MDX organization, environment, Runtime Manager, API Manager, and policy query syntax | Installed `mdx-cli --help` and the relevant subcommand help; use only read-only operations. |
| Valid deployment blocks and feature defaults | `library/src/main/java/com/avioconsulting/mule/deployment/dsl/MuleDeployContext.groovy` |
| DSL property requirements and request behavior | `library/src/main/java/com/avioconsulting/mule/deployment/dsl/` and `library/src/main/java/com/avioconsulting/mule/deployment/api/models/deployment/` |
| Minimal and advanced syntax | `library/examples/` |
| User-facing Maven usage and operational limitations | `README.md`, `maven-plugin/README.md`, and `library/README.md` |

## Extending The Skill

1. Implement the plugin feature and cover its behavior with unit or integration tests.
2. Add or update a minimal example under `library/examples/` for user-visible DSL syntax.
3. Update the user-facing Maven plugin or CLI README if the feature changes its installation, command, authentication, or operational behavior.
4. Update the appropriate `SKILL.md` section from the evidence map. Keep `references/dsl-reference.md` synchronized with the focused examples and source because consumers do not receive `library/examples`.
5. When MDX behavior changes, update the MDX-assisted workflow from the installed CLI's help output. Preserve the read-only boundary and PowerShell-template-only output for non-execution requests.
6. Add a failure-table entry only if the failure has a reliable signature and a safe, deterministic recovery. Otherwise instruct the agent to stop, preserve evidence, and ask for the missing decision.
7. Keep the templates minimal. Do not add optional policies, Design Center sync, API Manager work, secure properties, sizing, or credentials to a default template.
8. Verify the frontmatter remains valid: lowercase hyphenated `name`, a concise third-person `description` that includes trigger terms, and only string metadata values.
9. Review the diff for contradictory commands, outdated defaults, or instructions that would cause a real deployment without explicit user approval.
10. Restart OpenCode and confirm the skill is discoverable with a request containing a trigger such as `configure mule-deploy-maven-plugin` or `deploy a Mule app to Runtime Fabric`.

## Safety Decisions

The skill treats a deployment as consequential. It may repair local configuration only after evidence identifies the problem, but it does not fabricate platform decisions or attempt an unbounded redeploy loop.

- Secrets are always referenced by Maven property name, never literal values.
- Missing credentials, permissions, business-group IDs, target names, or sizing are questions, not values to infer.
- A real deployment requires an explicit user request after validation.
- CloudHub 2 and Runtime Fabric require an Exchange-published application; the skill must not fall back to a local JAR.
- Maven and CLI parameter models are not interchangeable: Maven uses `-DmuleDeploy.name=value`, while CLI uses `-a name=value` and does not inject Maven artifact or coordinate parameters.
- The current CLI logs its complete `-a` map. The skill must retain the restriction against secret-bearing CLI parameters unless CI masking is confirmed.
- MDX is read-only discovery for command preparation. It may resolve non-secret platform values but must not read secure environment variables, write repository configuration, invoke MDX writes, or execute a live deployment without explicit user approval.
- `enabledFeatures` is explicit for deployment-only flows because the library otherwise defaults to all available features.
- A retry is limited to one verified transient failure that did not create or update a deployment.

## Validation Checklist

Before merging a skill change:

- Confirm all Maven property names and defaults with the current Mojo source.
- Confirm all CLI option names, defaults, parameter mapping, and output behavior with the current CLI source.
- Confirm MDX command shapes with the installed CLI help. If a tool-wrapper PATH issue occurs, confirm the explicitly resolved executable before documenting its use.
- Confirm every displayed DSL block parses against the current DSL or is represented by a tested example.
- Confirm `references/dsl-reference.md` covers each supported top-level deployment block and labels mutually exclusive or unsupported combinations.
- Confirm every referenced file exists.
- Ensure no template contains a literal secret, real business-group ID, production target, or unapproved runtime sizing.
- Ensure a configuration-only request cannot reach a real deployment command.
- Ensure `OnlineValidate` is described as read-only and `OfflineValidate` as no-network validation.
- Ensure Maven and CLI command examples use their respective parameter conventions and CLI examples always select a dry-run mode explicitly.
- Ensure MDX-assisted, non-execution responses contain only a PowerShell command template with resolved non-secret values and a brief secure-placeholder note.
- Test skill discovery after restarting OpenCode.
