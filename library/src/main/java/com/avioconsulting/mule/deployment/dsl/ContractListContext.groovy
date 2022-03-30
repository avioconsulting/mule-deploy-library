package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.RequestedContract

class ContractListContext extends BaseContext {
    String clientApplicationName
    private List exchangeIdsToContractWith

    List<RequestedContract> createContractRequestList(FileBasedAppDeploymentRequest request) {
        def contractCollection = exchangeIdsToContractWith.collect { id ->
            if (!isNormalizedClientApplicationName(clientApplicationName, request.environment))
                normalizeClientApplicationName(request)
            new RequestedContract(clientApplicationName, id)
        }
        return contractCollection
    }

    String normalizeClientApplicationName(FileBasedAppDeploymentRequest request) {
        if(!clientApplicationName) {
            this.clientApplicationName =  "${request.getArtifactId()}-client"
        }
        this.clientApplicationName = "${clientApplicationName}-${request.environment}"
    }

    @Override
    List<String> findOptionalProperties() {
        return ['clientApplicationName']
    }

    static boolean isNormalizedClientApplicationName(String clientApplicationName, String environment) {
        clientApplicationName.contains("client-${environment}")
    }
}
