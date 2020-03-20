package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification

class ApiSpecContext extends BaseContext {
    String name

    ApiSpecification createRequest() {
        new ApiSpecification(this.name)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
