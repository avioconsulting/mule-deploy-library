---
name: mule-deploy
description: Mule deployment, Anypoint deployment, and deployment-command guidance for Mule apps using AVIO Mule Deploy. Use when users ask to deploy, release, promote, validate, prepare a deployment command, generate muleDeploy.groovy, configure mule-deploy-maven-plugin, use muleDeploy CLI, resolve Anypoint business groups/environments/targets, or deploy to CloudHub, CloudHub 2.0, Runtime Fabric, or on-prem Mule.
metadata:
  owner: avio
  repository: avioconsulting/mule-deploy-library
  plugin: com.avioconsulting.mule:mule-deploy-maven-plugin
---

# Mule Deploy Tooling

Configure, validate, and deploy a Mule application using either `mule-deploy-maven-plugin` or the `muleDeploy` CLI. Work in the consuming Mule application's root, not in this library, unless the user explicitly asks to change this library.

## Required Reference

Before generating or editing `muleDeploy.groovy`, read [references/dsl-reference.md](references/dsl-reference.md). It is the self-contained DSL catalog for consumers that do not have this repository's `library/examples` directory. Select exactly one deployment block and only the requested supported options; it is not a file to copy verbatim.

## Guardrails

- Never put passwords, connected-app secrets, crypto keys, client secrets, or secure application property values in `pom.xml`, `muleDeploy.groovy`, command history, or a response.
- Never inspect, print, or ask the user to paste a secret from an environment variable. Use placeholders in command templates and identify the CI or local secure-variable mechanism that supplies them.
- The current CLI prints the complete `-a` parameter map. Do not pass a secret through `-a` unless the execution environment demonstrably masks it and the user has approved that risk. Do not use the CLI for secret-bearing DSL values when that protection is unavailable.
- Do not deploy until the user has explicitly requested a deployment. Configuration and validation requests must stop after validation.
- Never silently change deployment targets, environments, business groups, application names, replica counts, worker sizes, Mule runtime versions, policies, API Manager settings, or Design Center settings.
- Make only reversible, deployment-specific repairs automatically. Examples: add a missing plugin declaration, correct a missing DSL path, add a confirmed non-secret parameter, or repair a confirmed DSL syntax error.
- A real deployment may be retried once only when the failure is clearly transient and no application update was accepted. Do not retry authentication, authorization, DSL, artifact, target, or application-start failures.

## Discover And Select A Runner

Inspect the consuming project's root `pom.xml`, parent POMs or active profiles, `mule-artifact.json`, existing `muleDeploy.groovy`, Maven wrapper files, and application artifact/version conventions.

Confirm the deployment type, environment, application name, target, business group or ID, runtime version, artifact source, authentication mode, and whether API lifecycle or policy features are required. Ask concise questions for unknown values; do not invent targets, credentials, IDs, runtime sizing, or feature selection.

Use the Maven plugin by default when Maven is available. It integrates with the application lifecycle and automatically exposes the attached Mule artifact plus Maven coordinates to `params`.

Use the CLI only when Maven cannot run on the deployment agent and a supported CLI distribution is already installed. The CLI provides its own launcher and dependencies but requires a compatible JDK. Verify the actual launcher and version with `muleDeploy --help` or `muleDeploy --version`; do not assume its installation path.

- Maven runner: use `com.avioconsulting.mule:mule-deploy-maven-plugin` in the consuming application's POM.
- CLI runner: do not add or modify a POM merely to run the CLI. It requires the DSL path as its final positional argument.
- Do not mix Maven parameter syntax (`-DmuleDeploy.name=value`) with CLI syntax (`-a name=value`).
- CloudHub 1 and on-premises deployments need a packaged Mule application JAR. CloudHub 2 and Runtime Fabric deploy an Exchange-published application by GAV/version; do not substitute a local JAR.

## Configure Maven

Search the effective POM configuration for `com.avioconsulting.mule:mule-deploy-maven-plugin` before adding anything.

- Preserve an existing plugin's version, configuration, executions, and profile placement; add only the requested missing behavior.
- If absent, add it under the consuming application's applicable `<build><plugins>` section, using the confirmed released version or the project's existing version-property convention.
- Do not add this repository's snapshot version unless the user explicitly requests it and has configured the snapshot repository.

