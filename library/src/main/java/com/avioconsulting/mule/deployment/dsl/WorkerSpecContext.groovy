package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.WorkerTypes

class WorkerSpecContext extends BaseContext {
    String muleVersion
    boolean usePersistentQueues
    WorkerTypes workerType = WorkerTypes.Micro
    int workerCount = 1
    AwsRegions awsRegion

    CloudhubWorkerSpecRequest createRequest() {
        new CloudhubWorkerSpecRequest(this.muleVersion,
                                      this.usePersistentQueues,
                                      this.workerCount,
                                      this.workerType,
                                      this.awsRegion)
    }

    @Override
    List<String> findOptionalProperties() {
        ['muleVersion', 'awsRegion']
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

    def methodMissing(String name, def args) {
        switch (name) {
            case AwsRegions.simpleName:
                return new LowerCaseEnumWrapper(AwsRegions)
            case WorkerTypes.simpleName:
                return new LowerCaseEnumWrapper(WorkerTypes)
            default:
                return super.methodMissing(name,
                                           args)
        }
    }
}
