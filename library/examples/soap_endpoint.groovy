def muleVersionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4'
]

muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Design Center Test Project'
        // This will automatically disable Design Denter sync since Design Center is for REST APIs
        // Deploy process will assume you have manually created a WSDL based Exchange asset
        // Same logic as REST part of this deployment process will be used to derive the Exchange
        // asset ID. for this example, it would be mule-deploy-design-center-test-project
        soapEndpointWithVersion 'v1'
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
        applicationName {
            baseAppName 'the-app'
        }
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
