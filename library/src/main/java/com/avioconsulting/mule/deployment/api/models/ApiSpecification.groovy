package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.EnsureWeCloseRamlLoader
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.raml.v2.api.RamlModelBuilder

class ApiSpecification {
    /***
     * The name of your Design Center project and also the desired API definition name
     */
    final String name
    /**
     * What exchange-asset-id do you want to use. This is optional. By default it will be derived
     * from your name if you do not supply it.
     * E.g. Product API -> product-api
     * SystemStuff API - > systemstuff-api
     */
    final String exchangeAssetId

    /**
     * What is the "main RAML" file in your app? This is optional. If you don't specify it, it will be
     * the first RAML round at the "root" of your 'api' directory.
     */
    final String mainRamlFile

    /**
     * e.g. v1 or v2. this is optional, will be assumed to be v1 by default
     */
    final String apiMajorVersion

    /**
     * The endpoint to show in the API Manager definition.
     */
    final String endpoint

    /***
     * Standard request - see properties for parameter details
     */
    ApiSpecification(String name,
                     List<RamlFile> ramlFiles,
                     String mainRamlFile = null,
                     String exchangeAssetId = null,
                     String endpoint = null) {
        this.name = name
        this.mainRamlFile = mainRamlFile ?: findMainRamlFile(ramlFiles)
        this.apiMajorVersion = getApiVersion(this.mainRamlFile,
                                             ramlFiles)
        this.exchangeAssetId = exchangeAssetId ?: name.toLowerCase().replace(' ',
                                                                             '-')
        this.endpoint = endpoint
    }

    private static String getApiVersion(String mainRamlFile,
                                        List<RamlFile> ramlFiles) {
        // see EnsureWeCloseLoader for why we do this
        def resourceLoader = new EnsureWeCloseRamlLoader(ramlFiles)
        def builder = new RamlModelBuilder(resourceLoader)
        def mainFile = ramlFiles.find { f ->
            f.fileName == mainRamlFile
        }
        def ramlModel = builder.buildApi(mainFile.contents,
                                         '.')
        if (ramlModel.hasErrors()) {
            throw new Exception("RAML ${mainRamlFile} is invalid. ${ramlModel.validationResults}")
        }
        def version = ramlModel.apiV10.version()
        version?.value() ?: 'v1'
    }

    private static String findMainRamlFile(List<RamlFile> ramlFiles) {
        ramlFiles.find { ramlFile ->
            new File(ramlFile.fileName).parentFile == null
        }.fileName
    }
}
