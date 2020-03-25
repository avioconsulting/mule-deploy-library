package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.OnPremDeploymentRequest

class OnPremContext extends BaseContext {
    String environment, applicationName, appVersion, file, targetServerOrClusterName
    Map<String, String> appProperties = [:]

    OnPremDeploymentRequest createDeploymentRequest() {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new OnPremDeploymentRequest(this.environment,
                                    this.targetServerOrClusterName,
                                    new File(this.file),
                                    this.applicationName,
                                    this.appVersion,
                                    this.appProperties)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
