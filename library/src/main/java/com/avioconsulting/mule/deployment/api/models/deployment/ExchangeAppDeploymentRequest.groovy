package com.avioconsulting.mule.deployment.api.models.deployment

abstract class ExchangeAppDeploymentRequest extends AppDeploymentRequest {

    ExchangeAppDeploymentRequest(String appName, String appVersion, String environment) {
        super(appName, appVersion, environment)
    }
}
