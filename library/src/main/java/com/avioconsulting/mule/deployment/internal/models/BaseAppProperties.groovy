package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical

@Canonical
class BaseAppProperties {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty('auto-discovery.api-id')
    String apiAutoDiscoveryId
}
