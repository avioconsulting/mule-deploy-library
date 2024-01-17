package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName

class AutodiscoveryContext extends BaseContext {
    String clientId
    String clientSecret

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
