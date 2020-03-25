# Overview

This CLI tool uses the `mule-deploy-library` DSL and offers a shell script/batch file that will "load" a DSL Groovy file of your choosing and then perform a deployment.

# Installing

Right now, the CLI is published as an `cli/target/appassembler` directory in [Jenkins](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/) that you can download via artifacts. The `appassembler` directory includes the shell scripts and ALL Java dependencies. Just grab the files using the `all files in zip` link and place in a directory of your choosing.

It's highly recommended that you at least use the Maven plugin for validation (see the Maven plugin README.md file)

# DSL

Now you'll want to create your DSL file. You can use the GDSL file from this project to help IntelliJ with syntax completion.

See `examples/mule4_project/muleDeploy.groovy` as an example. This file is intended to be in source control. Obviously do not commit secrets to source control.

Things like crypto keys, client secrets, and environments will need to use `params` so that the CI/CD system can choose what to use. The Maven plugin and DSL will take care of dropping in `-DmuleDeploy.someProp=someValue` such that `params.someProp` evaluates to `someValue`.

## User/service account access

You'll need a service account user/password to run this. See the README.md file inside the library directory for what permissions the user needs.

# Running

If you read the DSL section, you should have a DSL file that's ready to go.

You can get an idea of available options by running `./muleDeploy --help`:

```
Missing required parameter: <groovyFile>
Usage: deploy [-V] [-m=<dryRunMode>] [-o=<anypointOrganizationName>]
              -p=<anypointPassword> -u=<anypointUsername>
              [-a=<String=String>]...
              [-e=<environmentsToDoDesignCenterDeploymentOn>]... <groovyFile>
Will deploy using your Mule DSL
      <groovyFile>   The path to your DSL file
  -a, --arg=<String=String>
                     Other arguments to use for params in your DSL. e.g. -a
                       env=DEV will set params.env in your DSL
  -e, --design-center-environments=<environmentsToDoDesignCenterDeploymentOn>
                     Which environments to do the design center deployment
                       during. Default is DEV only
  -m, --dry-run-mode=<dryRunMode>
                     Choices are Run (normal run), OfflineValidate (checks your
                       DSL but is offline), or OnlineValidate (does GET
                       operations but no changes)
  -o, --anypoint-org-name=<anypointOrganizationName>
                     The org/business group to use. If you do not specify it,
                       the default for your user will be used
  -p, --anypoint-password=<anypointPassword>

  -u, --anypoint-username=<anypointUsername>

  -V, --version      print version info

```

Running the deployment looks something like this:

```sh
./muleDeploy -u brady -p foo -a env=DEV -a appArtifact=target/mule4testapp-1.0.0-mule-application.jar -a cryptoKey=foobar -a autoDiscClientId=theId -a autoDiscClientSecret=theSecret muleDeploy.groovy 
```

## Dry runs

There are 3 'dry run' options.

1. `Run` - does a real deployment
2. `OfflineValidate` - Just ensures your DSL file can be processed correctly. Does no networking traffic with anypoint.mulesoft.com
3. `OnlineValidate` - Will do GETs and read-only activity with anypoint.mulesoft.com and tell you what it would do. 

By default the CLI will use `Run` mode. To change the mode, set `-m OnlineValidate` on your command line.
