package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerV2SpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategies
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerTypes

class WorkerSpecV2Context extends BaseContext {
    String muleVersion, target
    boolean lastMileSecurity = false
    boolean persistentObjectStore = false
    boolean clustered = false
    UpdateStrategies updateStrategy = UpdateStrategies.rolling
    boolean replicasAcrossNodes = false
    String publicURL = null
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
