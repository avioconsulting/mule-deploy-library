package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.PolicyPathApplication
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy
import org.apache.http.client.methods.HttpGet

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
                new ExistingPolicy(template.assetId as String,
                                   template.assetVersion as String,
                                   policyMap.configuration as Map<String, Object>,
                                   paths,
                                   policyMap.policyId as String)
            }
        } as List<ExistingPolicy>
    }
}
