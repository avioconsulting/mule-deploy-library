muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Cloudhub v2 Test Project'
    }

    cloudHubV2Application {
        environment params.env
        applicationName 'hello-world-test'
        appVersion '1.0.0-SNAPSHOT'
        //cryptoKey params.cryptoKey -?
        // When the user is member of multiple organizations, businessGroupId must be provided
        // in order to have correct ID when the plugin is retrieving info of target, environment, etc
        //businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee' -?
        workerSpecs {
            target 'US West (Oregon)'
            muleVersion '4.4.0'
            replicas '1'
            vCores '0.1'
            updateStrategy 'recreate'
        }

        environment 'DEV'
        businessGroup 'AVIO Sandbox'
        // When the user is member of multiple organizations, businessGroupId must be provided
        // in order to have correct ID when the plugin is retrieving info like target, environment, etc
        businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
    }
}
