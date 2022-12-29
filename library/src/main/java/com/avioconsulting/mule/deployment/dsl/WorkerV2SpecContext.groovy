package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize

class WorkerV2SpecContext extends BaseContext {
    String muleVersion, target
    boolean lastMileSecurity = false
    boolean persistentObjectStore = false
    boolean clustered = false
    UpdateStrategy updateStrategy = UpdateStrategy.rolling
    boolean replicasAcrossNodes = false
    boolean publicURL = false
    VCoresSize replicaSize = VCoresSize.vCore1GB
    int workerCount = 1

    WorkerSpecRequest createRequest() {
        new WorkerSpecRequest(this.target,
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
