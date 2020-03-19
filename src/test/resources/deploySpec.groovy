muleDeploy {
    cloudHubApplication {
        environment 'DEV'
        applicationName 'the-app'
        workerSpecs {
            muleVersion '4.2.2'
            usePersistentQueues true
            workerType 'Micro'
            workerCount 1
            awsRegion 'us-east-1'
        }
        file 'path/to/file.jar'
        cryptoKey 'theKey'
        anypointClientId 'the_client_id'
        anypointClientSecret 'the_client_secret'
        cloudHubAppPrefix 'AVI'
        appProperties([
                someProp: 'someValue'
        ])
    }
}
