package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ApiManagerDefinition
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class ApiManagerDeployer {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger

    ApiManagerDeployer(HttpClientWrapper clientWrapper,
                       PrintStream logger) {

        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    def createApiDefinition(ApiManagerDefinition apiManagerDefinition) {
        def groupId = apiManagerDefinition.groupId
        def requestPayload = [
                spec: [
                        groupId: groupId
                ]
        ]
        def requestJson = JsonOutput.toJson(requestPayload)
        logger.println "Creating API definition using payload: ${JsonOutput.prettyPrint(requestJson)}"
        def request = new HttpPost("${clientWrapper.baseUrl}/apimanager/api/v1/organizations/${groupId}/environments/bah/apis").with {
            setEntity(new StringEntity(requestJson,
                                       ContentType.APPLICATION_JSON))
            it
        }
        def id = clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                      'Creating API Definition') { response ->
            response.id
        }
        logger.println "Created API definition with ID ${id}"
        return id.toString()
    }
}
