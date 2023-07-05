package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.internal.models.RamlFile

abstract class ExchangeAppDeploymentRequest extends AppDeploymentRequest {

    ExchangeAppDeploymentRequest(String appName, String appVersion, String environment) {
        super(appName, appVersion, environment)
    }

    @Override
    List<RamlFile> getRamlFilesFromApp(String rootRamlDirectory, boolean ignoreExchange) {
        return Collections.emptyList()
    }
}
