def muleVersionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4'
]

muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        // assumes there are 2 Design Center projects, 1 for V1, 1 for V2
        name 'Design Center Project Name V1'
        // asset ID needs to be specified in both since we're using the same asset (mult. major versions)
        exchangeAssetId 'the-asset-id'
        // the "v1" RAML file
        mainRamlFile 'stuff.raml'
        // If you have multiple projects in use, you have to tell the tool which directory to sync from
        // this example assumes you've moved the "v1 RAML" from `src/main/resources/api` in your app source
        // to `src/main/resources/api_v1`.
        sourceDirectory '/api_v1'
        // Since multiple specs are included, have to tell the framework which property to set the API ID on
        // for auto discovery purposes. The default (auto-discovery.api-id) will no longer work since
        // your autodiscovery element will need to tie 2 different IDs to 2 different API main flows
        autoDiscoveryPropertyName 'auto.disc.v1'
    }

    apiSpecification {
        name 'Design Center Project Name V2'
        // asset ID needs to be specified in both since we're using the same asset (mult. major versions)
        exchangeAssetId 'the-asset-id'
        // the "v2" RAML file
        mainRamlFile 'stuff-v2.raml'
        // Since multiple specs are included, have to tell the framework which property to set the API ID on
        // for auto discovery purposes. The default (auto-discovery.api-id) will no longer work since
        // your autodiscovery element will need to tie 2 different IDs to 2 different API main flows
        autoDiscoveryPropertyName 'auto.disc.v2'
        // no sourceDirectory is necessary here IF you're using what's in `src/main/resources/api`
        // for your V2 RAMLs
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
    }
}
