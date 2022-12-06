package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.CloudhubV2WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategies
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerTypes

class WorkerSpecContext extends BaseContext {
    String muleVersion, updateId, groupId, target
    String publicURL = null
    boolean usePersistentQueues, customLog4j2Enabled, staticIpEnabled
    boolean objectStoreV2Enabled = true
    boolean lastMileSecurity = false
    boolean persistentObjectStore = false
    boolean clustered = false
    boolean replicasAcrossNodes = false
    boolean forwardSslSession = false
    WorkerTypes workerType = WorkerTypes.Micro
    VCoresSize replicaSize = VCoresSize.vCore1GB
    UpdateStrategies updateStrategy = UpdateStrategies.rolling
    int workerCount = 1
    AwsRegions awsRegion

    CloudhubWorkerSpecRequest createRequest() {
        new CloudhubWorkerSpecRequest(this.muleVersion,
                                      this.usePersistentQueues,
                                      this.workerCount,
                                      this.workerType,
                                      this.awsRegion,
                                      this.updateId,
                                      this.customLog4j2Enabled,
                                      this.staticIpEnabled,
                                      this.objectStoreV2Enabled)
    }

    CloudhubV2WorkerSpecRequest createV2Request() {
        new CloudhubV2WorkerSpecRequest(this.muleVersion,
                                      this.workerCount,
                                      this.replicaSize,
                                      this.target,
                                      this.objectStoreV2Enabled,
                                      this.updateStrategy,
                                      this.lastMileSecurity,
                                      this.persistentObjectStore,
                                      this.clustered,
                                      this.replicasAcrossNodes,
                                      this.publicURL,
                                      this.forwardSslSession)
    }

    @Override
    List<String> findOptionalProperties() {
        ['muleVersion', 'awsRegion', 'updateId', 'customLog4j2Enabled', 'staticIpEnabled', 'objectStoreV2Enabled']
    }

    /***
     * Allows us to do stuff like this in the DSL and have it resolve without having to import an ENUM
     * @param name
     * @return
     */
    def propertyMissing(String name) {
        switch (name) {
            case AwsRegions.simpleName:
                return AwsRegions
            case WorkerTypes.simpleName:
                return WorkerTypes
            default:
                return null
        }
    }

    def invokeMethod(String name, def args) {
        switch (name) {
            case AwsRegions.simpleName:
                return new LowerCaseEnumWrapper(AwsRegions)
            case WorkerTypes.simpleName:
                return new LowerCaseEnumWrapper(WorkerTypes)
            default:
                return super.invokeMethod(name,
                                          args)
        }
    }
}
