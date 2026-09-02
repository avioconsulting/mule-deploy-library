# Mule Deploy DSL Reference

Use this reference before generating or editing `muleDeploy.groovy`. It is self-contained so it works in consuming applications and standalone CLI installations that do not contain this repository's `library/examples` directory.

This is a generation catalog, not a deployable file: select one deployment block, select only the supported optional sections, and omit every option not explicitly requested.

`params.<name>` is supplied by Maven as `-DmuleDeploy.<name>=value` (or `-Dmd.<name>=value`) and by the CLI as `-a <name>=value`. Maven also supplies the attached Mule artifact and Maven coordinates; the CLI does not. Do not supply secret `params` values to the CLI unless its output is proven to mask its complete `-a` parameter map.

## API Specifications And Policies

Use these sections only with CloudHub 1 or on-premises deployments. CloudHub 2 and Runtime Fabric do not support Design Center sync, API Manager definitions, or policy sync. Select an explicit `enabledFeatures` list rather than `all` when deployment must not manage the API lifecycle or policies.

```groovy
// Place these sections inside muleDeploy { ... } only when required.

apiSpecification {
    name 'confirmed Design Center project name'
    exchangeAssetId 'confirmed-exchange-asset-id'       // Optional; derived from name when omitted.
    mainRamlFile 'api.raml'                             // Optional; inferred from the API root when omitted.
    endpoint 'https://api.example.com'                  // Optional API Manager endpoint.
    autoDiscoveryPropertyName 'auto-discovery.api-id'   // Optional; this is the default when omitted.
    designCenterBranchName 'main'                       // Optional; the default is master.
    sourceDirectory 'api'                               // Optional; the default is the API directory in the application.
}

// SOAP alternative: do not combine soapEndpointWithVersion with mainRamlFile,
// designCenterBranchName, or sourceDirectory.
apiSpecification {
    name 'confirmed SOAP API name'
    exchangeAssetId 'confirmed-exchange-asset-id'
    endpoint 'https://soap.example.com'
    soapEndpointWithVersion 'v1'
}

policies {
    // Generic custom policy. Omit paths to apply the policy to the whole API.
    policy {
        assetId 'confirmed-custom-policy-asset-id'
        version 'confirmed-policy-version'
        groupId 'confirmed-policy-group-id' // Optional; defaults to the organization group for generic policies.
        config([
            setting: 'confirmed-value'
        ])
        paths {
            path {
                method HttpMethod().get
                method HttpMethod().put
                regex '.*/resource/.*'
            }
        }
    }

    // MuleSoft-managed custom policy. Group ID defaults to MuleSoft's group.
    mulesoftPolicy {
        assetId 'client-id-enforcement'
        version 'confirmed-policy-version'
        config([
            credentialsOriginHasHttpBasicAuthenticationHeader: 'customExpression',
            clientIdExpression: '#[attributes.headers["client_id"]]',
            clientSecretExpression: '#[attributes.headers["client_secret"]]'
        ])
    }

    clientEnforcementPolicyBasic {
        version 'confirmed-policy-version' // Optional; library default applies when omitted.
        paths {
            path {
                method HttpMethod().post
                regex '.*/orders'
            }
        }
    }

    jwtPolicy {
        jwksUrl 'https://issuer.example.com/.well-known/jwks.json'
        expectedAudience 'confirmed-audience'
        expectedIssuer 'https://issuer.example.com/'
        clientIdExpression '#[attributes.headers["client_id"]]' // Optional.
        jwksCachingTtlInMinutes 60                               // Optional.
        validateClaim 'scope', '#[authentication.properties.scope] contains "read"'
        // skipClientIdEnforcement() // Optional; call only when explicitly approved.
    }

    azureAdJwtPolicy {
        azureAdTenantId 'confirmed-azure-ad-tenant-id'
        expectedAudience 'confirmed-audience'
        jwksCachingTtlInMinutes 60 // Optional.
        requireRole 'confirmed-role'
        validateClaim 'scp', '#[authentication.properties.scp] contains "read"'
        // skipClientIdEnforcement() // Optional; call only when explicitly approved.
    }

    DLBIPWhiteListPolicy {
        version 'confirmed-policy-version' // Optional.
        ipToAllow '192.0.2.10'
        ipToAllow '192.0.2.11'
        // dwListIndexToUse 1 // Optional; use only when confirmed.
    }
}

enabledFeatures {
    appDeployment
    designCenterSync
    apiManagerDefinitions
    policySync
}

// `all` is valid only by itself, but is intentionally not a deployment default:
// enabledFeatures { all }
```

## CloudHub 1 Catalog

CloudHub 1 uploads a Mule application JAR. With Maven, use `file params.appArtifact`; with the CLI, pass that exact JAR path through the non-secret `-a appArtifact=...` parameter.

