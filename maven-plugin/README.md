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
                <executions>
                    <execution>
                        <id>stuff</id>
                        <phase>deploy</phase>
                        <goals>
                            <goal>muleDeploy</goal>
                        </goals>
                        <configuration>
                            <groovyFile>deploySpec.groovy</groovyFile>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

Now follow the steps under 'DSL' below.

## Standalone/no Maven project approach

1. Create a DSL
2. Invoke from the command line like below:

```
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:1.0.0:muleDeploy -Dgroovy.file=deploySpec_minimum.groovy -Denvironment=DEV
```

## DSL

Now you'll want to create your DSL file. You can use the GDSL file from this project to help IntelliJ with syntax completion.

See `deploySpec_minimum.groovy` as an example.
