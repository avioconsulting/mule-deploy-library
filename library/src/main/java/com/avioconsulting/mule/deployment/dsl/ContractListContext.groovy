package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.RequestedContract

class ContractListContext extends BaseContext {
    String clientApplicationName
    private Object exchangeIdsToContractWith

    List<RequestedContract> createContractRequestList(FileBasedAppDeploymentRequest request) {
        if(!clientApplicationName) {
            this.clientApplicationName =  "${request.getArtifactId()}-client"
        }
        this.clientApplicationName = "${clientApplicationName}-${request.environment}"
        def contractCollection = exchangeIdsToContractWith.collect { id ->
            new RequestedContract(clientApplicationName, id)
        }
        return contractCollection
    }

    @Override
    List<String> findOptionalProperties() {
        return ['clientApplicationName']
    }

}