```groovy
muleDeploy {
    version '1.0'

    // Optional: API specifications, policies, and enabledFeatures from the catalog above.
    cloudHubApplication {
        environment params.env
        applicationName {
            baseAppName 'confirmed-application-name'
            prefix params.appNamePrefix // Optional; omit rather than pass an empty value.
            suffix 'api'                // Optional.
        }
        appVersion params.appVersion    // Optional for CloudHub 1.
        file params.appArtifact
        cryptoKey params.cryptoKey      // Optional; do not store the value in this file.
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        workerSpecs {
            muleVersion '4.6.0' // Optional; use the approved runtime.
            usePersistentQueues true
            workerType WorkerTypes().small
            workerCount 1
            awsRegion AwsRegions().uswest1
            updateId 'confirmed-update-id' // Optional.
            customLog4j2Enabled true
            staticIpEnabled true
            objectStoreV2Enabled true
        }
        analyticsAgentEnabled false // Defaults to true.
        appProperties([
            nonSecretProperty: 'confirmed-value'
        ])
        otherCloudHubProperties([
            confirmedPlatformProperty: true
        ])
    }
}
```

## CloudHub 2 Catalog

CloudHub 2 deploys the application by its Exchange coordinates and version; it does not upload a local JAR. Do not include API specifications, policies, Design Center sync, API Manager definitions, or policy sync.

```groovy
muleDeploy {
    version '1.0'

    cloudHubV2Application {
        environment params.env
        applicationName {
            baseAppName 'confirmed-application-name'
            prefix params.appNamePrefix // Optional.
            suffix 'api'                // Optional.
        }
        appVersion params.appVersion
        businessGroupId params.businessGroupId
        cryptoKey params.cryptoKey // Optional; do not store the value in this file.
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        workerSpecs {
            target 'confirmed-cloudhub-2-target'
            muleVersion '4.6.0'
            releaseChannel 'LTS'
            javaVersion '17'
            workerCount 1
            replicaSize VCoresSize.vCore1GB
            cpuReserved 500
            memoryReserved 1024
            replicasAcrossNodes false
            clustered false
            updateStrategy UpdateStrategy.recreate
            publicUrl 'api.example.com'
            generateDefaultPublicUrl false
            pathRewrite '/api'
            lastMileSecurity false
            forwardSslSession false
            objectStoreV2 true
            disableAmLogForwarding false
            tracingEnabled true
        }
        appProperties([
            nonSecretProperty: 'confirmed-value'
        ])
        appSecureProperties([
            securePropertyName: params.securePropertyValue
        ])
        otherCloudHubProperties([
            confirmedPlatformProperty: true
        ])
    }
}
```

## Runtime Fabric Catalog

Runtime Fabric also deploys the application by Exchange coordinates and version; it does not upload a local JAR. Do not include API specifications, policies, Design Center sync, API Manager definitions, or policy sync.

```groovy
muleDeploy {
    version '1.0'

    runtimeFabricApplication {
        environment params.env
        applicationName {
            baseAppName 'confirmed-application-name'
            prefix params.appNamePrefix // Optional.
            suffix 'api'                // Optional.
        }
        appVersion params.appVersion
        businessGroupId params.businessGroupId
        cryptoKey params.cryptoKey // Optional; do not store the value in this file.
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        workerSpecs {
            target 'confirmed-runtime-fabric-target'
            muleVersion '4.6.0'
            releaseChannel 'LTS'
            javaVersion '17'
            workerCount 1
            replicaSize VCoresSize.vCore1GB
            cpuReserved 500
            memoryReserved 1024
            replicasAcrossNodes false
            clustered false
            updateStrategy UpdateStrategy.recreate
            publicUrl 'api.example.com'
            generateDefaultPublicUrl false
            pathRewrite '/api'
            lastMileSecurity false
            forwardSslSession false
            objectStoreV2 true
            disableAmLogForwarding false
            tracingEnabled true
        }
        appProperties([
            nonSecretProperty: 'confirmed-value'
        ])
        appSecureProperties([
            securePropertyName: params.securePropertyValue
        ])
        otherCloudHubProperties([
            confirmedPlatformProperty: true
        ])
    }
}
```

## On-Premises Catalog

On-premises deployment uploads a Mule application JAR. With Maven, use `file params.appArtifact`; with the CLI, pass that exact JAR path through the non-secret `-a appArtifact=...` parameter.

```groovy
muleDeploy {
    version '1.0'

    // Optional: API specifications, policies, and enabledFeatures from the catalog above.
    onPremApplication {
        environment params.env
        applicationName {
            baseAppName 'confirmed-application-name'
            prefix params.appNamePrefix // Optional.
            suffix 'api'                // Optional.
        }
        appVersion params.appVersion // Optional for on-premises.
        file params.appArtifact
        targetServerOrClusterName 'confirmed-server-or-cluster'
        appProperties([
            nonSecretProperty: 'confirmed-value'
        ])
    }
}
```
