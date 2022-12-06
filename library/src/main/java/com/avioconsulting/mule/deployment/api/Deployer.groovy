package com.avioconsulting.mule.deployment.api


import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.credentials.Credential
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.subdeployers.*

/***
 * Top level deployer. This is what most of your interaction should be with
 */
class Deployer implements IDeployer {
    private final ILogger logger
    private final EnvironmentLocator environmentLocator
    private final HttpClientWrapper clientWrapper
    private final ICloudHubDeployer cloudHubDeployer
    private final ICloudHubDeployer cloudHubV2Deployer
    private final IOnPremDeployer onPremDeployer
    private final IDesignCenterDeployer designCenterDeployer
    private final IApiManagerDeployer apiManagerDeployer
    private final IPolicyDeployer policyDeployer
    private final List<String> environmentsToDoDesignCenterDeploymentOn
    private int stepNumber
    private final DryRunMode dryRunMode

    /**
     *
     * @param credential {@link Credential } to access anypoint platform.
     * @param logger all messages will be logged like this. This is Jenkins plugins friendly (or you can supply System.out)
     * @param dryRunMode Should we do a real run?
     * @param anypointOrganizationName Optional parameter. If null, the default organization/biz group for the user will be used. Otherwise supply name (NOT GUID) of the biz group or organization you want to use
     * @param baseUrl Base URL, optional
     * @param environmentsToDoDesignCenterDeploymentOn Normally workflow wise you'd only want to do this on DEV
     */
    Deployer(Credential credential,
             ILogger logger,
             DryRunMode dryRunMode,
             String anypointOrganizationName = null,
             String baseUrl = 'https://anypoint.mulesoft.com',
             List<String> environmentsToDoDesignCenterDeploymentOn = ['DEV']) {
        this(new HttpClientWrapper(baseUrl,
                                   credential,
                                   logger,
                                   anypointOrganizationName),
             dryRunMode,
             logger,
             environmentsToDoDesignCenterDeploymentOn)
    }


    private Deployer(HttpClientWrapper httpClientWrapper,
                     DryRunMode dryRunMode,
                     ILogger logger,
                     List<String> environmentsToDoDesignCenterDeploymentOn,
                     EnvironmentLocator environmentLocator = null,
                     ICloudHubDeployer cloudHubDeployer = null,
                     IOnPremDeployer onPremDeployer = null,
                     IDesignCenterDeployer designCenterDeployer = null,
                     IApiManagerDeployer apiManagerDeployer = null,
                     IPolicyDeployer policyDeployer = null) {
        this.dryRunMode = dryRunMode
        this.logger = logger
        this.environmentsToDoDesignCenterDeploymentOn = environmentsToDoDesignCenterDeploymentOn
        this.clientWrapper = httpClientWrapper
        this.environmentLocator = environmentLocator ?: new EnvironmentLocator(this.clientWrapper,
                                                                               logger)
        this.cloudHubDeployer = cloudHubDeployer ?: new CloudHubDeployer(this.clientWrapper,
                                                                         this.environmentLocator,
                                                                         logger,
                                                                         dryRunMode)
        this.cloudHubV2Deployer = new CloudHubV2Deployer(this.clientWrapper,
                                                         this.environmentLocator,
                                                         logger,
                                                         dryRunMode)
        this.onPremDeployer = onPremDeployer ?: new OnPremDeployer(this.clientWrapper,
                                                                   this.environmentLocator,
                                                                   logger,
                                                                   dryRunMode)
        this.designCenterDeployer = designCenterDeployer ?: new DesignCenterDeployer(this.clientWrapper,
                                                                                     logger,
                                                                                     dryRunMode,
                                                                                     this.environmentLocator)
        this.apiManagerDeployer = apiManagerDeployer ?: new ApiManagerDeployer(this.clientWrapper,
                                                                               this.environmentLocator,
                                                                               logger,
                                                                               dryRunMode)
        this.policyDeployer = policyDeployer ?: new PolicyDeployer(this.clientWrapper,
                                                                   this.environmentLocator,
                                                                   this.logger,
                                                                   dryRunMode)
    }

