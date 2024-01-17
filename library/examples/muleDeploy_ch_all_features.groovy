muleDeploy {
    version '1.0'

    apiSpecification {
        name 'Test API'
        designCenterBranchName 'master'
        // everything else in this closure is optional
        exchangeAssetId 'test-api-1'
        mainRamlFile 'test-api.raml'
        endpoint 'https://foobar'
        autoDiscoveryPropertyName 'apiId1'
    }

    apiSpecification {
        name 'Test API 2'
        designCenterBranchName 'master'
        // everything else in this closure is optional
        exchangeAssetId 'test-api-2'
        mainRamlFile 'test-api.raml'
        endpoint 'https://foobar'
        autoDiscoveryPropertyName 'apiId2'
        sourceDirectory '/api2/'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubApplication {
        environment params.env
        applicationName {
            baseAppName 'the-app'
        }
        appVersion '1.0.0'
        workerSpecs {
            muleVersion params.env == 'DEV' ? '4.4.0' : '4.3.0'
        }
        file 'target/the-app-1.0.0-mule-application.jar'
        cryptoKey 'theKey'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
    }
}