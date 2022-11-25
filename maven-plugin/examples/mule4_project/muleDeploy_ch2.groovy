muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Design Center Test Project Cloudhub v2'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHub2Application {
        environment params.env
        // the Maven plugin will automatically set `params.appArtifact` to the path of the target JAR if it is used
        // in a project POM
        file params.appArtifact
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        cloudHubAppPrefix 'AVI'

        // Cloudhub v2 specific params
        uri 'https://anypoint.mulesoft.com'
        provider 'MC'
        target 'Cloudhub-US-West-2'
        muleVersion '4.4.0'
        server 'anypoint-exchange-v3'
        applicationName '${project.name}'
        replicas '1'
        vCores '0.1'
        businessGroup 'Mule Sandbox'
    }
}
