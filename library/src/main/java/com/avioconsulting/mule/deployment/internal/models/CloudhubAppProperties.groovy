package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class CloudhubAppProperties {
    final String env
    @JsonProperty('crypto.key')
    final String cryptoKey
    @JsonProperty('anypoint.platform.client_id')
    final String clientId
    @JsonProperty('anypoint.platform.client_secret')
    final String clientSecret
    @JsonProperty('anypoint.platform.config.analytics.agent.enabled')
    final Boolean analyticsEnabled
    @JsonProperty('anypoint.platform.visualizer.layer')
    final String apiVisualizerLayer

    CloudhubAppProperties(String appName,
                          String env,
                          String cryptoKey,
                          String clientId,
                          String clientSecret,
                          Boolean analyticsEnabled = null) {
        this.env = env
        this.cryptoKey = cryptoKey
        this.clientId = clientId
        this.clientSecret = clientSecret
        this.analyticsEnabled = analyticsEnabled
        this.apiVisualizerLayer = {
            switch(appName) {
                case ~/prc.*/:
                    return 'Process'
                case ~/sys.*/:
                    return 'System'
                case ~/exp.*/:
                    return 'Experience'
            }
        }()
    }
}
