muleDeploy {
    settings {
        username 'the_username'
        password 'the_password'
        // optional
        organizationName 'ACME Brick'
    }

    cloudHubApplication {
        environment 'DEV'
        applicationName 'the-app'
        workerSpecs {
            // only muleVersion is required
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
        // optional from here on out
        appProperties([
                someProp: 'someValue'
        ])
        otherCloudHubProperties([
                some_ch_value_we_havent_covered_yet: true
        ])
    }
}
