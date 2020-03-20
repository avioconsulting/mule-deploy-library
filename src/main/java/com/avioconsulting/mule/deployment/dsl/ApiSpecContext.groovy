package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, apiMajorVersion, mainRamlFile, endpoint

    ApiSpecification createRequest() {
        new ApiSpecification(this.name,
                             this.apiMajorVersion,
                             this.mainRamlFile,
                             this.exchangeAssetId,
                             this.endpoint)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
