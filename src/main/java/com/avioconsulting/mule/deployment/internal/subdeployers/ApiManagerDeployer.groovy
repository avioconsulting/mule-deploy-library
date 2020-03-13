package com.avioconsulting.mule.deployment.internal.subdeployers


import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.*
import com.avioconsulting.mule.deployment.internal.models.graphql.GetAssetsQuery
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import okio.Okio
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
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

    ExistingApiSpec getExistingApiDefinition(ApiSpec desiredApiManagerDefinition) {
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
        return new ExistingApiSpec(environment,
                                   getResponse)
    }

    ExistingApiSpec createApiDefinition(ResolvedApiSpec apiManagerDefinition) {
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
        return new ExistingApiSpec(apiManagerDefinition.environment,
                                   createResponse)
    }

    def updateApiDefinition(ExistingApiSpec apiManagerDefinition) {
        def requestPayload = [
                assetVersion : apiManagerDefinition.exchangeAssetVersion,
                instanceLabel: apiManagerDefinition.instanceLabel,
                endpoint     : [
                        uri                : apiManagerDefinition.endpoint,
                        proxyUri           : null,
                        muleVersion4OrAbove: apiManagerDefinition.isMule4OrAbove,
                        isCloudHub         : null
                ]
        ]
        def requestJson = JsonOutput.toJson(requestPayload)
        logger.println "Updating API definition using payload: ${JsonOutput.prettyPrint(requestJson)}"
        def request = createApiManagerRequest("/${apiManagerDefinition.id}",
                                              apiManagerDefinition.environment) { url ->
            new HttpPatch(url).with {
                setEntity(new StringEntity(requestJson,
                                           ContentType.APPLICATION_JSON))
                it
            }
        }
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'Updating API definition')
        logger.println('Successfully updated API definition')
    }

    ResolvedApiSpec resolveAssetVersion(ApiSpec apiManagerDefinition,
                                        String appVersion) {
        def assetId = apiManagerDefinition.exchangeAssetId
        def query = new GetAssetsQuery(assetId,
                                       clientWrapper.anypointOrganizationId)
        def requestPayload = [
                query    : query.queryDocument(),
                variables: query.variables().marshal()
        ]
        logger.println "Searching for assets for Exchange asset '${assetId}'"
        def request = new HttpPost("${clientWrapper.baseUrl}/graph/api/v1/graphql").with {
            setEntity(new StringEntity(JsonOutput.toJson(requestPayload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        def assets = clientWrapper.execute(request).withCloseable { response ->
            clientWrapper.assertSuccessfulResponse(response,
                                                   'GraphQL query')
            def source = Okio.source(response.entity.content)
            def bufferedSource = Okio.buffer(source)
            query.parse(bufferedSource).data().get().assets
        }
        def chosenAsset = pickVersion(appVersion,
                                      assets)
        logger.println("Identified asset version ${chosenAsset.version}")
        new ResolvedApiSpec(apiManagerDefinition,
                            chosenAsset.version)
    }

    private static int getLastVersionOctet(String version) {
        version.split('\\.').last().toInteger()
    }

    private static GetAssetsQuery.Asset pickVersion(String appVersion,
                                                    List<GetAssetsQuery.Asset> assets) {
        def lastAppVersionOctet = getLastVersionOctet(appVersion)
        def result = assets.reverse().find { asset ->
            def lastAssetVersionOctet = getLastVersionOctet(asset.version)
            return lastAssetVersionOctet <= lastAppVersionOctet
        }
        if (!result) {
            def availableVersions = assets.collect { a -> a.version }
            throw new Exception("Expected to find an asset version <= our app version of ${appVersion} but did not! Asset versions found in Exchange were ${availableVersions}")
        }
        return result
    }
}