Use validation at `package`. Add a `deploy` execution only when the user wants the Maven `deploy` lifecycle to perform the real application deployment:

```xml
<plugin>
    <groupId>com.avioconsulting.mule</groupId>
    <artifactId>mule-deploy-maven-plugin</artifactId>
    <version>${mule-deploy-maven-plugin.version}</version>
    <executions>
        <execution>
            <id>validate-mule-deployment</id>
            <phase>package</phase>
            <goals><goal>validate</goal></goals>
        </execution>
        <execution>
            <id>deploy-mule-application</id>
            <phase>deploy</phase>
            <goals><goal>deploy</goal></goals>
        </execution>
    </executions>
</plugin>
```

The default DSL path is project-root `muleDeploy.groovy`; use `<groovyFile>` or `-Dgroovy.file=...` only for a deliberate alternate location. Maven exposes `-DmuleDeploy.name=value` and `-Dmd.name=value` as `params.name`. It also injects `params.appArtifact`, `params.groupId`, `params.artifactId`, and `params.artifactVersion` when it finds an attached Mule artifact.

For a separately invoked deployment, package first and run the fully qualified goal with the confirmed released version. Include only non-secret properties in the displayed command:

```sh
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:<released-version>:deploy \
  -Dgroovy.file=muleDeploy.groovy \
  -DmuleDeploy.appArtifact=target/<application>-<version>-mule-application.jar
```

## MDX-Assisted Command Preparation

Use this path when the user requests a deployment command or deployment readiness guidance. Resolve non-secret Anypoint values with read-only MDX queries, then return a runnable PowerShell command template. Do not run a live deployment, call a write operation, or probe secure values unless the user explicitly requests execution.

### Resolve MDX CLI

1. Use the normal `mdx-cli` executable first.
2. If its wrapper reports that it is unavailable, resolve it in PowerShell:

```powershell
Get-Command mdx-cli -All
where.exe mdx-cli
cmd.exe /d /c "where mdx-cli & mdx-cli --version"
```

3. Prefer the explicitly resolved executable, such as `C:\Users\<user>\bin\mdx-cli.ps1`, when wrapper PATH sanitization prevents the normal invocation.
4. Record this as a tooling PATH discrepancy, not an Anypoint deployment blocker. Do not edit PATH, install MDX, or stop non-MDX configuration work because the wrapper cannot locate it.

### Read-Only Discovery

Use only MDX list/get/whoami operations. Resolve exact matches and ask a short clarification question when a business group, environment, target, application, or API instance name is ambiguous. Do not write discovered IDs into `pom.xml`, `muleDeploy.groovy`, or repository configuration.

| Required value | Read-only MDX operation |
| --- | --- |
| Business group name and ID | `org whoami`; use `org list` when organization IDs are already known. |
| Environment name and ID | `env list --org-id <business-group-id>`. |
| CloudHub 2 private/shared-space target | Use the supported Runtime Manager target/private-space list operation for the business group. Preserve the exact case-sensitive target **name**, not its ID. If the installed MDX version has no target operation, report that gap and ask for the approved target name rather than inventing a command. |
| Existing Runtime Manager application state | `runtime applications list --org-id <business-group-id> --env-id <environment-id>`. |
| API Manager instance state | `api-manager instances list --org-id <business-group-id> --env-id <environment-id>` with the DSL's Exchange asset or autodiscovery filters when known. |
| Applied API Manager policies | `api-manager policies list --org-id <business-group-id> --env-id <environment-id> --api-id <api-manager-instance-id>`. |

`runtime private-spaces list --org-id <business-group-id>` is a read-only private-space discovery operation. Inspect the installed MDX command help before selecting an available target query; do not guess an unsupported MDX subcommand.

### Readiness Rules

- A missing Runtime Manager application is not a blocker. A first deployment is expected to create it.
- A missing API Manager instance is not automatically a blocker. Inspect the DSL's API specification, autodiscovery, `enabledFeatures`, and policy configuration to determine whether the deployment provisions or requires it.
- Report API Manager policy state as a prerequisite only when the DSL or deployer behavior requires it.
- For CloudHub 2, the AVIO deployer resolves targets by exact, case-sensitive target name. Supply the name through the DSL property or its corresponding `muleDeploy` parameter; never substitute the MDX target ID.
- Authentication uses exactly one pair: `anypoint.connected-app.id` and `anypoint.connected-app.secret`, or `anypoint.username` and `anypoint.password`. `anypoint.org.name` is optional organization selection, not authentication.
- `clientEnforcementPolicyBasic` maps to API Manager's HTTP Basic Auth Header policy and defaults to version `1.2.1`. Include `-DmuleDeploy.basicAuthPolicyVersion=1.2.1` only when the DSL explicitly uses `params.basicAuthPolicyVersion`.

