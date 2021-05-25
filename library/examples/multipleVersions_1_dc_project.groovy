def muleVersionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4'
]

muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        // assumes there are 2 Design Center projects, 1 for V1, 1 for V2
        name 'Design Center Project Name'
        // the "v1" RAML file
        mainRamlFile 'stuff.raml'
        // will use this branch for v1 and the other apiSpecification entry will use master (default)
        designCenterBranchName 'v1'
    }

    apiSpecification {
        name 'Design Center Project Name'
        // the "v2" RAML file
        mainRamlFile 'stuff-v2.raml'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubApplication {
        environment params.environment
        applicationName 'the-app'
        appVersion '1.2.3'
        // mule version for workerSpecs will be derived from POM
        file 'something.jar'
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        cloudHubAppPrefix 'AVI'
    }
}