    /**
     * Deploys a CloudHub or on-prem application, end to end
     * @param appDeploymentRequest Details about how to deploy your app
     * @param apiSpecification How API specification details work. This can be optional. Doing so will automatically remove Design Center sync and policy sync from enabled features
     * @param desiredPolicies Which policies to apply. The default value is empty, which means apply no policies and remove any policies already there
     * @param enabledFeatures Which features of this tool to turn on. All by default.
     */
    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications = null,
                          List<Policy> desiredPolicies = [],
                          List<Features> enabledFeatures = [Features.All]) {
        stepNumber = 0
        // TODO: dirty?
        String description
        def deployer
        if (appDeploymentRequest instanceof CloudhubDeploymentRequest) {
            deployer = cloudHubDeployer
            description = 'CloudHub app deployment'
        } else if (appDeploymentRequest instanceof CloudhubV2DeploymentRequest) {
            deployer = cloudHubV2Deployer
            description = 'CloudHub v2 app deployment'
        } else {
            deployer = onPremDeployer
            description = 'On-prem app deployment'
        }
        def isSoapApi = apiSpecifications.any { spec -> spec.soapApi }
        performCommonDeploymentTasks(isSoapApi,
                                     apiSpecifications,
                                     desiredPolicies,
                                     appDeploymentRequest,
                                     appDeploymentRequest.environment,
                                     enabledFeatures,
                                     deployer)
        def skipReason = getFeatureSkipReason(isSoapApi,
                                              enabledFeatures,
                                              Features.AppDeployment)
        executeStep(description,
                    skipReason) {
            deployer.deploy(appDeploymentRequest)
        }
    }

    private def executeStep(String description,
                            String skipReason,
                            Closure stuff) {
        stepNumber++
        def prefix = "---------------- Step ${stepNumber}: ${description}"
        if (skipReason) {
            logger.println("${prefix} - SKIPPING due to ${skipReason}")
        } else {
            logger.println("${prefix} - EXECUTING")
            try {
                def result = stuff()
                logger.println("${prefix} - DONE")
                return result
            }
            catch (e) {
                logger.println("${prefix} - FAILED due to ${e}")
                throw e
            }
        }
    }

    private static String getFeatureSkipReason(boolean isSoapApi,
                                               List<Features> enabledFeatures,
                                               Features feature) {
        if (isSoapApi && feature == Features.DesignCenterSync) {
            return 'DesignCenterSync disabled because a SOAP API is in use'
        }
        def enabled = enabledFeatures.contains(Features.All) || enabledFeatures.contains(feature)
        enabled ? null : "Feature ${feature} was not supplied"
    }

    private def performCommonDeploymentTasks(boolean isSoapApi,
                                             ApiSpecificationList apiSpecifications,
                                             List<Policy> desiredPolicies,
                                             FileBasedAppDeploymentRequest appDeploymentRequest,
                                             String environment,
                                             List<Features> enabledFeatures,
                                             ISubDeployer deployer) {
        def isFeatureDisabled = { Features feature ->
            getFeatureSkipReason(isSoapApi,
                                 enabledFeatures,
                                 feature)
        }
        String skipReason
        if (!apiSpecifications) {
            skipReason = "no API spec(s) were provided"
        } else if (!this.environmentsToDoDesignCenterDeploymentOn.contains(environment)) {
            skipReason = "Deploying to '${environment}', only ${this.environmentsToDoDesignCenterDeploymentOn} triggers Design Center deploys"
        } else {
            skipReason = isFeatureDisabled(Features.DesignCenterSync)
        }
        apiSpecifications.each { apiSpecification ->
            def apiHeader = "${apiSpecification.name}/branch ${apiSpecification.designCenterBranchName}"
            executeStep("Design Center Deployment - ${apiHeader}",
                        skipReason) {
                designCenterDeployer.synchronizeDesignCenterFromApp(apiSpecification,
                                                                    appDeploymentRequest)
            }
            if (!apiSpecifications) {
                skipReason = "no API spec(s) were provided"
            } else {
                skipReason = isFeatureDisabled(Features.ApiManagerDefinitions)
            }
            ExistingApiSpec existingApiManagerDefinition = executeStep("API Manager Definition - ${apiHeader}",
                                                                       skipReason) {
                def isMule4 = deployer.isMule4Request(appDeploymentRequest)
                def internalSpec = new ApiSpec(apiSpecification.exchangeAssetId,
                                               apiSpecification.endpoint,
                                               environment,
                                               apiSpecification.apiMajorVersion,
                                               isMule4)
                apiManagerDeployer.synchronizeApiDefinition(internalSpec,
                                                            appDeploymentRequest.appVersion)
            } as ExistingApiSpec
            if (existingApiManagerDefinition) {
                appDeploymentRequest.setAutoDiscoveryId(apiSpecification.autoDiscoveryPropertyName,
                                                        existingApiManagerDefinition.id)
                skipReason = isFeatureDisabled(Features.PolicySync)
            } else {
                skipReason = 'API Sync was disabled so policy is too'
            }
            executeStep("Policy Sync - ${apiHeader}",
                        skipReason) {
                policyDeployer.synchronizePolicies(existingApiManagerDefinition,
                                                   desiredPolicies)
            }
        }
    }
}
