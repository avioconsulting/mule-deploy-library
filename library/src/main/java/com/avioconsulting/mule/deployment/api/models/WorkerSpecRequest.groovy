package com.avioconsulting.mule.deployment.api.models

import groovy.transform.ToString

@ToString
class WorkerSpecRequest {
    /**
     * E.g. 4.2.2. This parameter is optional. If you do not supply it, then the deployer will derive it
     * by looking at POM properties, the <app.runtime> property for Mule 4 projects and <mule.version> for
     * Mule 3 projects. The POM will be read from the JAR/ZIP of the app
     */
    String muleVersion

    /***
     * Enable Last-Mile security to forward HTTPS connections to be decrypted by this application.
     * This requires an SSL certificate to be included in the Mule ap. Defaults to false.
     */
    boolean lastMileSecurity

    /***
     * Use ObjectStore v2. Defaults to false.
     */
    boolean objectStoreV2

    /***
     * Enables clustering across two or more replicas of the application. Defaults to false.
     */
    boolean clustered

    /***
     * rolling: Maintains availability by updating replicas incrementally.
     * recreate: Terminates replicas before re-deployment. Defaults to rolling.
     */
    UpdateStrategy updateStrategy

    /***
     * Enforces the deployment of replicas across different nodes. Defaults to false.
     */
    boolean replicasAcrossNodes

    /***
     * Enables SSL forwarding during a session. Defaults to false.
     */
    boolean forwardSslSession

    /***
     * Disables forwarding applications logs to Anypoint Monitoring.. Defaults to true.
     */
    boolean disableAmLogForwarding

    /***
     * When this parameter is set to true, CloudHub 2.0 generates a public URL for the deployed application. Default to false
     */
    boolean generateDefaultPublicUrl

    /**
     * Specifies the number of cores to allocate for each application replica. The default value is 0.5 vCores
     * Valid only for RTF deployment, not for CloudHub 2.0
     */
    String cpuReserved

    /**
     * Specifies the amount of memory to allocate for each application replica. The default value is 700 MB
     * Valid only for RTF deployment, not for CloudHub 2.0
     */
    String memoryReserved

    /***
     * How big of a worker to use
     */
    VCoresSize replicaSize

    /***
     * How many workers, defaults to 1
     */
    int workerCount

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    String target
    /***
     * The publicUrl, this is defined when the user wishes to deploy an API with a public ingress endpoint available
     */
    String publicUrl
    /***
     * Overwriting the publicUrl, this is defined and publicUrl is null when a User wishes to overwrite their custom url for the API
     */
    String pathRewrite
    /***
     * The releaseChannel, this is either LTS or EDGE
     */
    String releaseChannel
    /***
     * The Java Version, this is either 8 or 17
     */
    String javaVersion
    /***
     * Tracing Enabled flag, this defaults to false
     */
    Boolean tracingEnabled
    /***
     * Standard request, see properties for parameter info.
     */
    WorkerSpecRequest(String target,
                      String muleVersion,
                      Boolean lastMileSecurity,
                      Boolean objectStoreV2,
                      Boolean clustered,
                      UpdateStrategy updateStrategy,
                      Boolean replicasAcrossNodes,
                      Boolean generateDefaultPublicUrl,
                      VCoresSize replicaSize,
                      Integer workerCount,
                      Boolean forwardSslSession,
                      Boolean disableAmLogForwarding,
                      Integer cpuReserved,
                      Integer memoryReserved,
                      String publicUrl,
                      String pathRewrite,
                      String releaseChannel,
                      String javaVersion,
                      Boolean tracingEnabled) {

        println "target: $target, muleVersion: $muleVersion, lastMileSecurity: $lastMileSecurity, objectStoreV2: $objectStoreV2, clustered: $clustered, updateStrategy: $updateStrategy, replicasAcrossNodes: $replicasAcrossNodes, generateDefaultPublicUrl: $generateDefaultPublicUrl, replicaSize: $replicaSize, workerCount: $workerCount, forwardSslSession: $forwardSslSession, disableAmLogForwarding: $disableAmLogForwarding, cpuReserved: ${cpuReserved}m, memoryReserved: ${memoryReserved}Mi, publicUrl: $publicUrl, pathRewrite: $pathRewrite, releaseChannel: $releaseChannel, javaVersion: $javaVersion, tracingEnabled: $tracingEnabled"

        this.target = target
        this.muleVersion = muleVersion ?: null
        this.lastMileSecurity = lastMileSecurity != null ? lastMileSecurity : false
        this.objectStoreV2 = objectStoreV2 != null ? objectStoreV2 : true
        this.clustered = clustered != null ? clustered : true
        this.updateStrategy = updateStrategy != null ? updateStrategy : UpdateStrategy.rolling
        this.replicasAcrossNodes = replicasAcrossNodes != null ? replicasAcrossNodes : true
        this.generateDefaultPublicUrl = generateDefaultPublicUrl != null ? generateDefaultPublicUrl : true
        this.forwardSslSession = forwardSslSession != null ? forwardSslSession : false
        this.disableAmLogForwarding = disableAmLogForwarding != null ? disableAmLogForwarding : false
        this.cpuReserved = cpuReserved != null ? "${cpuReserved}m" : "20m"
        this.memoryReserved = memoryReserved != null ? "${memoryReserved}Mi" : "700Mi"
        this.replicaSize = replicaSize != null ? replicaSize : VCoresSize.vCore1GB
        this.workerCount = workerCount != null ? workerCount : 1
        this.publicUrl = publicUrl ?: null
        this.pathRewrite = pathRewrite ?: null
        this.releaseChannel = releaseChannel ?: 'LTS'
        this.javaVersion = javaVersion ?: '8'
        this.tracingEnabled = tracingEnabled != null ? tracingEnabled : false
    }

}
