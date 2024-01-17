muleDeploy {
    cloudHubV2Application {
        environment params.env
        applicationName {
            baseAppName 'the-app'
        }
        appVersion '${project.version}'
        workerSpecs {
            muleVersion '4.4.0'
            target 'us-west-2'
            replicas '1'
            vCores 'vCore1GB'
            clustered true
            persistentObjectStore true
            updateStrategy 'recreate'
            lastMileSecurity true
        }
        environment 'DEV'
        businessGroup 'AVIO Sandbox'
        businessGroupId '${project.groupId}'
    }
}