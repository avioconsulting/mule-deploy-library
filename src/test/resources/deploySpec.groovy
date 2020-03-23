muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Design Center Project Name'
        // everything else in this closure is optional
        exchangeAssetId 'the-asset-id'
        apiMajorVersion 'v1'
        mainRamlFile 'stuff.raml'
        endpoint 'https://foobar'
    }

    policies {
        policy {
            assetId 'some-asset-id'
            version '1.2.1'
            config([
                    hello: 123
            ])
            // paths is optional, will apply to entire API definition if omitted
            paths {
                path {
                    method GET
                    method PUT
                    regex '.*foo'
                }
                path {
                    method PUT
                    regex '.*bar'
                }
            }
            // by default, groupId is assumed to be Mulesoft's built-in policy group. Otherwise, you can do this
            groupId 'stuff'
            // OR use the same organization being used for deployment
            groupId ourGroupId
        }
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    // this entire section is optional. if you omit it
    enabledFeatures {
        All
        // or you can do this and specify a list of what you want enabled
        DesignCenterSync
        ApiManagerDefinitions
    }

    cloudHubApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        workerSpecs {
            // only muleVersion is required
            muleVersion '4.2.2'
            usePersistentQueues true
            workerType WorkerTypes.Micro
            workerCount 1
            awsRegion AwsRegions.UsWest1
        }
        file 'path/to/file.jar'
        cryptoKey 'theKey'
        autoDiscovery {
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
