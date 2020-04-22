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
                    method HttpMethod().get
                    method HttpMethod().put
                    regex '.*foo'
                }
                path {
                    method HttpMethod().put
                    regex '.*bar'
                }
            }
            // by default, groupId is assumed to be your organization's group. You can customize it like this
            groupId 'stuff'
        }
        mulesoftPolicy {
            version '1.2.1'
        }
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    // this entire section is optional. if you omit it
    enabledFeatures {
        all
        // or you can do this and specify a list of what you want enabled
        designCenterSync
        apiManagerDefinitions
    }

    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }

    cloudHubApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        workerSpecs {
            muleVersion '4.2.2'
            usePersistentQueues true
            workerType WorkerTypes().small
            workerCount 1
            awsRegion AwsRegions().uswest1
            updateId 'abc'
            customLog4j2Enabled true
            staticIpEnabled true
            objectStoreV2Enabled false
        }
        analyticsAgentEnabled true
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