package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
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
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class PolicyDeployer implements ApiManagerFunctionality, IPolicyDeployer {
    final HttpClientWrapper clientWrapper
    final EnvironmentLocator environmentLocator
    final ILogger logger
    private final DryRunMode dryRunMode

    PolicyDeployer(HttpClientWrapper clientWrapper,
                   EnvironmentLocator environmentLocator,
                   ILogger logger,
                   DryRunMode dryRunMode) {

        this.dryRunMode = dryRunMode
        this.logger = logger
        this.environmentLocator = environmentLocator
        this.clientWrapper = clientWrapper
    }

    def synchronizePolicies(ExistingApiSpec apiSpec,
                            List<Policy> desiredPolicies) {
        desiredPolicies = normalizePolicies(desiredPolicies)
        def cannotFetch = dryRunMode == DryRunMode.OnlineValidate && apiSpec.id == ApiManagerDeployer.DRY_RUN_API_ID
        if (cannotFetch) {
            logger.println('In dry-run mode with new API definition so cannot fetch existing policies')
        }
        def existing = cannotFetch ? [] : getExistingPolicies(apiSpec)
        def unmatched = new ArrayList<ExistingPolicy>(existing)
        // Reserve exact matches first so duplicate GAVs do not steal each other's instances.
        def matches = desiredPolicies.collect { Policy desired ->
            def match = unmatched.find { it.withoutId == desired }
            if (match != null) unmatched.remove(match)
            match
        }
        desiredPolicies.eachWithIndex { Policy desired, int index ->
            if (matches[index] == null) {
                def match = unmatched.find {
                    it.groupId == desired.groupId && it.assetId == desired.assetId && it.version == desired.version
                }
                if (match != null) unmatched.remove(match)
                matches[index] = match
            }
        }
        // Remove only obsolete instances (including old versions) before creating replacements.
        unmatched.each { deletePolicy(apiSpec, it) }
        desiredPolicies.eachWithIndex { Policy desired, int index ->
            ExistingPolicy match = matches[index]
            if (match == null) {
                createPolicy(apiSpec, desired, index + 1)
            } else if (match.disabled || match.withoutId != desired || match.order != index + 1) {
                updatePolicy(apiSpec, match, desired, index + 1)
            }
        }
    }

    private def updatePolicy(ExistingApiSpec apiSpec, ExistingPolicy existing, Policy desired, int order) {
        def payload = JsonOutput.toJson([
                configurationData: desired.policyConfiguration,
                pointcutData: desired.policyPathApplications ? desired.policyPathApplications.collect {
                    [methodRegex: it.httpMethods.collect { method -> method.toString() }.join('|'),
                     uriTemplateRegex: it.regex]
                } : null,
                order: order,
                disabled: false
        ])
        if (dryRunMode != DryRunMode.Run) {
            logger.println("For API ${apiSpec.id}, WOULD enable and update policy ${existing.id} but in dry-run mode")
            return
        }
        logger.println("For API ${apiSpec.id}, enabling and updating policy ${existing.id}")
        def request = new HttpPatch(getApiManagerUrl("/${apiSpec.id}/policies/${existing.id}", apiSpec.environment))
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON))
        clientWrapper.executeWithSuccessfulCloseableResponse(request, 'updating policy')
    }

    private List<Policy> normalizePolicies(List<Policy> desiredPolicies) {
        desiredPolicies.collect { policy ->
            if (policy.groupId) {
                return policy
            }
            // we don't know what our org ID is until we're in here
            policy.withGroupId(clientWrapper.anypointOrganizationId)
        }
    }

    private def deletePolicy(ExistingApiSpec apiSpec,
                             ExistingPolicy policy) {
        if (dryRunMode != DryRunMode.Run) {
            logger.println("For API ${apiSpec.id}, WOULD delete policy ${policy} but in dry-run mode")
            return
        }
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
        if (dryRunMode != DryRunMode.Run) {
            logger.println("For API ${apiSpec.id}, WOULD create policy ${JsonOutput.prettyPrint(requestPayload)} but in dry-run mode")
            return
        }
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
        def request = new HttpGet(getApiManagerUrl("/${apiSpec.id}/policies?fullInfo=false",
                                                   apiSpec.environment))
        logger.println("For API ${apiSpec.id}, getting existing policies")
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'fetch policies') { response ->
            def policies = response instanceof List ? response : (response.policies != null ? response.policies : response.value)
            if (!(policies instanceof List)) {
                throw new IllegalStateException('Unexpected API Manager policy response; refusing to reconcile policies')
            }
            policies.sort { Map policyMap ->
                policyMap.order
            }.collect { Map policyMap ->
                def template = (policyMap.template ?: policyMap) as Map
                if (!(policyMap.id ?: policyMap.policyId) || !template.groupId || !template.assetId || !template.assetVersion) {
                    throw new IllegalStateException('API Manager returned a policy without its identity; refusing to reconcile policies')
                }
                def paths = policyMap.pointcutData.collect { Map pointCutMap ->
                    def methods = (pointCutMap.methodRegex as String).split('\\|').collect { method ->
                        HttpMethod.valueOf(method)
                    }
                    new PolicyPathApplication(methods,
                                              pointCutMap.uriTemplateRegex as String)
                }
                new ExistingPolicy(template.assetId as String,
                                   template.assetVersion as String,
                                    (policyMap.containsKey('configurationData') ? policyMap.configurationData : policyMap.configuration) as Map<String, Object>,
                                   template.groupId as String,
                                   paths.any() ? paths : null,
                                    (policyMap.id ?: policyMap.policyId) as String,
                                    policyMap.disabled == true,
                                    policyMap.order as Integer)
            }
        } as List<ExistingPolicy>
    }
}
