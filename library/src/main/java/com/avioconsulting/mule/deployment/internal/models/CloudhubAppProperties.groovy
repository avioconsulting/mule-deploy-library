package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical

@Canonical
class CloudhubAppProperties extends BaseAppProperties {
    String env
    @JsonProperty('crypto.key')
    String cryptoKey
    @JsonProperty('anypoint.platform.client_id')
    String clientId
    @JsonProperty('anypoint.platform.client_secret')
    String clientSecret
}
