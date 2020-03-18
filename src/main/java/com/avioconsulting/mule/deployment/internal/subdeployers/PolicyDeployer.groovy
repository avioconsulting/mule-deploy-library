package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class PolicyDeployer implements ApiManagerFunctionality {
    final HttpClientWrapper clientWrapper
    final EnvironmentLocator environmentLocator
    final PrintStream logger

    PolicyDeployer(HttpClientWrapper clientWrapper,
                   EnvironmentLocator environmentLocator,
                   PrintStream logger) {

        this.logger = logger
        this.environmentLocator = environmentLocator
        this.clientWrapper = clientWrapper
    }

    def synchronizePolicies(ExistingApiSpec apiSpec,
                            List<Policy> desiredPolicies) {
        def existing = getExistingPolicies(apiSpec)
        def existingForComparison = existing.collect { policy -> policy.withoutId }
        if (existingForComparison == desiredPolicies) {
            logger.println('Existing policies are correct, no updates required')
            return
        }
        def policiesToCreate = desiredPolicies - existingForComparison
        policiesToCreate.withIndex().each { Policy policy, int index ->
            createPolicy(apiSpec,
                         policy,
                         // 1 based index
                         index + 1)
        }
    }

    private def deletePolicy(ExistingApiSpec apiSpec,
                             ExistingPolicy policy) {
        logger.println("For API ${apiSpec.id}, deleting policy ${policy}")
        def request = new HttpDelete(getApiManagerUrl("/${apiSpec.id}/policies/${policy.id}",
                                                      apiSpec.environment))
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'Deleting policy')
    }

    private def createPolicy(ExistingApiSpec apiSpec,
                             Policy policy,
                             int order) {
        def pointcutData = policy.policyPathApplications.collect { policyPath ->
            [
                    methodRegex     : policyPath.httpMethods.collect { m -> m.toString() }.join('|'),
                    uriTemplateRegex: policyPath.regex
            ]
        }
        def requestPayloadMap = [
                configurationData: policy.policyConfiguration,
                pointcutData     : pointcutData.any() ? pointcutData : null,
                order            : order,
                groupId          : policy.groupId,
                assetId          : policy.assetId,
                assetVersion     : policy.version
        ]
        String requestPayload = JsonOutput.toJson(requestPayloadMap)
        logger.println("For API ${apiSpec.id}, creating policy ${JsonOutput.prettyPrint(requestPayload)}")
        def request = new HttpPost(getApiManagerUrl("/${apiSpec.id}/policies",
                                                    apiSpec.environment)).with {
            setEntity(new StringEntity(requestPayload,
                                       ContentType.APPLICATION_JSON))
            it
        }
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'creating policy')
    }

    private List<ExistingPolicy> getExistingPolicies(ExistingApiSpec apiSpec) {
        def request = new HttpGet(getApiManagerUrl("/${apiSpec.id}/policies",
                                                   apiSpec.environment))
        logger.println("For API ${apiSpec.id}, getting existing policies")
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'fetch policies') { response ->
            response.policies.sort { Map policyMap ->
                policyMap.order
            }.collect { Map policyMap ->
                def template = policyMap.template as Map
                def paths = policyMap.pointcutData.collect { Map pointCutMap ->
                    def methods = (pointCutMap.methodRegex as String).split('\\|').collect { method ->
                        HttpMethod.valueOf(method)
                    }
                    new PolicyPathApplication(methods,
                                              pointCutMap.uriTemplateRegex as String)
                }
                new ExistingPolicy(template.groupId as String,
                                   template.assetId as String,
                                   template.assetVersion as String,
                                   policyMap.configuration as Map<String, Object>,
                                   paths.any() ? paths : null,
                                   policyMap.policyId as String)
            }
        } as List<ExistingPolicy>
    }
}
