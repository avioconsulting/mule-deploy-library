package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification

class ApiSpecContext extends BaseContext {
    ApiSpecification createRequest() {

    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
