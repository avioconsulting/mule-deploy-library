package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, mainRamlFile, endpoint, autoDiscoveryPropertyName, designCenterBranchName, sourceDirectory

    ApiSpecification createRequest(FileBasedAppDeploymentRequest request) {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your API spec is not complete. The following errors exist:\n${errorList}")
        }
        def sourceDirectory = ApiSpecification.getSourceDirectoryOrDefault(this.sourceDirectory)
        def ramlFiles = request.getRamlFilesFromApp(sourceDirectory)
        new ApiSpecification(this.name,
                             ramlFiles,
                             this.mainRamlFile,
                             this.exchangeAssetId,
                             this.endpoint,
                             this.autoDiscoveryPropertyName,
                             this.designCenterBranchName,
                             this.sourceDirectory)
    }

    @Override
    List<String> findOptionalProperties() {
        ['exchangeAssetId', 'mainRamlFile', 'endpoint', 'autoDiscoveryPropertyName', 'designCenterBranchName', 'sourceDirectory']
    }
}
