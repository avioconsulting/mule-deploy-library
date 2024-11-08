package com.avioconsulting.mule.deployment.internal.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class CloudhubAppProperties {
    public static final String ENV = 'env'
    public static final String CRYPTO_KEY = 'crypto.key'
    public static final String ANYPOINT_PLATFORM_CLIENT_ID = 'anypoint.platform.client_id'
    public static final String ANYPOINT_PLATFORM_CLIENT_SECRET = 'anypoint.platform.client_secret'
    public static final String ANYPOINT_PLATFORM_ANALYTICS_AGENT_ENABLED = 'anypoint.platform.config.analytics.agent.enabled'
    public static final String ANYPOINT_PLATFORM_VISUALIZATION_LAYER = 'anypoint.platform.visualizer.layer'


    @JsonProperty('env')
    final String environment
    @JsonIgnore
    String environmentProperty = ENV
    @JsonProperty('crypto.key')
    final String cryptoKey
    @JsonIgnore
    String cryptoKeyProperty = CRYPTO_KEY
    @JsonProperty('anypoint.platform.client_id')
    final String clientId
    @JsonProperty('anypoint.platform.client_secret')
    final String clientSecret
    @JsonProperty('anypoint.platform.config.analytics.agent.enabled')
    final Boolean analyticsEnabled
    @JsonProperty('anypoint.platform.visualizer.layer')
    final String apiVisualizerLayer

    CloudhubAppProperties(String appName,
                          String environment,
                          String environmentProperty,
                          String cryptoKey,
                          String cryptoKeyProperty,
                          String clientId,
                          String clientSecret,
                          Boolean analyticsEnabled = null) {
        this.environment = environment
        if(environmentProperty) this.environmentProperty = environmentProperty
        this.cryptoKey = cryptoKey
        if(cryptoKeyProperty) this.cryptoKeyProperty = cryptoKeyProperty
        this.clientId = clientId
        this.clientSecret = clientSecret
        this.analyticsEnabled = analyticsEnabled
        //TODO logic need to be improved.
        this.apiVisualizerLayer = {
            switch (appName) {
                case ~/prc.*/:
                    return 'Process'
                case ~/.*prc-api/:
                    return 'Process'
                case ~/sys.*/:
                    return 'System'
                case ~/.*sys-api/:
                    return 'System'
                case ~/exp.*/:
                    return 'Experience'
                case ~/.*exp-api/:
                    return 'Experience'
            }
        }()
    }
}
