muleDeploy {
    version '1.0'

    enabledFeatures {
        appDeployment
    }

    apiSpecification {
        name 'Test API'
        // everything else in this closure is optional
        designCenterBranchName 'master'
        exchangeAssetId 'test-api-1'
        mainRamlFile 'test-api.raml'
        endpoint 'https://foobar'
        autoDiscoveryPropertyName 'apiId1'
    }

    apiSpecification {
        name 'Test API 2'
        // when using more than one apiSpecification, is required to specify the properties below to avoid duplication
        autoDiscoveryPropertyName 'apiId2'
        sourceDirectory '/api2/'
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