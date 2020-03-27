package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical

@Canonical
@JsonInclude(JsonInclude.Include.NON_NULL)
class CloudhubAppProperties extends BaseAppProperties {
    String env
    @JsonProperty('crypto.key')
    String cryptoKey
    @JsonProperty('anypoint.platform.client_id')
    String clientId
    @JsonProperty('anypoint.platform.client_secret')
    String clientSecret
    @JsonProperty('anypoint.platform.config.analytics.agent.enabled')
    Boolean analyticsEnabled
}
