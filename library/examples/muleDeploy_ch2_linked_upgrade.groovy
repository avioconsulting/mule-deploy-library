// Example CloudHub 2.0 deployment with linked upgrade from CloudHub v1
// This feature enables migration from CloudHub v1 to CloudHub v2 by linking the deployment
// to an existing CloudHub v1 application with the same name

muleDeploy {
    // version of the tool
    version '1.0'

    cloudHubV2Application {
        environment params.env
        applicationName {
            baseAppName 'my-app-name'
        }
        appVersion '1.0.0-SNAPSHOT'
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }

        // When the user is member of multiple organizations, businessGroupId must be provided
        // in order to have correct ID when the plugin is retrieving info of target, environment, etc
        businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
        
        workerSpecs {
            target 'Cloudhub-US-East-1'
            muleVersion '4.4.0'
        }
        
        // Enable linked upgrade from CloudHub v1 to CloudHub v2
        // When set to true, the deployment will include upgradeData linking to the CloudHub v1 app
        // The linkedCloudhubAppName will automatically be set to the normalized application name
        // Set to false (or omit) for normal deployments without linking
        enableLinkedUpgrade true
    }
}
