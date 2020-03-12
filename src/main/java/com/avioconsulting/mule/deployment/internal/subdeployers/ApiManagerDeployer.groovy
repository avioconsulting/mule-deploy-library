package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.*
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpGet
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

    private ApiQueryResponse chooseApiDefinition(String instanceLabel,
                                                 List<ApiQueryResponse> responses) {
        if (responses.size() == 1) {
            return responses[0]
        }
        def matchViaLabel = responses.find { r ->
            r.instanceLabel == instanceLabel
        }
        if (matchViaLabel) {
            return matchViaLabel
        }
        logger.println "There were multiple API definitions for this Exchange asset and none of them were labeled with '${instanceLabel}' so using the first one"
        responses[0]
    }

    ExistingApiManagerDefinition getExistingApiDefinition(ApiManagerDefinition desiredApiManagerDefinition) {
        def assetId = desiredApiManagerDefinition.exchangeAssetId
        println "Checking for existing API Manager definition using Exchange asset ID '${assetId}'"
        def environment = desiredApiManagerDefinition.environment
        def request = createApiManagerRequest("?assetId=${assetId}",
                                              environment) { url ->
            new HttpGet(url)
        }
        def queryResponses = clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                                  'Querying API Definitions') {
            Map response ->
                new ObjectMapper().convertValue(response,
                                                ApiQueryByExchangeAssetIdResponse)
        } as ApiQueryByExchangeAssetIdResponse
        if (queryResponses.total == 0) {
            return null
        }
        def allApis = queryResponses.assets.collectMany { a -> a.apis }
        def correctQueryResponse = chooseApiDefinition(desiredApiManagerDefinition.instanceLabel,
                                                       allApis)
        println "Identified API ID ${correctQueryResponse.id}, now retrieving details"
        def getRequest = createApiManagerRequest("/${correctQueryResponse.id}",
                                                 environment) { url ->
            new HttpGet(url)
        }
        def getResponse = clientWrapper.executeWithSuccessfulCloseableResponse(getRequest,
                                                                               'Fetching API definition') {
            Map response ->
                new ObjectMapper().convertValue(response,
                                                ApiGetResponse)
        } as ApiGetResponse
        return ExistingApiManagerDefinition.createFrom(environment,
                                                       getResponse)
    }

    ExistingApiManagerDefinition createApiDefinition(ApiManagerDefinition apiManagerDefinition) {
        def groupId = clientWrapper.anypointOrganizationId
        def requestPayload = [
                spec         : [
                        groupId: groupId,
                        assetId: apiManagerDefinition.exchangeAssetId,
                        version: apiManagerDefinition.exchangeAssetVersion
                ],
                endpoint     : [
                        uri                : apiManagerDefinition.endpoint,
                        proxyUri           : null,
                        muleVersion4OrAbove: apiManagerDefinition.isMule4OrAbove,
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
        def createResponse = clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                                  'Creating API Definition') {
            Map response ->
                new ObjectMapper().convertValue(response,
                                                ApiGetResponse)
        } as ApiGetResponse
        logger.println "Created API definition with ID ${createResponse.id}"
        return ExistingApiManagerDefinition.createFrom(apiManagerDefinition.environment,
                                                       createResponse)
    }

    def updateApiDefinition(ExistingApiManagerDefinition apiManagerDefinition) {

    }
}
