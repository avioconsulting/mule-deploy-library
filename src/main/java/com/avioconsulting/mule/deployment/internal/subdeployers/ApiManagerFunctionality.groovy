package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper

trait ApiManagerFunctionality {
    abstract EnvironmentLocator getEnvironmentLocator()
    abstract HttpClientWrapper getClientWrapper()

    String getApiManagerUrl(String restOfUrl,
                            String environment) {
        def environmentId = environmentLocator.getEnvironmentId(environment)
        "${clientWrapper.baseUrl}/apimanager/api/v1/organizations/${clientWrapper.anypointOrganizationId}/environments/${environmentId}/apis${restOfUrl}"
    }
}
