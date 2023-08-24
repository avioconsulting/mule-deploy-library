muleDeploy {
    apiSpecification {
        name 'app display tittle'
    }
    cloudHubV2Application {
        environment "Name of the environment as it is in anypoint (DEV/PROD/DESIGN)"
        applicationName 'should be the same app name of the app already deployed in Anypoint Exchange'
        appVersion 'should be the same app version of the app already deployed in Anypoint Exchange'
        workerSpecs {
            target 'CloudHub V2 region name to deploy the app to'
            muleVersion 'mule runtime version'
            replicas 'number of replicas to deploy. Maximun 8'
            vCores 'type of core to use'
            updateStrategy 'recreate/rolling'
        }
        businessGroup 'businnes group name as it is in Anypoint'
        businessGroupId 'businnes group Id as it is in Anypoint'
    }
}