muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Design Center Test Project Cloudhub v2'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubV2Application {
        environment params.env
        file params.appArtifact
        cryptoKey params.cryptoKey
        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }
        cloudHubAppPrefix 'AVI'

        // Cloudhub v2 specific params
        uri 'https://anypoint.mulesoft.com'
        muleVersion '4.4.0'
        username 'Anypoint_user'
        password 'Anypoint_pass'
        authToken '4bb5-88e8'
        connectedAppClientId 'f2ea2cb4'
        connectedAppClientSecret 'c600'
        connectedAppGrantType 'client_credentials'
        applicationName '${project.name}'
        scopeLoggingConfigurations {
            scopeLoggingConfiguration {
                scope 'com.pkg.warning'
                logLevel 'WARN'
            }
            scopeLoggingConfiguration {
                scope 'com.pkg.debug'
                logLevel 'DEBUG'
            }
        }
        target 'Cloudhub-US-West-2'
        provider 'MC'
        environment 'DEV'
        replicas '1'
        vCores '0.1'
        businessGroup 'AVIO Sandbox'
        businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
        deploymentTimeout '1000000'
        server 'anypoint-exchange-v3'
        skip 'false'
        skipDeploymentVerification 'false'
        deploymentSettings {
            lastMileSecurity 'false'
            updateStrategy 'recreate'
            clustered 'true'
            http {
                inbound {
                    publicUrl 'myapp.anypoint.com'
                    lastMileSecurity 'true'
                    forwardSslSession 'true'
                }
            }
            persistentObjectStore 'false'
            jvm {
                args '-XX:MaxMetaspaceSize=500m -XX:MaxRAMPercentage=60.0'
            }
            generateDefaultPublicUrl 'true'
        }
        integrations {
            services {
                objectStoreV2 {
                    enabled 'true'
                }
            }
        }
    }
}
