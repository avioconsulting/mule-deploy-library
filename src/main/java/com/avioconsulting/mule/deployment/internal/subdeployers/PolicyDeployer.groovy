package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy

class PolicyDeployer {
    private final HttpClientWrapper clientWrapper
    private final EnvironmentLocator environmentLocator
    private final PrintStream logger

    PolicyDeployer(HttpClientWrapper clientWrapper,
                   EnvironmentLocator environmentLocator,
                   PrintStream logger) {

        this.logger = logger
        this.environmentLocator = environmentLocator
        this.clientWrapper = clientWrapper
    }

    private List<ExistingPolicy> getExistingPolicies(ExistingApiSpec apiSpec) {
        []
    }
}
