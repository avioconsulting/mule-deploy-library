package com.avioconsulting.mule.deployment.internal.http

import com.avioconsulting.mule.deployment.api.ILogger
import org.apache.http.client.methods.HttpGet

class EnvironmentLocator {
    private final HttpClientWrapper clientWrapper
    private final ILogger logger
    private String anypointOrganizationId
    @Lazy
    private Map<String, String> environments = {
        logger.println('Fetching all environment GUIDs')
        def anypointOrganizationId = this.anypointOrganizationId ?: clientWrapper.anypointOrganizationId
        def request = new HttpGet("${clientWrapper.baseUrl}/accounts/api/organizations/${anypointOrganizationId}/environments")
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             "Retrieve environments (check to ensure your org ID, ${anypointOrganizationId}, is correct and the credentials you are using have the right permissions.)")
            return result.data.collectEntries { env ->
                [env.name, env.id]
            }
        }
        finally {
            response.close()
        }
    }()

    EnvironmentLocator(HttpClientWrapper httpClientWrapper,
                       ILogger logger) {
        this.logger = logger
        this.clientWrapper = httpClientWrapper
    }

    def getEnvironmentId(String environmentName, String anypointOrganizationId = null) {
        this.anypointOrganizationId = anypointOrganizationId
        def environment = environments[environmentName]
        if (!environment) {
            def valids = environments.keySet()
            throw new Exception("Unable to find environment '${environmentName}'. Valid environments are ${valids}")
        }
        return environment
    }
}
