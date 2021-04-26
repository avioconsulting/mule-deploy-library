package com.avioconsulting.mule.deployment.internal.models

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import groovy.transform.Immutable

@Immutable
class ApiSpecificationRamlSelected {
    /***
     * The name of your Design Center project and also the desired API definition name
     */
    String name
    /**
     * What exchange-asset-id do you want to use. This is optional. By default it will be derived
     * from your name if you do not supply it.
     * E.g. Product API -> product-api
     * SystemStuff API - > systemstuff-api
     */
    String exchangeAssetId

    /**
     * What is the "main RAML" file in your app? This is optional. If you don't specify it, it will be
     * the first RAML round at the "root" of your 'api' directory.
     */
    String mainRamlFile
    /**
     * The endpoint to show in the API Manager definition.
     */
    String endpoint

    /**
     * e.g. v1 or v2. this is optional, will be assumed to be v1 by default
     */
    String apiMajorVersion

    static ApiSpecificationRamlSelected createFromDslModel(ApiSpecification model,
                                                           String mainRamlFile,
                                                           String apiMajorVersion) {
        new ApiSpecificationRamlSelected(model.name,
                                         model.exchangeAssetId,
                                         mainRamlFile,
                                         model.endpoint,
                                         apiMajorVersion)
    }
}
