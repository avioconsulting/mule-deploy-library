package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest

class OnPremContext extends BaseContext {
    String environment, appVersion, file, targetServerOrClusterName
    ApplicationNameContext applicationNameContext = new ApplicationNameContext()
    Map<String, String> appProperties = [:]

    OnPremDeploymentRequest createDeploymentRequest() {
        validateBaseContext()
        new OnPremDeploymentRequest(this.environment,
                                    this.targetServerOrClusterName,
                                    new File(this.file),
                                    this.applicationNameContext.createApplicationName(),
                                    this.appVersion,
                                    this.appProperties)
    }

    @Override
    List<String> findOptionalProperties() {
        ['applicationName', 'appVersion']
    }
}
