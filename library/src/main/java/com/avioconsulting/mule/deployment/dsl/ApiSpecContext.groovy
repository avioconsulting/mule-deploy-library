package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, mainRamlFile, endpoint

    ApiSpecification createRequest() {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your API spec is not complete. The following errors exist:\n${errorList}")
        }
        new ApiSpecification(this.name,
                             this.mainRamlFile,
                             this.exchangeAssetId,
                             this.endpoint)
    }

    @Override
    List<String> findOptionalProperties() {
        ['exchangeAssetId', 'mainRamlFile', 'endpoint']
    }
}
