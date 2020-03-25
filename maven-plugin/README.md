# Overview

This Maven plugin uses the `mule-deploy-library` DSL and offers a goal/mojo that will "load" a DSL Groovy file of your choosing and then perform a deployment.

# Configuring/installing

The plugin has 2 goals (validate and deploy). The first goal should definitely go into your POM. The second goal can either be executed via a Mule project's POM or executed without a POM. See README.md in the root directory for recommendations on which method to use.

## POM Changes

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
                <configuration>
                    <!-- this is optional, deploySpec.groovy is te default if you do not specify -->
                    <groovyFile>deploySpec.groovy</groovyFile>
                </configuration>
                <executions>
                    <execution>
                        <id>do_validation</id>
                        <!-- Needs to be at the package phase so we have an artifact to use -->
                        <phase>package</phase>
                        <goals>
                            <goal>validate</goal>
                        </goals>
                    </execution>
                    <!-- This execution is optional, depending on whether you want to run the deployment with Maven or not -->
                    <execution>
                        <id>stuff</id>
                        <phase>deploy</phase>
                        <goals>
                            <goal>deploy</goal>
                        </goals>
                        <configuration>                            
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

## DSL

Now you'll want to create your DSL file. You can use the GDSL file from this project to help IntelliJ with syntax completion.

See `examples/mule4_project/muleDeploy.groovy` as an example. This file is intended to be in source control. Obviously do not commit secrets to source control.

Things like crypto keys, client secrets, and environments will need to use `params` so that the CI/CD system can choose what to use. The Maven plugin and DSL will take care of dropping in `-DmuleDeploy.someProp=someValue` such that `params.someProp` evaluates to `someValue`.

## User/service account access

You'll need a service account user/password to run this. See the README.md file inside the library directory for what permissions the user needs.

# Running

If you read the DSL section, you should have a DSL file that's ready to go.

If you chose to include the deploy execution, the plugin will run during whichever phase you bind it to (e.g. deploy). Your command line will look something like this:

```sh
mvn clean deploy -DmuleDeploy.env=DEV -Danypoint.username=bob -Danypoint.password=asecret -DmuleDeploy.cryptoKey=hello -DmuleDeploy.autoDiscClientId=theId -DmuleDeploy.autoDiscClientSecret=theSecret
```

If you do not want to execute the deployment with the project POM, then run something like this:

```sh
# -Dgroovy.file=muleDeploy.groovy is optional, goal will assume muleDeploy.groovy is the filename if not supplied
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:1.0.0:deploy -DmuleDeploy.env=DEV -Danypoint.username=bob -Danypoint.password=asecret -DmuleDeploy.cryptoKey=hello -DmuleDeploy.autoDiscClientId=theId -DmuleDeploy.autoDiscClientSecret=theSecret
```

In both cases, you can configure properties like `anypoint.username` in the `settings.xml` file's `<properties>` section and omit them from the command line. 

## Dry runs

TBD. Describe how this works.
