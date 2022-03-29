package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.RequestedContract
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

import javax.annotation.Nullable

class ApplicationContractDeployer implements IApplicationContractDeployer {

    final HttpClientWrapper clientWrapper
    final EnvironmentLocator environmentLocator
    final ILogger logger
    private final DryRunMode dryRunMode

    ApplicationContractDeployer(HttpClientWrapper clientWrapper,
                   EnvironmentLocator environmentLocator,
                   ILogger logger,
                   DryRunMode dryRunMode) {
        this.dryRunMode = dryRunMode
        this.logger = logger
        this.environmentLocator = environmentLocator
        this.clientWrapper = clientWrapper
    }

    def synchronizeApplicationContracts(ExistingApiSpec existingApiManagerDefinition, List<RequestedContract> requestedContractList) {
        if(requestedContractList.size() == 0) {
            return
        }
        def clientApplicationName = requestedContractList.find()?.ourClientApplicationName
        def ourClientAppId =  findOurClientAppId(clientApplicationName)
        if (ourClientAppId == null) {
            throw new Exception("Could not locate the client ${clientApplicationName} in Exchange, please make sure it has been created and the properties files updated with the associated credentials.")
        }
        def environmentId = environmentLocator.getEnvironmentId(existingApiManagerDefinition.environment)
        def contractRequestList = filterAndNormalizeContractsList(requestedContractList, collectCurrentContracts(ourClientAppId))

        contractRequestList.forEach { contract ->
            contract.ourClientApplicationId = ourClientAppId
            contract.organizationId = clientWrapper.anypointOrganizationId
            contract.environmentId = environmentId
            clientWrapper.executeWithSuccessfulCloseableResponse(new HttpGet(
                    "${clientWrapper.baseUrl}/exchange/api/v2/assets/${clientWrapper.anypointOrganizationId}/${contract.exchangeAssetId}/asset"),
                    "get version information for ${contract.exchangeAssetId}")
                    { response ->
                        contract.versionGroup = response.versionGroup
                        contract.version = response.version
                    }
            contract.apiManagerId = findApiManagerId(contract.exchangeAssetId, contract.versionGroup, environmentId)
            createContract(contract)
        }
    }

    @Nullable
    private Integer findOurClientAppId(String clientAppName) {
        clientWrapper.executeWithSuccessfulCloseableResponse(new HttpGet("${clientWrapper.baseUrl}/apiplatform/repository/v2/organizations/${clientWrapper.anypointOrganizationId}/applications?targetAdminSite=true&query=${clientAppName}"),
                "find client application information for ${clientAppName}") { response ->
            response.applications.find()?.id
        } as Integer
    }

    private List<String> collectCurrentContracts(Integer ourClientAppId) {
        clientWrapper.executeWithSuccessfulCloseableResponse(new HttpGet("${clientWrapper.baseUrl}/apiplatform/repository/v2/organizations/${clientWrapper.anypointOrganizationId}/applications/${ourClientAppId}/contracts"),
                "get contract information") { response ->
            def key = "assetId:"
            (response.apiVersion.api.name).collect {
                it.substring(it.indexOf(key) + key.length(), it.length())
            }
        } as List<String>
    }

    @Nullable
    private Integer findApiManagerId(String exchangeAssetId, String versionGroup, String environmentId) {
        clientWrapper.executeWithSuccessfulCloseableResponse(new HttpGet("${clientWrapper.baseUrl}/exchange/api/v2/assets/${clientWrapper.anypointOrganizationId}/${exchangeAssetId}/versionGroups/${versionGroup}/instances"),
                "find API Manager intance information for ${exchangeAssetId}/${versionGroup}") { response ->
            response.find()?.id
        } as Integer
    }

    private void createContract(RequestedContract contract) {
        def createRequest = new HttpPost("${clientWrapper.baseUrl}/exchange/api/v2/organizations/${clientWrapper.anypointOrganizationId}/applications/${contract.ourClientApplicationId}/contracts")
        createRequest.setEntity(new StringEntity(contract.buildJsonRequest(), ContentType.APPLICATION_JSON))
        clientWrapper.executeWithSuccessfulCloseableResponse(createRequest,
                "publish new contract to ${contract.exchangeAssetId}")
                { response ->
                    response
                }
    }

    private static List<RequestedContract> filterAndNormalizeContractsList(List<RequestedContract> requestedContractsList, List<String> existingContracts) {
        requestedContractsList.findAll({ contractRequest -> !existingContracts.contains(contractRequest.exchangeAssetId) })
    }
}
