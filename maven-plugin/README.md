# Overview

This Maven plugin uses the `mule-deploy-library` DSL and offers a goal/mojo that will "load" a DSL Groovy file of your choosing and then perform a deployment.

# Installing

The plugin can either be dropped in a project or executed standalone.

## Project approach

In your project's POM, add the following snippet. Get the latest version number from Nexus.

```xml
<project>
...
    <build>
        <plugins>
            <plugin>
                <groupId>com.avioconsulting.mule</groupId>
                <artifactId>mule-deploy-maven-plugin</artifactId>
                <version>1.0.0</version>
            </plugin>
        </plugins>
    </build>
...
</project>
```

## Standalone approach

TBD

## DSL

TBD
