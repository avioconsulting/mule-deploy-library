package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.FromStringRamlResourceLoader
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.mule.apikit.model.api.ApiReference
import org.mule.parser.service.ParserService

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

    /**
     * The application property that will be set by the deployer (after upserting the API definition) so that
     * the app's autodiscovery element knows what the API ID is. Defaults to 'auto-discovery.api-id'
     */
    final String autoDiscoveryPropertyName

    /**
     * Which design center branch should be updated from app (and published to Exchange). Default is master.
     */
    final String designCenterBranchName

    /***
     * Which source directory in your app code should be used to sync to Design Center? By default
     * this is the '/api' directory which ends up inside the JAR from being in src/main/resources.
     */
    final String sourceDirectory

    /***
     * Standard request - see properties for parameter details
     */
    ApiSpecification(String name,
                     List<RamlFile> ramlFiles,
                     String mainRamlFile = null,
                     String exchangeAssetId = null,
                     String endpoint = null,
                     String autoDiscoveryPropertyName = null,
                     String designCenterBranchName = null,
                     String sourceDirectory = null) {
        this.autoDiscoveryPropertyName = autoDiscoveryPropertyName ?: 'auto-discovery.api-id'
        this.name = name
        sourceDirectory = getSourceDirectoryOrDefault(sourceDirectory)
        this.mainRamlFile = mainRamlFile ?: findMainRamlFile(ramlFiles)
        // SOAP will not have a main RAML file, just use v1 in that case
        this.apiMajorVersion = this.mainRamlFile ? getApiVersion(this.mainRamlFile,
                                                                 ramlFiles,
                                                                 sourceDirectory) : 'v1'
        this.exchangeAssetId = exchangeAssetId ?: name.toLowerCase().replace(' ',
                                                                             '-')
        this.endpoint = endpoint
        this.designCenterBranchName = designCenterBranchName ?: 'master'
        this.sourceDirectory = sourceDirectory
    }

    static String getSourceDirectoryOrDefault(String sourceDirectory) {
        sourceDirectory ?: '/api'
    }

    private static String getApiVersion(String mainRamlFile,
                                        List<RamlFile> ramlFiles,
                                        String sourceDirectory) {
        def mainFile = ramlFiles.find { f ->
            f.fileName == mainRamlFile
        }
        if (!mainFile) {
            throw new Exception("You specified '${mainRamlFile}' as your main RAML file but it does not exist in your application under ${sourceDirectory}!")
        }
        // see EnsureWeCloseLoader for why we do this
        def resourceLoader = new FromStringRamlResourceLoader(ramlFiles)
        def parserService = new ParserService()
        def apiRef = ApiReference.create(mainFile.fileName,
                                         resourceLoader)
        def parseResult = parserService.parse(apiRef)
        if (!parseResult.success()) {
            throw new Exception("RAML ${mainRamlFile} is invalid. ${parseResult.errors}")
        }
        def version = parseResult.get().version
        version ?: 'v1'
    }

    private static String findMainRamlFile(List<RamlFile> ramlFiles) {
        ramlFiles.find { ramlFile ->
            new File(ramlFile.fileName).parentFile == null
        }?.fileName
    }
}
