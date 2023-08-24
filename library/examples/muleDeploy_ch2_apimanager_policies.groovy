muleDeploy {
    version '1.0'

    apiSpecification {
        name 'TDC Test API 3'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubV2Application {
        environment params.env
        applicationName 'the-app'
        // appVersion is also the version of artifact in Exchange for API Manager/Policies logic
        appVersion '1.0.0'
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        workerSpecs {
            target 'Cloudhub-US-West-2'
            muleVersion '4.4.0'
        }
    }
}