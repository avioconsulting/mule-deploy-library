package com.avioconsulting.jenkins.mule.impl

import org.apache.http.client.methods.HttpGet

class EnvironmentLocator {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger
    @Lazy
    private final Map<String, String> environments = {
        getEnvironments()
    }

    EnvironmentLocator(HttpClientWrapper httpClientWrapper,
                       PrintStream logger) {
        this.logger = logger
        this.clientWrapper = httpClientWrapper
    }

    def getEnvironmentId(String environmentName) {
        def environment = environments[environmentName]
        if (!environment) {
            def valids = environments.keySet()
            throw new Exception("Unable to find environment '${environmentName}'. Valid environments are ${valids}")
        }
    }

    private Map<String, String> getEnvironments() {
        def anypointOrganizationId = clientWrapper.anypointOrganizationId
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
    }
}
