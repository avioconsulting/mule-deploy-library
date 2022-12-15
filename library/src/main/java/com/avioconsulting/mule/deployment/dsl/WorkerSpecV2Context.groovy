package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerV2SpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize

class WorkerSpecV2Context extends BaseContext {
    String muleVersion, target
    boolean lastMileSecurity = false
    boolean persistentObjectStore = false
    boolean clustered = false
    UpdateStrategy updateStrategy = UpdateStrategy.rolling
    boolean replicasAcrossNodes = false
    boolean publicURL = false
    VCoresSize replicaSize = VCoresSize.vCore1GB
    int workerCount = 1

    CloudhubWorkerV2SpecRequest createRequest() {
        new CloudhubWorkerV2SpecRequest(this.target,
                                        this.muleVersion,
                                        this.lastMileSecurity,
                                        this.persistentObjectStore,
                                        this.clustered,
                                        this.updateStrategy,
                                        this.replicasAcrossNodes,
                                        this.publicURL,
                                        this.replicaSize,
                                        this.workerCount)
    }

    @Override
    List<String> findOptionalProperties() {
        ['muleVersion', 'lastMileSecurity', 'persistentObjectStore', 'clustered', 'updateStrategy', 'replicasAcrossNodes', 'publicURL', 'replicaSize', 'workerCount']
    }
}
