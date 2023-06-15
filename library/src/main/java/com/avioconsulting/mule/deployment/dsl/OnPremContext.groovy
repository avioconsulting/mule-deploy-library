package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest

class OnPremContext extends BaseContext {
    String environment, applicationName, appVersion, file, targetServerOrClusterName
    Map<String, String> appProperties = [:]

    OnPremDeploymentRequest createDeploymentRequest() {
        validateBaseContext()
        new OnPremDeploymentRequest(this.environment,
                                    this.targetServerOrClusterName,
                                    new File(this.file),
                                    this.applicationName,
                                    this.appVersion,
                                    this.appProperties)
    }

    @Override
    List<String> findOptionalProperties() {
        ['applicationName', 'appVersion']
    }
}
