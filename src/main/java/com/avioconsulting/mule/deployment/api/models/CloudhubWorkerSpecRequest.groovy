package com.avioconsulting.mule.deployment.api.models

class CloudhubWorkerSpecRequest {
    /**
     * E.g. 4.2.2
     */
    final String muleVersion
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
    CloudhubWorkerSpecRequest(String muleVersion,
                              boolean usePersistentQueues = false,
                              int workerCount = 1,
                              WorkerTypes workerType = WorkerTypes.Micro,
                              AwsRegions awsRegion = null) {
        this.muleVersion = muleVersion
        this.usePersistentQueues = usePersistentQueues
        this.workerType = workerType
        this.workerCount = workerCount
        this.awsRegion = awsRegion
    }
}
