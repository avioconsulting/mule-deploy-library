package com.avioconsulting.mule.deployment.api

import com.avioconsulting.mule.deployment.api.models.*
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.subdeployers.*

/***
 * Top level deployer. This is what most of your interaction should be with
 */
class Deployer {
    private final PrintStream logger
    private final EnvironmentLocator environmentLocator
    private final HttpClientWrapper clientWrapper
    private final ICloudHubDeployer cloudHubDeployer
    private final IOnPremDeployer onPremDeployer
    private final IDesignCenterDeployer designCenterDeployer
    private final IApiManagerDeployer apiManagerDeployer
    private final IPolicyDeployer policyDeployer
    private final List<String> environmentsToDoDesignCenterDeploymentOn
    private int stepNumber

    /**
     *
     * @param username anypoint creds to deploy with
     * @param password anypoint creds to deploy with
     * @param logger all messages will be logged like this. This is Jenkins plugins friendly (or you can supply System.out)
     * @param anypointOrganizationName Optional parameter. If null, the default organization/biz group for the user will be used. Otherwise supply name (NOT GUID) of the biz group or organization you want to use
     * @param baseUrl Base URL, optional
     * @param environmentsToDoDesignCenterDeploymentOn Normally workflow wise you'd only want to do this on DEV
     */
    Deployer(String username,
             String password,
             PrintStream logger,
             String anypointOrganizationName = null,
             String baseUrl = 'https://anypoint.mulesoft.com',
             List<String> environmentsToDoDesignCenterDeploymentOn = ['DEV']) {
        this(new HttpClientWrapper(baseUrl,
                                   username,
                                   password,
                                   logger,
                                   anypointOrganizationName),
             logger,
             environmentsToDoDesignCenterDeploymentOn)
    }


    private Deployer(HttpClientWrapper httpClientWrapper,
                     PrintStream logger,
                     List<String> environmentsToDoDesignCenterDeploymentOn,
                     EnvironmentLocator environmentLocator = null,
                     ICloudHubDeployer cloudHubDeployer = null,
                     IOnPremDeployer onPremDeployer = null,
                     IDesignCenterDeployer designCenterDeployer = null,
                     IApiManagerDeployer apiManagerDeployer = null,
                     IPolicyDeployer policyDeployer = null) {
        this.logger = logger
        this.environmentsToDoDesignCenterDeploymentOn = environmentsToDoDesignCenterDeploymentOn
        this.clientWrapper = httpClientWrapper
        this.environmentLocator = environmentLocator ?: new EnvironmentLocator(this.clientWrapper,
                                                                               logger)
        this.cloudHubDeployer = cloudHubDeployer ?: new CloudHubDeployer(this.clientWrapper,
                                                                         this.environmentLocator,
                                                                         logger)
        this.onPremDeployer = onPremDeployer ?: new OnPremDeployer(this.clientWrapper,
                                                                   this.environmentLocator,
                                                                   logger)
        this.designCenterDeployer = designCenterDeployer ?: new DesignCenterDeployer(this.clientWrapper,
                                                                                     logger)
        this.apiManagerDeployer = apiManagerDeployer ?: new ApiManagerDeployer(this.clientWrapper,
                                                                               this.environmentLocator,
                                                                               logger)
        this.policyDeployer = policyDeployer ?: new PolicyDeployer(this.clientWrapper,
                                                                   this.environmentLocator,
                                                                   this.logger)
    }

    /**
     * Deploys a CloudHub application, end to end
     * @param appDeploymentRequest Details about how to deploy your app
     * @param apiSpecification How API specification details work. This can be optional. Doing so will automatically remove Design Center sync and policy sync from enabled features
     * @param desiredPolicies Which policies to apply. The default value is empty, which means apply no policies and remove any policies already there
     * @param enabledFeatures Which features of this tool to turn on. All by default.
     */
    def deployApplication(CloudhubDeploymentRequest appDeploymentRequest,
                          ApiSpecification apiSpecification = null,
                          List<Policy> desiredPolicies = [],
                          List<Features> enabledFeatures = [Features.All]) {
        stepNumber = 0
        performCommonDeploymentTasks(apiSpecification,
                                     desiredPolicies,
                                     appDeploymentRequest,
                                     appDeploymentRequest.environment,
                                     enabledFeatures,
                                     cloudHubDeployer)
        def skipReason = getFeatureSkipReason(enabledFeatures,
                                              Features.AppDeployment)
        executeStep('CloudHub app deployment',
                    skipReason) {
            cloudHubDeployer.deploy(appDeploymentRequest)
        }
    }