### Required Output

For an MDX-assisted command request that does not explicitly authorize execution, return only:

1. One PowerShell command in a code block, with MDX-resolved non-secret values filled in.
2. One brief note that the placeholders must be supplied through local secure environment variables or CI secret management.

Use the user's requested application release version for `-DmuleDeploy.appVersion`. Resolve the Maven plugin version from the consuming POM or user request; use `2.1.0` only when that is the requested/configured plugin release. Keep all secrets as placeholders and do not read their values.

```powershell
mvn 'com.avioconsulting.mule:mule-deploy-maven-plugin:<PLUGIN_VERSION>:deploy' `
  '-Ddeploy.mode=Run' `
  '-Danypoint.connected-app.id=<ANYPOINT_CONNECTED_APP_ID>' `
  '-Danypoint.connected-app.secret=<ANYPOINT_CONNECTED_APP_SECRET>' `
  '-Danypoint.org.name=<RESOLVED_ORGANIZATION_NAME>' `
  '-DmuleDeploy.env=<RESOLVED_ENVIRONMENT_NAME>' `
  '-DmuleDeploy.appVersion=<REQUESTED_RELEASE_VERSION>' `
  '-DmuleDeploy.cryptoKey=<SECURE_PROPERTIES_KEY>' `
  '-DmuleDeploy.businessGroupId=<RESOLVED_BUSINESS_GROUP_ID>' `
  '-DmuleDeploy.autoDiscClientId=<AUTODISCOVERY_CLIENT_ID>' `
  '-DmuleDeploy.autoDiscClientSecret=<AUTODISCOVERY_CLIENT_SECRET>' `
  '-DmuleDeploy.deploymentTarget=<RESOLVED_TARGET_NAME>' `
  '-DmuleDeploy.basicAuthPolicyVersion=1.2.1'
```

Omit optional properties that the actual DSL does not reference. In particular, omit auto-discovery, crypto, target, business-group, and policy-version properties when they are not used. Omit `basicAuthPolicyVersion` unless the DSL explicitly references `params.basicAuthPolicyVersion`.

## Configure The DSL

Preserve an existing DSL unless the requested changes require an edit. Every generated script starts with `muleDeploy {`, specifies `version '1.0'`, and contains exactly one of `cloudHubApplication`, `cloudHubV2Application`, `runtimeFabricApplication`, or `onPremApplication`.

Use `params.<name>` for environment-specific and secret-derived values. With the CLI, every `params` value must be supplied through `-a name=value`; it does not automatically inject artifact or Maven-coordinate values. Read the Required Reference before choosing any advanced DSL property, policy, API specification, worker setting, or feature.

Do not use `enabledFeatures { all }` by default: it can initiate Design Center, API Manager, and policy operations as well as app deployment. CloudHub 2 and Runtime Fabric do not support Design Center sync, API Manager definitions, or policy sync.

## Configure And Run The CLI

Locate the installed CLI distribution and run its launcher with `--help`. Appassembler distributions normally expose `muleDeploy` on Unix-like systems and `muleDeploy.bat` on Windows; use the actual launcher path.

The CLI requires:

- `muleDeploy.groovy` as the final positional argument.
- Exactly one credential mode: `-u`/`-p` for basic authentication or `-caid`/`-casec` for connected-app authentication.
- `-a name=value` for each `params.name` used by the DSL.
- `-o`/`--anypoint-org-name` when the default organization/business group is not correct.
- `-e`/`--design-center-environments` only when Design Center deployment must run outside its default `DEV` environment.

For CloudHub 1 and on-premises DSLs, package and verify the application JAR, then explicitly pass its path as the non-secret `-a appArtifact=...` value. For CloudHub 2 and Runtime Fabric, confirm the Exchange-published artifact before running the CLI.

