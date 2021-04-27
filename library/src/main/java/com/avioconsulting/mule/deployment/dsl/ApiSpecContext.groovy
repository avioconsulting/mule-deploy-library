package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.models.RamlFile

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, mainRamlFile, endpoint, autoDiscoveryPropertyName

    ApiSpecification createRequest(List<RamlFile> ramlFiles) {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your API spec is not complete. The following errors exist:\n${errorList}")
        }
        new ApiSpecification(this.name,
                             ramlFiles,
                             this.mainRamlFile,
                             this.exchangeAssetId,
                             this.endpoint,
                             this.autoDiscoveryPropertyName)
    }

    @Override
    List<String> findOptionalProperties() {
        ['exchangeAssetId', 'mainRamlFile', 'endpoint', 'autoDiscoveryPropertyName']
    }
}
