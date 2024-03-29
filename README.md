# Overview

There are 3 ways to use this:

1. Via a Maven plugin
2. Via a CLI
3. Consume the library directly with code.

Both approaches 1 and 2 lean on using a Groovy DSL to supply your deployment specs. #3 leaves the choice up to you (either building request objects in Java or Groovy code or using the DSL). Keep in mind that the "Groovy DSL file" that specifies actual deployment specs can live anywhere (build system artifacts, etc.). It just has to be present on the filesystem by the time the Maven plugin or the CLI (if you choose that route) runs.

ALL of these methods assume your CI/CD tool white lists secrets from output. If it does not, it's on YOU to deal with that. Jenkins and Azure DevOps should do this out of the box with no further configuration.

# Authentication
The tool supports two type of authentication methods -
1. Basic Anypoint Platform Credentials
2. Anypoint [Connected App Credentials](https://help.mulesoft.com/s/article/How-to-deploy-an-application-to-CloudHub-using-Connected-App-functionality)

# Distribution

Starting v1.2.0, The Maven Plugin and Core library is published to [Maven Central](https://repo1.maven.org/maven2/com/avioconsulting/mule/). To **use released versions** from maven central, you do not need any credential or pluginRepository declaration. Just add mule-deploy-maven-plugin configuration to your project and you are good to go.


## Snapshots
Starting v1.2.0, SNAPSHOTs are build from `master` branch and are published to [Sonatype Snapshot Repository](https://oss.sonatype.org/content/repositories/snapshots/com/avioconsulting/mule/). 

To use snapshot versions, you would need to add following in your pom.xml - 

```xml
<pluginRepositories>
    <!-- Your any existing plugin repositories can stay in the pom and add a new one shown below -->
    <pluginRepository>
        <id>oss.sonatype.org-snapshot</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```


# Maven plugin

NOTE: The Maven plugin has 2 goals (deploy and validate). Regardless of whether you use it to actually perform the deployment, it's highly recommended you use the validate goal to ensure your DSL file is correct during the build pipeline.

## Use when

* You have JDK and Maven installed on the agent(s) your CI/CD system performs releases on.
* Installing artifacts like a CLI on a build/release agent ahead of time is difficult because the agent is ephemeral.

## Do NOT use when

* Organization does not have and/or does not want Maven on their agents.

## Details

The Maven plugin's `validate` goal is best run by putting it in the POM explicitly. The `deploy` goal can be used 2 ways (in a project POM or without a POM). There is no hard and fast answer and it largely depends on what your organization is comfortable with.

If you have no strong preference, then stick with the "with POM" approach which looks like this.

```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.avioconsulting.mule</groupId>
                <artifactId>mule-deploy-maven-plugin</artifactId>
                <version>${mule-deploy-maven-plugin.version}</version>                
                <executions>
                    <execution>
                        <id>do_validation</id>
                        <!-- Needs to be at the package phase so we have an artifact to use -->
                        <phase>package</phase>
                        <goals>
                            <goal>validate</goal>
                        </goals>
                    </execution>                  
                    <execution>
                        <id>stuff</id>
                        <phase>deploy</phase>
                        <goals>
                            <goal>deploy</goal>
                        </goals>                       
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

```sh
mvn clean deploy -DmuleDeploy.env=DEV -Danypoint.username=bob -Danypoint.password=asecret -DmuleDeploy.cryptoKey=hello -DmuleDeploy.autoDiscClientId=theId -DmuleDeploy.autoDiscClientSecret=theSecret
```

To see all parameters, run help goal of the plugin:
```shell
mvn com.avioconsulting.mule:mule-deploy-maven-plugin:help -Ddetail=true
```

See [maven-plugin/README.md](./maven-plugin/README.md) for more information.

# CLI

## Use when

* Maven is not available on the agents (and can't or won't be installed)
* At the moment, the CLI does not have a bundled JDK, just bundled dependencies. This could be changed if need be though.

## Do NOT use when

* Maven is available (that approach makes "setting up" the agent easier)

# Further Info

See the respective README files in each of the 3 subprojects for more details.
