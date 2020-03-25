# Overview

There are 3 ways to use this:

1. Via a Maven plugin
2. Via a CLI
3. Consume the library directly with code.

Both approaches 1 and 2 lean on using a Groovy DSL to supply your deployment specs. #3 leaves the choice up to you (either building request objects in Java or Groovy code or using the DSL).

# Maven plugin

The Maven plugin can be used 2 ways (in a project POM or standalone)

## Maven plugin in a project POM

### Use when
* You have JDK and Maven installed on the agent(s) your CI/CD system performs releases on

### Do NOT use when
* Strict config management practices are required by the client and deployment stuff cannot/should not be controlled by the Mule app's repo.
* The app's repo is not available during deployment time.

## Maven plugin standalone

### Use when
* You have JDK and Maven installed on the agent(s) your CI/CD system performs releases on
* Installing artifacts like a CLI on a build/release agent ahead of time is difficult because the agent is ephemeral.
* Strict config management practices are required by the client and deployment stuff cannot/should not be controlled by the Mule app's repo.

### Do NOT use when
* The config management piece above does not apply. Executing this way results in non-ideal command line executions.

# CLI

## Use when
* Maven is not available on the agents (and can't be)
* TODO: Right now we don't bundle a JDK with the CLI but we could

## Do NOT use when
* Maven is available (that approach makes "setting up" the agent easier)

# Further Info

See the respective README files in each of the 3 subprojects for more details.

