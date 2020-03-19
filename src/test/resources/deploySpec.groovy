import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.WorkerTypes

muleDeploy {
    cloudHubApplication {
        environment'DEV'
        applicationName 'the-app'
        workerSpecs {
            muleVersion '4.2.2'
            usePersistentQueues true
            workerType WorkerTypes.Micro
            workerCount 1
            awsRegion AwsRegions.UsEast1
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
