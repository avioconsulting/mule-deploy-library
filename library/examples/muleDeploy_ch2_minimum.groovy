muleDeploy {
    // version of the tool
    version '1.0'

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubV2Application {
        environment params.env
        applicationName 'hello-world-test'
        appVersion '1.0.0-SNAPSHOT'
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        cloudHubAppPrefix 'AVI'

        // When the user is member of multiple organizations, businessGroupId must be provided
        // in order to have correct ID when the plugin is retrieving info of target, environment, etc
        businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
        workerSpecs {
            target 'Cloudhub-US-East-1'
        }
    }
}