    /**
     * Deploys an on-prem application, end to end
     * @param appDeploymentRequest Details about how to deploy your app
     * @param apiSpecification How API specification details work. This can be optional. Doing so will automatically remove Design Center sync and policy sync from enabled features
     * @param desiredPolicies Which policies to apply. The default value is empty, which means apply no policies and remove any policies already there
     * @param enabledFeatures Which features of this tool to turn on. All by default.
     */
    def deployApplication(OnPremDeploymentRequest appDeploymentRequest,
                          ApiSpecification apiSpecification = null,
                          List<Policy> desiredPolicies = [],
                          List<Features> enabledFeatures = [Features.All]) {
        stepNumber = 0
        performCommonDeploymentTasks(apiSpecification,
                                     desiredPolicies,
                                     appDeploymentRequest,
                                     appDeploymentRequest.environment,
                                     enabledFeatures,
                                     onPremDeployer)
        def skipReason = getFeatureSkipReason(enabledFeatures,
                                              Features.AppDeployment)
        executeStep('On-prem app deployment',
                    skipReason) {
            onPremDeployer.deploy(appDeploymentRequest)
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

    private static String getFeatureSkipReason(List<Features> enabledFeatures,
                                               Features feature) {
        def enabled = enabledFeatures.contains(Features.All) || enabledFeatures.contains(feature)
        enabled ? null : "Feature ${feature} was not supplied"
    }

    private def performCommonDeploymentTasks(ApiSpecification apiSpecification,
                                             List<Policy> desiredPolicies,
                                             FileBasedAppDeploymentRequest appDeploymentRequest,
                                             String environment,
                                             List<Features> enabledFeatures,
                                             IDeployer deployer) {
        def isFeatureDisabled = { Features feature ->
            getFeatureSkipReason(enabledFeatures,
                                 feature)
        }
        String skipReason
        if (!apiSpecification) {
            skipReason = "no API spec was provided"
        } else if (!this.environmentsToDoDesignCenterDeploymentOn.contains(environment)) {
            skipReason = "Deploying to '${environment}', only ${this.environmentsToDoDesignCenterDeploymentOn} triggers Design Center deploys"
        } else {
            skipReason = isFeatureDisabled(Features.DesignCenterSync)
        }
        executeStep('Design Center Deployment',
                    skipReason) {
            designCenterDeployer.synchronizeDesignCenterFromApp(apiSpecification,
                                                                appDeploymentRequest)
        }
        if (!apiSpecification) {
            skipReason = "no API spec was provided"
        } else {
            skipReason = isFeatureDisabled(Features.ApiManagerDefinitions)
        }
        ExistingApiSpec existingApiManagerDefinition = executeStep('API Manager Definition',
                                                                   skipReason) {
            def isMule4 = deployer.isMule4Request(appDeploymentRequest)
            def internalSpec = new ApiSpec(apiSpecification.exchangeAssetId,
                                           apiSpecification.endpoint,
                                           environment,
                                           isMule4)
            apiManagerDeployer.synchronizeApiDefinition(internalSpec,
                                                        appDeploymentRequest.appVersion)
        } as ExistingApiSpec
        if (existingApiManagerDefinition) {
            appDeploymentRequest.autoDiscoveryId = existingApiManagerDefinition.id
            skipReason = isFeatureDisabled(Features.PolicySync)
        } else {
            skipReason = 'API Sync was disabled so policy is too'
        }
        executeStep('Policy Sync',
                    skipReason) {
            policyDeployer.synchronizePolicies(existingApiManagerDefinition,
                                               desiredPolicies)
        }
    }
}
