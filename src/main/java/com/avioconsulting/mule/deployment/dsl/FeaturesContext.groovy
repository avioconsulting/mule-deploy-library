package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features

class FeaturesContext extends BaseContext {
    List<Features> createFeatureList() {

    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
