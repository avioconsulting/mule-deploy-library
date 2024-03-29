muleDeploy {
    apiSpecification {
        name 'Mule Deploy Cloudhub v2 Test Project'
    }
    cloudHubV2Application {
        environment 'DEV'
        applicationName {
            baseAppName 'hello-world-test'
        }
        appVersion '1.0.0-SNAPSHOT'
        workerSpecs {
            target 'US West (Oregon)'
            muleVersion '4.4.0'
            replicas '1'
            vCores '0.1'
            updateStrategy 'recreate'
        }
        businessGroup 'AVIO Sandbox'
        businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
    }
}