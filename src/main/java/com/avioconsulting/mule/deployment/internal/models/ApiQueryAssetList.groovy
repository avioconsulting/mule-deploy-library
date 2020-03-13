package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.Immutable

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class ApiQueryAssetList {
    List<ApiQueryResponse> apis
}
