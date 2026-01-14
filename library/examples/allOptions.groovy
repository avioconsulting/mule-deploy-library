muleDeploy {
    // version of the tool
    version '1.0'

    // was able to keep publishing from 2 branches
    // Can we publish 3.0.1 to the v2 asset? NO, you cannot
    // Separate DC project to same exchange asset, works OK
    apiSpecification {
        name 'Design Center Project Name v1'
        designCenterBranchName 'v1'
        // everything else in this closure is optional
        exchangeAssetId 'the-asset-id'
        mainRamlFile 'stuff.raml'
        endpoint 'https://foobar'
        autoDiscoveryPropertyName 'the.id.for.1'
    }

    apiSpecification {
        name 'Design Center Project Name v2'
        // everything else in this closure is optional
        exchangeAssetId 'the-asset-id'
        mainRamlFile 'stuff-v2.raml'
        endpoint 'https://foobar'
        autoDiscoveryPropertyName 'the.id.for.2'
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
            // Sample using the client id enforcement with header properties
            assetId 'client-id-enforcement'
            version '1.2.3'
            config([
                    credentialsOriginHasHttpBasicAuthenticationHeader: 'customExpression',
                    clientIdExpression: '#[attributes.headers["client_id"]]',
                    clientSecretExpression: '#[attributes.headers["client_secret"]]'
            ])
        }
        azureAdJwtPolicy {
            azureAdTenantId 'abcd'
            expectedAudience 'https://aud'
        }
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
        clientEnforcementPolicyCustom {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
        DLBIPWhiteListPolicy {
            ipsToAllow ['192.168.1.1']
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
        applicationName {
            baseAppName 'the-app'
            prefix params.env
            suffix params.env
        }
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }

    cloudHubApplication {
        environment 'DEV'
        applicationName {
            baseAppName 'the-app'
            prefix params.env
            suffix params.env
        }
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
        // this is true by default so have to specify false to NOT set the property
        analyticsAgentEnabled false
        file 'path/to/file.jar'
        cryptoKey 'theKey'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        // optional from here on out
        appProperties([
                someProp: 'someValue'
        ])
        otherCloudHubProperties([
                some_ch_value_we_havent_covered_yet: true
        ])
    }

    cloudHubV2Application {
        environment params.env
        applicationName {
            baseAppName 'the-app'
            prefix params.env
            suffix params.env
        }
        appVersion '${project.version}'
        workerSpecs {
            target 'Cloudhub-US-West-2'
            muleVersion '4.4.0'
            persistentObjectStore false
            lastMileSecurity false
            clustered false
            updateStrategy UpdateStrategy.recreate
            replicasAcrossNodes false
            publicURL false
            replicaSize VCoresSize.vCore1GB
            workerCount 1
            forwardSslSession false
            disableAmLogForwarding true
        }
        businessGroupId '${project.groupId}'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        // optional from here on out
        appProperties([
            someProp: 'someValue'
        ])
        appSecureProperties([
            firstSecureProp: true,
            secondSecureProp: 'second'
        ])
        otherCloudHubProperties([
            some_ch_value_we_havent_covered_yet: true
        ])
    }
}
