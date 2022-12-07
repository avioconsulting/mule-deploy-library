package com.avioconsulting.mule.deployment.api.models

import groovy.transform.ToString

@ToString
class CloudhubWorkerV2SpecRequest {
    /**
     * E.g. 4.2.2. This parameter is optional. If you do not supply it, then the deployer will derive it
     * by looking at POM properties, the <app.runtime> property for Mule 4 projects and <mule.version> for
     * Mule 3 projects. The POM will be read from the JAR/ZIP of the app
     */
    final String muleVersion

    /**
     * The "in-between" CloudHub version ID. https://anypoint.mulesoft.com/cloudhub/api/mule-versions will give you options
     */
    final String updateId

    /**
     * If you want to use the log4j2.xml file in your app. This requires support portal interaction to use if you
     * have not done it already
     * https://docs.mulesoft.com/runtime-manager/custom-log-appender
     * False by default
     */
    final boolean customLog4j2Enabled

    /***
     * If your app needs a public static IP (internal/VPC static IPs are not supported)
     * False by default
     */
    final boolean staticIpEnabled

    /***
     * Use object store v2 (true by default)
     */
    final boolean objectStoreV2Enabled

    /***
     * Only affects VM usage in your app, has nothing to do with Anypoint MQ. Defaults to false.
     */
    final boolean usePersistentQueues
    /***
     * How big of a worker to use
     */
    final WorkerTypes workerType
    /***
     * How many workers, defaults to 1
     */
    final int workerCount
    /**
     * by default will use what's configured in Runtime Manager if you don't supply one
     */
    final AwsRegions awsRegion

    /***
     * Standard request, see properties for parameter info.
     */
    CloudhubWorkerV2SpecRequest(String muleVersion = null,
                                boolean usePersistentQueues = false,
                                int workerCount = 1,
                                WorkerTypes workerType = WorkerTypes.Micro,
                                AwsRegions awsRegion = null,
                                String updateId = null,
                                boolean customLog4j2Enabled = false,
                                boolean staticIpEnabled = false,
                                boolean objectStoreV2Enabled = true) {
        this.muleVersion = muleVersion
        this.usePersistentQueues = usePersistentQueues
        this.workerType = workerType
        this.workerCount = workerCount
        this.awsRegion = awsRegion
        this.updateId = updateId
        this.customLog4j2Enabled = customLog4j2Enabled
        this.staticIpEnabled = staticIpEnabled
        this.objectStoreV2Enabled = objectStoreV2Enabled
    }

    CloudhubWorkerV2SpecRequest withNewMuleVersion(String newMuleVersion) {
        new CloudhubWorkerV2SpecRequest(newMuleVersion,
                                      usePersistentQueues,
                                      workerCount,
                                      workerType,
                                      awsRegion,
                                      updateId,
                                      customLog4j2Enabled,
                                      staticIpEnabled,
                                      objectStoreV2Enabled)
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
