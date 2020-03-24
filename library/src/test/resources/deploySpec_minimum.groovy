muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Design Center Project Name'
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
        workerSpecs {
            // only muleVersion is required
            muleVersion '4.2.2'
            workerType WorkerTypes.Micro
        }
        file 'path/to/file.jar'
        cryptoKey 'theKey'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        cloudHubAppPrefix 'AVI'
    }
}
