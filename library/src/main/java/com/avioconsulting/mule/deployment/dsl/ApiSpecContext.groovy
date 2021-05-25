package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, mainRamlFile, endpoint, autoDiscoveryPropertyName, designCenterBranchName

    ApiSpecification createRequest(FileBasedAppDeploymentRequest request) {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your API spec is not complete. The following errors exist:\n${errorList}")
        }
        assert false: 'finish this'
        def ramlFiles = request.getRamlFilesFromApp('theRootRamlDirectory')
        new ApiSpecification(this.name,
                             ramlFiles,
                             this.mainRamlFile,
                             this.exchangeAssetId,
                             this.endpoint,
                             this.autoDiscoveryPropertyName,
                             this.designCenterBranchName)
    }

    @Override
    List<String> findOptionalProperties() {
        ['exchangeAssetId', 'mainRamlFile', 'endpoint', 'autoDiscoveryPropertyName', 'designCenterBranchName']
    }
}
