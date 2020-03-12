package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ExistingApiManagerDefinition {
    String id
    ApiManagerDefinition details

    static ExistingApiManagerDefinition createFrom(String environment,
                                                   ApiGetResponse getResponse) {
        new ExistingApiManagerDefinition(getResponse.id,
                                         new ApiManagerDefinition(getResponse.assetId,
                                                                  getResponse.assetVersion,
                                                                  getResponse.endpoint.uri,
                                                                  environment, // we want the label, not the GUID
                                                                  getResponse.instanceLabel,
                                                                  getResponse.endpoint.muleVersion4OrAbove))
    }
}
