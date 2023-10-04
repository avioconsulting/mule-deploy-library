muleDeploy {
    cloudHubV2Application {
        environment params.env
        applicationName '${project.name}'
        appVersion '${project.version}'
        workerSpecs {
            muleVersion '4.4.0'
            target 'us-west-2'
            replicas '1'
            vCores 'vCore1GB'
            updateStrategy 'recreate'
        }
        environment 'DEV'
        businessGroup 'AVIO Sandbox'
        businessGroupId '${project.groupId}'
    }
}