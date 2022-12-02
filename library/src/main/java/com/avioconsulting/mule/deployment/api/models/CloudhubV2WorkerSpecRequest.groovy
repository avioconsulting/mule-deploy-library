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

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    final String target

    /***
     * Use object store v2 (true by default)
     */
    final boolean objectStoreV2Enabled

    /***
     * How big of a worker to use (replica size)
     */
    final VCoresSize replicaSize

    /***
     * How many workers (replicas), defaults to 1
     */
    final int replicas

    /***
     *  Strategy used on deployment, default rolling
     */
    final UpdateStrategies updateStrategy

    /***
     *  Enables last mile security, default false
     */
    final boolean lastMileSecurity

    /***
     *  Configures the Mule application to use a persistent object store, default false
     */
    final boolean persistentObjectStore

    /***
     *  Enables clustering across two or more replicas of the application, default false
     */
    final boolean clustered

    /***
     *  Enforces the deployment of replicas across different nodes, default false
     */
    final boolean replicasAcrossNodes

    /***
     *  URL of the deployed application
     */
    final String publicURL

    /***
     *  Enables SSL forwarding during a session, default false
     */
    final boolean forwardSslSession

    /***
     * Standard request, see properties for parameter info.
     */
    CloudhubV2WorkerSpecRequest(String muleVersion = null,
                                int replicas = 1,
                                VCoresSize replicaSize = VCoresSize.vCore1GB,
                                String target = null,
                                boolean objectStoreV2Enabled = true,
                                UpdateStrategies updateStrategy = UpdateStrategies.rolling,
                                boolean lastMileSecurity = false,
                                boolean persistentObjectStore = false,
                                boolean clustered = false,
                                boolean replicasAcrossNodes = false,
                                String publicURL = null,
                                boolean forwardSslSession = false) {
        this.muleVersion = muleVersion
        this.replicas = replicas
        this.replicaSize = replicaSize
        this.target = target
        this.objectStoreV2Enabled = objectStoreV2Enabled
        this.updateStrategy = updateStrategy
        this.lastMileSecurity = lastMileSecurity
        this.persistentObjectStore = persistentObjectStore
        this.clustered = clustered
        this.replicasAcrossNodes = replicasAcrossNodes
        this.publicURL = publicURL
        this.forwardSslSession = forwardSslSession
    }

    CloudhubV2WorkerSpecRequest withNewMuleVersion(String newMuleVersion) {
        new CloudhubV2WorkerSpecRequest(newMuleVersion,
                                        replicas,
                                        replicaSize,
                                        target,
                                        objectStoreV2Enabled,
                                        updateStrategy,
                                        lastMileSecurity,
                                        persistentObjectStore,
                                        clustered,
                                        replicasAcrossNodes,
                                        publicURL,
                                        forwardSslSession)
    }
}
