package com.avioconsulting.mule.deployment.api.models

import groovy.transform.ToString

@ToString
class CloudhubV2WorkerSpecRequest {
    /**
     * E.g. 4.2.2. This parameter is optional. If you do not supply it, then the deployer will derive it
     * by looking at POM properties, the <app.runtime> property for Mule 4 projects and <mule.version> for
     * Mule 3 projects. The POM will be read from the JAR/ZIP of the app
     */
    final String muleVersion

    /***
     * Enable Last-Mile security to forward HTTPS connections to be decrypted by this application.
     * This requires an SSL certificate to be included in the Mule ap. Defaults to false.
     */
    final boolean lastMileSecurity

    /***
     * Enables clustering across two or more replicas of the application. Defaults to false.
     */
    final boolean persistentObjectStore

    /***
     * Use persistent ObjectStore. Defaults to false.
     */
    final boolean clustered

    /***
     * rolling: Maintains availability by updating replicas incrementally.
     * recreate: Terminates replicas before re-deployment. Defaults to rolling.
     */
    final UpdateStrategy updateStrategy

    /***
     * Enforces the deployment of replicas across different nodes. Defaults to false.
     */
    final boolean replicasAcrossNodes

    /***
     * Enables SSL forwarding during a session. Defaults to false.
     */
    final boolean forwardSslSession

    /***
     * When this parameter is set to true, CloudHub 2.0 generates a public URL for the deployed application. Default to false
     */
    final boolean publicURL

    /***
     * How big of a worker to use
     */
    final VCoresSize replicaSize

    /***
     * How many workers, defaults to 1
     */
    final int workerCount

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    final String target

    /***
     * Standard request, see properties for parameter info.
     */
    CloudhubV2WorkerSpecRequest(String target,
                                String muleVersion = null,
                                boolean lastMileSecurity = false,
                                boolean persistentObjectStore = false,
                                boolean clustered = false,
                                UpdateStrategy updateStrategy = UpdateStrategy.rolling,
                                boolean replicasAcrossNodes = false,
                                boolean publicURL = false,
                                VCoresSize replicaSize = VCoresSize.vCore1GB,
                                int workerCount = 1) {
        this.muleVersion = muleVersion
        this.lastMileSecurity = lastMileSecurity
        this.persistentObjectStore = persistentObjectStore
        this.clustered = clustered
        this.updateStrategy = updateStrategy
        this.replicasAcrossNodes = replicasAcrossNodes
        this.publicURL = publicURL
        this.replicaSize = replicaSize
        this.workerCount = workerCount
        this.target = target
    }

    CloudhubV2WorkerSpecRequest withNewMuleVersion(String newMuleVersion) {
        new CloudhubV2WorkerSpecRequest(target,
                                        newMuleVersion,
                                        lastMileSecurity,
                                        persistentObjectStore,
                                        clustered,
                                        updateStrategy,
                                        replicasAcrossNodes,
                                        publicURL,
                                        replicaSize,
                                        workerCount)
    }

    Map<String, String> getVersionInfo() {
        def map = [
                version: muleVersion
        ]
        if (updateId) {
            map['updateId'] = updateId
        }
        map
    }
}
