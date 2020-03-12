package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ApiManagerDefinition
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class ApiManagerDeployer {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger
    private final EnvironmentLocator environmentLocator

    ApiManagerDeployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       PrintStream logger) {

        this.environmentLocator = environmentLocator
        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    private HttpUriRequest createApiManagerRequest(String restOfUrl,
                                                   String environment,
                                                   Closure<HttpUriRequest> requestCreator) {
        def environmentId = environmentLocator.getEnvironmentId(environment)
        def url = "${clientWrapper.baseUrl}/apimanager/api/v1/organizations/${clientWrapper.anypointOrganizationId}/environments/${environmentId}/apis${restOfUrl}"
        requestCreator(url).with {
            setHeader('X-ANYPNT-ENV-ID',
                      environmentId)
            it
        }
    }

    def createApiDefinition(ApiManagerDefinition apiManagerDefinition) {
        def groupId = clientWrapper.anypointOrganizationId
        def requestPayload = [
                spec    : [
                        groupId: groupId,
                        assetId: apiManagerDefinition.exchangeAssetId,
                        version: apiManagerDefinition.exchangeAssetVersion
                ],
                endpoint: [
                        uri                : apiManagerDefinition.endpoint,
                        proxyUri           : null,
                        muleVersion4OrAbove: true,
                        isCloudHub         : null
                ],
                instanceLabel: apiManagerDefinition.instanceLabel
        ]
        def requestJson = JsonOutput.toJson(requestPayload)
        logger.println "Creating API definition using payload: ${JsonOutput.prettyPrint(requestJson)}"
        def request = createApiManagerRequest('',
                                              // POSTING to root
                                              apiManagerDefinition.environment) { url ->
            new HttpPost(url).with {
                setEntity(new StringEntity(requestJson,
                                           ContentType.APPLICATION_JSON))
                it
            }
        }
        def id = clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                      'Creating API Definition') { response ->
            response.id
        }
        logger.println "Created API definition with ID ${id}"
        return id.toString()
    }
}