Use CI secret injection and no-echo mechanisms for credential flags. The following command intentionally contains environment-variable references rather than secret values. Do not pass crypto keys, client secrets, or secure properties through `-a` unless their full values will be masked from CLI output.

```sh
./muleDeploy \
  --dry-run-mode OfflineValidate \
  --anypoint-connected-app-id "$ANYPOINT_CONNECTED_APP_ID" \
  --anypoint-connected-app-secret "$ANYPOINT_CONNECTED_APP_SECRET" \
  --arg env=DEV \
  --arg appArtifact=target/<application>-<version>-mule-application.jar \
  muleDeploy.groovy
```

`OfflineValidate` parses the DSL without Anypoint network activity, but the CLI still requires a credential argument group. `OnlineValidate` makes only read requests. The CLI defaults to `Run`; always state a dry-run mode explicitly until the user asks for a real deployment.

## Validate And Deploy

1. For Maven, use `./mvnw` or `./mvnw.cmd` when available, otherwise `mvn`. Run the phase that invokes the plugin's `validate` goal, normally `mvn clean package`. It validates `DEV`, `QA`, and `PRD` by default without Anypoint network calls.
2. For the CLI, run `muleDeploy --dry-run-mode OfflineValidate ... muleDeploy.groovy` with the required credential group and every required non-secret `-a` value.
3. For Maven deployments not bound to the lifecycle, invoke the explicit `validate` goal with the plugin's released version and required non-secret DSL properties.
4. Before a real deployment, run an online dry run where credentials and target access are available. Maven uses `-Ddeploy.mode=OnlineValidate`; CLI uses `--dry-run-mode OnlineValidate`.
5. Only after validation and explicit user approval, run the reviewed Maven lifecycle/explicit deploy command or replace the CLI dry-run mode with `Run`. Confirm environment, application, target, deployment type, artifact path or Exchange publication, and authentication source immediately before execution.
6. Report the runner, command form, result, deployed application/version, and intentionally excluded operations. Do not echo secret values from command output.

## Failure Investigation And Safe Healing

Capture relevant output, identify the earliest causal exception, inspect the affected POM, DSL, artifact, or CLI installation, then classify it before editing or rerunning.

| Evidence | Safe action |
| --- | --- |
| Maven plugin resolution failure | Add the confirmed released plugin declaration only if absent; otherwise verify the version and repository configuration. |
| CLI launcher missing or Java cannot start it | Locate the installed distribution or select Maven when available; do not create a replacement launcher. |
| CLI usage error or missing `<groovyFile>` | Correct the command structure and final DSL path, then rerun `OfflineValidate`. |
| `Property <name> was not found in your supplied params` | Add the confirmed non-secret CLI `-a name=value`, or stop for approved secret handling. |
| `Unable to process DSL`, parse error, or incomplete deployment request | Repair only the confirmed DSL error, then rerun offline validation. |
| Credential error, 401/403, missing business group/environment/target | Do not retry or guess values. Correct the authenticated setup or obtain the confirmed identifier/access. |
| Missing JAR or Maven artifact | Package the application, verify the actual JAR path, then correct the non-secret artifact reference. |
| Exchange artifact unavailable for CloudHub 2 or Runtime Fabric | Stop and require the application publish step; do not substitute a file deployment. |
| Invalid runtime, worker spec, or unsupported feature | Ask for approved platform configuration; do not invent a replacement. |
| Timeout, 429, 5xx, or connection failure before request acceptance | Verify no deployment was accepted, then retry the exact real-deployment command once. |
| App deployed but does not start | Do not redeploy automatically. Preserve deployment/log evidence and investigate application or runtime logs. |

After every automatic repair, rerun offline validation. After a failed real deployment that may have reached Anypoint, use `OnlineValidate` or inspect deployment state before another real deployment. Never loop indefinitely.

## Reference Facts

- Maven goals: `validate` and `deploy`.
- CLI launcher: `muleDeploy` or its installed Windows batch equivalent; the DSL path is required last.
- Default mode: `Run`; alternatives are `OfflineValidate` and `OnlineValidate`.
- Default DSL path: `muleDeploy.groovy`.
- Default Maven validation environments: `DEV,QA,PRD`.
- Default feature behavior may include all supported features when `enabledFeatures` is absent; configure an explicit list for deployment-only behavior.
