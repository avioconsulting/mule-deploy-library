# Overview

This Maven plugin uses the `mule-deploy-library` DSL and offers a goal/mojo that will "load" a DSL Groovy file of your choosing and then perform a deployment.

# Configuring/installing

The plugin can either be dropped in a Mule project's POM or executed without a POM. See README.md in the root directory for recommendations on which method to use.

## In POM approach

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
                            <!-- Can be supplied instead via -Danypoint.username=bob on the command line -->
                            <anypointUsername>bob</anypointUsername>
                            <!-- Can be supplied instead via -Danypoint.password=asecret on the command line -->
                            <anypointPassword>asecret</anypointPassword>
                            <!-- Optional. If omitted, it will use the default org or business group for the user
                             If you wish to supply this explicitly, you can either do it here like this or
                             via -Danypoint.org.name=OrgNameOrBizGroup -->
                            <anypointOrganizationName>OrgNameOrBizGroup</anypointOrganizationName>
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

## Without POM approach

1. Create a DSL file
2. Invoke from the command line as described below

## DSL

Now you'll want to create your DSL file. You can use the GDSL file from this project to help IntelliJ with syntax completion.

See `deploySpec_minimum.groovy` as an example. This file is intended to be in source control. Obviously do not commit secrets to source control.



## User/service account access

You'll need a service account user/password to run this. See the README.md file inside the library directory for what permissions the user needs.

# Running



TBD: Describe params stuff

For the project case, the plugin will run during whichever phase you bind it to (e.g. deploy). You'll probably want to supply `-Danypoint.username=bob` and `-Danypoint.password=asecret` on the command line using credentials from your CI/CD system.

For standalone cases, do something like this:

```sh
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:1.0.0:muleDeploy -Dgroovy.file=deploySpec_minimum.groovy -Denvironment=DEV -Danypoint.username=bob -Danypoint.password=asecret
```

## Dry runs

TBD. Describe how this works.
