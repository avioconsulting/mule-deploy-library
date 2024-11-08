package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize

class WorkerSpecV2Context extends BaseContext {
    String muleVersion
    String releaseChannel
    String javaVersion

    String target
    Integer workerCount
    VCoresSize replicaSize
    Integer cpuReserved
    Integer memoryReserved
    Boolean replicasAcrossNodes
    Boolean clustered
    UpdateStrategy updateStrategy

    String publicUrl
    Boolean generateDefaultPublicUrl
    String pathRewrite
    Boolean lastMileSecurity
    Boolean forwardSslSession

    Boolean objectStoreV2
    Boolean disableAmLogForwarding
    Boolean tracingEnabled

    WorkerSpecRequest createRequest() {
        new WorkerSpecRequest(this.target,
                              this.muleVersion,
                              this.lastMileSecurity,
                              this.objectStoreV2,
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
        [  'muleVersion',
           'lastMileSecurity', 'objectStoreV2', 'clustered',
            'updateStrategy', 'replicasAcrossNodes', 'generateDefaultPublicUrl', 'replicaSize',
            'workerCount', 'forwardSslSession', 'disableAmLogForwarding',
            'cpuReserved', 'memoryReserved', 'publicUrl', 'pathRewrite',
            'releaseChannel', 'javaVersion', 'tracingEnabled'
        ]
    }


    @Override
    String toString() {
        return "WorkerSpecV2Context{" +
                "muleVersion='" + (muleVersion ?: "null") + '\'' +
                ", releaseChannel='" + (releaseChannel ?: "null") + '\'' +
                ", javaVersion='" + (javaVersion ?: "null") + '\'' +
                ", target='" + (target ?: "null") + '\'' +
                ", workerCount=" + (workerCount ?: "null") +
                ", replicaSize=" + (replicaSize ?: "null") +
                ", cpuReserved=" + (cpuReserved ?: "null") +
                ", memoryReserved=" + (memoryReserved ?: "null") +
                ", replicasAcrossNodes=" + (replicasAcrossNodes ?: "null") +
                ", clustered=" + (clustered ?: "null") +
                ", updateStrategy=" + (updateStrategy ?: "null") +
                ", publicUrl='" + (publicUrl ?: "null") + '\'' +
                ", generateDefaultPublicUrl=" + (generateDefaultPublicUrl ?: "null") +
                ", pathRewrite='" + (pathRewrite ?: "null") + '\'' +
                ", lastMileSecurity=" + (lastMileSecurity ?: "null") +
                ", forwardSslSession=" + (forwardSslSession ?: "null") +
                ", objectStoreV2=" + (objectStoreV2 ?: "null") +
                ", disableAmLogForwarding=" + (disableAmLogForwarding ?: "null") +
                ", tracingEnabled=" + (tracingEnabled ?: "null") +
                '}';
    }
}
