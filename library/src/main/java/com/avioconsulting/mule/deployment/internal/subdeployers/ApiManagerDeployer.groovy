package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
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
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class ApiManagerDeployer implements IApiManagerDeployer, ApiManagerFunctionality {
    final HttpClientWrapper clientWrapper
    final ILogger logger
    final EnvironmentLocator environmentLocator
    private final DryRunMode dryRunMode
    static final String DRY_RUN_API_ID = '******'

    ApiManagerDeployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       ILogger logger,
                       DryRunMode dryRunMode) {
        this.dryRunMode = dryRunMode
        this.environmentLocator = environmentLocator
        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    /**
     * Creates a definition if it does not exist and updates if it does
     * @param desiredApiManagerDefinition - desired specs
     * @param appVersion - version of application being deployed
     * @return Updated, looked up API definition with ID
     */
    ExistingApiSpec synchronizeApiDefinition(ApiSpec desiredApiManagerDefinition,
                                             String appVersion) {
        def resolvedApiSpec = resolveAssetVersion(desiredApiManagerDefinition,
                                                  appVersion)
        def existing = getExistingApiDefinition(desiredApiManagerDefinition)
        if (existing) {
            if (existing.withoutId == resolvedApiSpec) {
                logger.println 'API definition already exists and is already correct, no changes required'
                return existing
            } else {
                if (dryRunMode != DryRunMode.Run) {
                    logger.println 'API definition already exists but is out of date. WOULD update but in dry-run mode'
                    return null
                }
                logger.println 'API definition already exists but is out of date, updating'
                def newDefinition = new ExistingApiSpec(existing.id,
                                                        resolvedApiSpec.exchangeAssetId,
                                                        resolvedApiSpec.exchangeAssetVersion,
                                                        resolvedApiSpec.endpoint,
                                                        resolvedApiSpec.environment,
                                                        resolvedApiSpec.isMule4OrAbove)
                updateApiDefinition(newDefinition)
                return newDefinition
            }
        }
        if (dryRunMode != DryRunMode.Run) {
            logger.println 'API definition does not yet exist, WOULD create but in dry-run mode'
            // fake
            return new ExistingApiSpec(DRY_RUN_API_ID,
                                       resolvedApiSpec.exchangeAssetId,
                                       resolvedApiSpec.exchangeAssetVersion,
                                       resolvedApiSpec.endpoint,
                                       resolvedApiSpec.environment,
                                       resolvedApiSpec.isMule4OrAbove)
        }
        logger.println 'API definition does not yet exist, will create'
        createApiDefinition(resolvedApiSpec)
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

    private ExistingApiSpec getExistingApiDefinition(ApiSpec desiredApiManagerDefinition) {
        def assetId = desiredApiManagerDefinition.exchangeAssetId
        println "Checking for existing API Manager definition using Exchange asset ID '${assetId}'"
        def environment = desiredApiManagerDefinition.environment
        def request = new HttpGet(getApiManagerUrl("?assetId=${assetId}",
                                                   environment))
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
        request = new HttpGet(getApiManagerUrl("/${correctQueryResponse.id}",
                                               environment))
        def getResponse = clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                               'Fetching API definition') {
            Map response ->
                new ObjectMapper().convertValue(response,
                                                ApiGetResponse)
        } as ApiGetResponse
        return new ExistingApiSpec(environment,
                                   getResponse)
    }

    private ExistingApiSpec createApiDefinition(ResolvedApiSpec apiManagerDefinition) {
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
        // POSTING to root
        def request = new HttpPost(getApiManagerUrl('',
                                                    apiManagerDefinition.environment)).with {
            setEntity(new StringEntity(requestJson,
                                       ContentType.APPLICATION_JSON))
            it
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

    private def updateApiDefinition(ExistingApiSpec apiManagerDefinition) {
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
        def request = new HttpPatch(getApiManagerUrl("/${apiManagerDefinition.id}",
                                                     apiManagerDefinition.environment)).with {
            setEntity(new StringEntity(requestJson,
                                       ContentType.APPLICATION_JSON))
            it
        }
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'Updating API definition')
        logger.println('Successfully updated API definition')
    }

    private ResolvedApiSpec resolveAssetVersion(ApiSpec apiManagerDefinition,
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
            def dataOptional = query.parse(bufferedSource).data()
            dataOptional.isPresent() ? dataOptional.get().assets : []
        }
        String chosenAssetVersion
        if (assets.empty && dryRunMode == DryRunMode.OnlineValidate) {
            logger.println('In dry-run mode and Exchange push has not yet occurred (could not find asset) so using placeholder')
            chosenAssetVersion = DRY_RUN_API_ID
        } else {
            def chosenAsset = pickVersion(appVersion,
                                          assets)
            chosenAssetVersion = chosenAsset.version
        }
        logger.println("Identified asset version ${chosenAssetVersion}")
        new ResolvedApiSpec(apiManagerDefinition,
                            chosenAssetVersion)
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
