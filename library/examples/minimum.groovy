def muleVersionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4'
]

muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Design Center Test Project'
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
            muleVersion muleVersionsForEnvironments[params.environment]
            workerType WorkerTypes().micro
        }
        file 'something.jar'
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        cloudHubAppPrefix 'AVI'
    }
}
