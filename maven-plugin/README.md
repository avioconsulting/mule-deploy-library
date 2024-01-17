# Overview

This Maven plugin uses the `mule-deploy-library` DSL and offers a goal/mojo that will "load" a DSL Groovy file of your choosing and then perform a deployment.

# Configuring/installing

The plugin has 2 goals (validate and deploy). The first goal should definitely go into your POM. The second goal can either be executed via a Mule project's POM or executed without a POM. See README.md in the root directory for recommendations on which method to use.

## POM Changes

In your project's POM, add the following snippet -

**NOTE:** Get the latest release version from [Releases](https://github.com/avioconsulting/mule-deploy-library/releases).

```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.avioconsulting.mule</groupId>
                <artifactId>mule-deploy-maven-plugin</artifactId>
                <version>${mule-deploy-maven-plugin.version}</version>
                <configuration>
                    <!-- this is optional, muleDeploy.groovy is the default if you do not specify -->
                    <groovyFile>muleDeploy.groovy</groovyFile>
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

## Versioning

This framework supports "v2" of API definitions in 3 different ways.

1. Entirely separate app for "v2". In this case, the deploy tool doesn't even need to know about it because it's a new application in every way.
2. 2 different branches in the same Design Center project. Each branch is fed from a different `api` directory in your app and each branch is pushed to a different "major version" of the same Exchange asset.
3. 2 different Design Center projects. Each project is fed from a different `api` directory in your app and each project is pushed to a different "major version" of the same Exchange asset.

Option 2 is probably the preferred route for most use cases. Both options 2 and 3 will require that you specify which `sourceDirectory` to use and supply unique `autoDiscoveryPropertyName` values. See the 2 multipleVersions example files in `library/examples` to see how to do this.

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
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:1.0.0:deploy -DmuleDeploy.env=DEV -Danypoint.username=bob -Danypoint.password=asecret -DmuleDeploy.cryptoKey=hello -DmuleDeploy.autoDiscClientId=theId -DmuleDeploy.autoDiscClientSecret=theSecret -DmuleDeploy.appArtifact=target/mule4testapp-1.0.0-mule-application.jar
```

In both cases, you can configure properties like `anypoint.username` in the `settings.xml` file's `<properties>` section and omit them from the command line. 

## Dry runs

There are 3 'dry run' options.

1. `Run` - does a real deployment
2. `OfflineValidate` - Just ensures your DSL file can be processed correctly. Does no networking traffic with anypoint.mulesoft.com
3. `OnlineValidate` - Will do GETs and read-only activity with anypoint.mulesoft.com and tell you what it would do. 

The `validate` goal described above is hard wired to run `OfflineValidate` on your DSL file using a standard set of placeholder properties (see `ValidateMojo.placeholderProperties`).

If you wish to use `OnlineValidate` with the deploy goal, you can set `-Ddeploy.mode=OnlineValidate` on your command line.
