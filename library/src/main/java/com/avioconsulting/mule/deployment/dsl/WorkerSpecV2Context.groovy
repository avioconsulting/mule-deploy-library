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
    boolean generateDefaultPublicUrl = true
    boolean forwardSslSession = false
    boolean disableAmLogForwarding = true
    VCoresSize replicaSize = VCoresSize.vCore1GB
    int workerCount = 1
    int cpuReserved, memoryReserved
    String publicUrl
    String releaseChannel
    String javaVersion
    boolean pathRewrite
    boolean tracingEnabled

    WorkerSpecRequest createRequest() {
        new WorkerSpecRequest(this.target,
                              this.muleVersion,
                              this.lastMileSecurity,
                              this.persistentObjectStore,
                              this.clustered,
                              this.updateStrategy,
                              this.replicasAcrossNodes,
                              this.generateDefaultPublicUrl,
                              this.replicaSize,
                              this.workerCount,
                              this.forwardSslSession,
                              this.disableAmLogForwarding,
                              this.cpuReserved,
                              this.memoryReserved,
                              this.publicUrl,
                              this.pathRewrite,
                              this.releaseChannel,
                              this.javaVersion,
                              this.tracingEnabled)
    }

    @Override
    List<String> findOptionalProperties() {
        [
            'lastMileSecurity', 'persistentObjectStore', 'clustered',
            'updateStrategy', 'replicasAcrossNodes', 'publicURL', 'replicaSize',
            'workerCount', 'cpuReserved', 'memoryReserved', 'publicUrl', 'pathRewrite',
            'releaseChannel', 'javaVersion', 'tracingEnabled'
        ]
    }
}
