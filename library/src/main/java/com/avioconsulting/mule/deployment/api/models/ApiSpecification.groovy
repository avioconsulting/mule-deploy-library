package com.avioconsulting.mule.deployment.api.models

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
     * e.g. v1 or v2. this is optional, will be assumed to be v1 by default
     */
    final String apiMajorVersion
    /**
     * What is the "main RAML" file in your app? This is optional. If you don't specify it, it will be
     * the first RAML round at the "root" of your 'api' directory.
     */
    final String mainRamlFile
    /**
     * The endpoint to show in the API Manager definition.
     */
    final String endpoint

    /***
     * Standard request - see properties for parameter details
     */
    ApiSpecification(String name,
                     String apiMajorVersion = 'v1',
                     String mainRamlFile = null,
                     String exchangeAssetId = null,
                     String endpoint = null) {
        this.name = name
        this.apiMajorVersion = apiMajorVersion ?: 'v1'
        this.mainRamlFile = mainRamlFile
        this.exchangeAssetId = exchangeAssetId ?: name.toLowerCase().replace(' ',
                                                                             '-')
        this.endpoint = endpoint
    }
}
