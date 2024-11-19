package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.internal.models.RamlFile

abstract class ExchangeAppDeploymentRequest extends AppDeploymentRequest {

    ExchangeAppDeploymentRequest(ApplicationName applicationName, String appVersion, String environment, String environmentProperty) {
        super(applicationName, appVersion, environment, environmentProperty)
    }

    @Override
    List<RamlFile> getRamlFilesFromApp(String rootRamlDirectory, boolean ignoreExchange) {
        return null
    }

    @Override
    List<Features> getUnsupportedFeatures() {
        return [Features.DesignCenterSync]
    }
}
