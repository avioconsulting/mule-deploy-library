package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features

class FeaturesContext {
    private List<Features> features = []

    List<Features> createFeatureList() {
        if (features.empty) {
            throw new Exception('No features were specified. Either A) remove the enabledFeatures section, B) Add All inside enabledFeatures, or C) Add specific features')
        }
        if (features.contains(Features.All) && features.unique().size() > 1) {
            throw new Exception('You cannot combine All with other features')
        }
        features
    }

    def propertyMissing(String name) {
        def matching = Features.values().find { f ->
            f.name().uncapitalize() == name
        }
        assert matching: "Expected to match ${name} to uncapitalized version of ${Features.values()}"
        features << matching
    }
}
