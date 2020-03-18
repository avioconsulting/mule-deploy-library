package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy
import groovy.json.JsonOutput
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

    private def createPolicy(ExistingApiSpec apiSpec,
                             Policy policy) {
        String requestPayload = '{}'
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
                                   paths,
                                   policyMap.policyId as String)
            }
        } as List<ExistingPolicy>
    }
}
