muleDeploy {
    settings {
        username 'the_username'
        password 'the_password'
        org_id ''
    }

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
        autodiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        cloudHubAppPrefix 'AVI'
        appProperties([
                someProp: 'someValue'
        ])
    }
}
