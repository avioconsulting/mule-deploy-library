package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ApiSpec {
    final String exchangeAssetId, endpoint, environment, instanceLabel, apiMajorVersion
    final boolean isMule4OrAbove

    ApiSpec(String exchangeAssetId,
            String endpoint,
            String environment,
            String apiMajorVersion,
            boolean isMule4OrAbove,
            String instanceLabel = null) {
        this.apiMajorVersion = apiMajorVersion
        this.exchangeAssetId = exchangeAssetId
        this.endpoint = endpoint
        this.environment = environment
        this.isMule4OrAbove = isMule4OrAbove
        this.instanceLabel = instanceLabel ?: "${environment} - Automated"
    }
}
