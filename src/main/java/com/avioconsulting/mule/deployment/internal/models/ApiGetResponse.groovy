package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ApiGetResponse {
    String id, instanceLabel, assetId, assetVersion
    ApiGetEndpointInfo endpoint
}
