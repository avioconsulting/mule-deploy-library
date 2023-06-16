package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.deployment.FileBasedAppDeploymentRequest

class ApiSpecContext extends BaseContext {
    String name, exchangeAssetId, mainRamlFile, endpoint, autoDiscoveryPropertyName, designCenterBranchName, sourceDirectory, soapEndpointWithVersion

    ApiSpecification createRequest(FileBasedAppDeploymentRequest request) {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your API spec is not complete. The following errors exist:\n${errorList}")
        }
        if (this.soapEndpointWithVersion) {
            if (this.mainRamlFile || this.designCenterBranchName || this.sourceDirectory) {
                throw new Exception('You used soapEndpointWithVersion but also supplied 1 or more of the following. These are not compatible! mainRamlFile, designCenterBranchName, sourceDirectory')
            }
            return createSoapSpec()
        }
        return createRestSpec(request)
    }

    private ApiSpecification createSoapSpec() {
        return new ApiSpecification(this.name,
                                    this.soapEndpointWithVersion,
                                    this.exchangeAssetId,
                                    this.endpoint,
                                    this.autoDiscoveryPropertyName)
    }

    private ApiSpecification createRestSpec(FileBasedAppDeploymentRequest request) {
        // This might sort of be a circular/weird dependency between ApiSpecification and FileBasedAppDeploymentRequest
        // but not sure of the best way to handle this
        def sourceDirectory = ApiSpecification.getSourceDirectoryOrDefault(this.sourceDirectory)
        // we cannot parse the RAML without included Exchange modules
        def ramlFiles = request.getRamlFilesFromApp(sourceDirectory,
                                                    false)
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
        ['exchangeAssetId',
         'mainRamlFile',
         'endpoint',
         'autoDiscoveryPropertyName',
         'designCenterBranchName',
         'sourceDirectory',
         'soapEndpointWithVersion']
    }
}
