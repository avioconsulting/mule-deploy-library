package com.avioconsulting.jenkins.mule.impl.models

class CloudhubWorkerSpecRequest {
    /**
     * E.g. 4.2.2
     */
    final String muleVersion
    final boolean usePersistentQueues
    final WorkerTypes workerType
    final int workerCount
    /**
     * by default will use what's configured in Runtime Manager if you don't supply one
     */
    final AwsRegions awsRegion

    CloudhubWorkerSpecRequest(String muleVersion,
                              boolean usePersistentQueues,
                              int workerCount,
                              WorkerTypes workerType = WorkerTypes.Micro,
                              AwsRegions awsRegion = null) {
        this.muleVersion = muleVersion
        this.usePersistentQueues = usePersistentQueues
        this.workerType = workerType
        this.workerCount = workerCount
        this.awsRegion = awsRegion
    }
}
