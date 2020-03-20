package com.avioconsulting.mule.deployment.dsl

class AutodiscoveryContext extends BaseContext {
    String clientId
    String clientSecret

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
