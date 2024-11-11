import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy

muleDeploy {
    // version of the tool
    version '1.0'

    apiSpecification {
        name 'Mule Deploy Cloudhub v2 Test Project'
    }

    policies {
        clientEnforcementPolicyBasic {
            // version is optional (will use version in this library by default)
            version '1.2.1'
            // can supply paths just like above if necessary
        }
    }

    cloudHubV2Application {
        businessGroupId params.groupId
        environment params.env
        environmentProperty 'env'
        cryptoKey params.cryptoKey
        cryptoKeyProperty 'crypto.key'
        appVersion params.appVersion

        applicationName {
            baseAppName params.artifactId
            prefix 'avio'
            suffix params.env
        }

        workerSpecs {
            target params.target

            muleVersion '4.6'
            releaseChannel 'LTS'
            javaVersion '8'

            workerCount 1
            replicaSize VCoresSize.vCore1GB
            replicasAcrossNodes true
            clustered true
            updateStrategy UpdateStrategy.rolling

            if(params.env.toLowerCase() != 'prod') {
                publicUrl "https://api-${params.env.toLowerCase()}.avio.dev/${params.artifactId}"
            } else {
                publicUrl "https://api.avio.dev/${params.artifactId}"
            }
            generateDefaultPublicUrl true
            pathRewrite null
            lastMileSecurity false
            forwardSslSession false

            objectStoreV2 true
            disableAmLogForwarding false
            tracingEnabled false
        }

        autoDiscovery {
            clientId params.autoDiscClientId
            clientSecret params.autoDiscClientSecret
        }

        appProperties {

        }
        appSecureProperties {

        }
    }
}
