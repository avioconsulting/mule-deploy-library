package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.Version
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.*
import com.avioconsulting.mule.deployment.internal.models.graphql.GetAssetsQuery
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
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
                                                        resolvedApiSpec.apiMajorVersion,
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
                                       resolvedApiSpec.apiMajorVersion,
                                       resolvedApiSpec.isMule4OrAbove)
        }
        logger.println 'API definition does not yet exist, will create'
        createApiDefinition(resolvedApiSpec)
    }

    private ApiQueryResponse chooseApiDefinition(String instanceLabel,
                                                 String majorApiVersion,
                                                 List<ApiQueryResponse> responses) {
        def responsesForThisVersion = responses.findAll { r ->
            r.productVersion == majorApiVersion
        }
        if (responsesForThisVersion.size() == 1) {
            return responsesForThisVersion[0]
        }
        def matchViaLabel = responsesForThisVersion.find { r ->
            r.instanceLabel == instanceLabel
        }
        if (matchViaLabel) {
            return matchViaLabel
        }
        logger.println "There were multiple API definitions for this Exchange asset and none of them were labeled with '${instanceLabel}' so using the first one"
        responsesForThisVersion[0]
    }

    ExistingApiSpec getExistingApiDefinition(ApiSpec desiredApiManagerDefinition) {
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
                                                       desiredApiManagerDefinition.apiMajorVersion,
                                                       allApis)
        if (correctQueryResponse == null) {
            return null
        }
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
        def assets = getExchangeAssets(apiManagerDefinition.exchangeAssetId)
        String chosenAssetVersion
        if (assets.empty && dryRunMode == DryRunMode.OnlineValidate) {
            logger.println('In dry-run mode and Exchange push has not yet occurred (could not find asset) so using placeholder')
            chosenAssetVersion = DRY_RUN_API_ID
        } else {
            chosenAssetVersion = pickVersion(apiManagerDefinition.apiMajorVersion,
                                             appVersion,
                                             assets)
        }
        logger.println("Identified asset version ${chosenAssetVersion}")
        new ResolvedApiSpec(apiManagerDefinition,
                            chosenAssetVersion)
    }

    /**
     *
     * @param apiMajorVersion - Major version/exchange version group for the API Version
     * @param appVersion - Mule application version from the maven pom (Semver format + optional qualifier)
     * @param assets - List of published exchange assets for the specified API
     * @return - the greatest asset version less than or equal to our application verstion, or for the apiMajorVersion if it is different from the appVersion
     */
    private String pickVersion(String apiMajorVersion,
                               String appVersion,
                               List<GetAssetsQuery.Asset> assets) {
        def parsedVersions = assets.findAll { asset ->
            asset.versionGroup == apiMajorVersion
        }.collect { asset ->
            parseVersion(asset.version)
        }
        def appVersionParsed = parseVersion(appVersion)
        def majorApiVersionNumber = getMajorVersionNumber(apiMajorVersion)
        if (majorApiVersionNumber != appVersionParsed.majorVersion) {
            // our app, even if supporting 2 API versions, can only have a single app (think Runtime Manager)
            // version. therefore we have to use a "hypothetical" 1.x.x app version as our baseline if we
            // are looking for a v1 API Exchange asset
            appVersionParsed = new Version(majorApiVersionNumber,
                    appVersionParsed.minorVersion,
                    appVersionParsed.patchLevel,
                    appVersionParsed.getQualifier(), null, null)
            logger.println("Our app (${appVersion}) is supporting multiple API definitions but we are currently managing a lower major version of the API Definition, ${apiMajorVersion}. Therefore we will look for the latest Exchange asset version <= ${appVersionParsed}")
        } else {
            logger.println("Looking for latest Exchange asset version <= app version of ${appVersionParsed}")
        }
        def result = parsedVersions.sort().reverse().find { version ->
            version <= appVersionParsed
        }
        if (!result) {
            def availableVersions = assets.collect { a -> a.version }
            throw new Exception("Expected to find a ${apiMajorVersion} asset version <= our app version of ${appVersion} but did not! Asset versions found in Exchange were ${availableVersions}")
        }
        return result
    }
}
