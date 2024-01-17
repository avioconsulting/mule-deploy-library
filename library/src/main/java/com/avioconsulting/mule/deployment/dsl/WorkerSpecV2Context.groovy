package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
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
    boolean forwardSslSession = false
    boolean disableAmLogForwarding = true
    VCoresSize replicaSize = VCoresSize.vCore1GB
    int workerCount = 1
    int cpuReserved, memoryReserved

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
                              this.workerCount,
                              this.forwardSslSession,
                              this.disableAmLogForwarding,
                              this.cpuReserved,
                              this.memoryReserved)
    }

    @Override
    List<String> findOptionalProperties() {
        [
            'lastMileSecurity', 'persistentObjectStore', 'clustered',
            'updateStrategy', 'replicasAcrossNodes', 'publicURL', 'replicaSize',
            'workerCount', 'cpuReserved', 'memoryReserved'
        ]
    }
}
