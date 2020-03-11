package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.*
import com.avioconsulting.mule.deployment.subdeployers.*

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
    private final List<String> environmentsToDoDesignCenterDeploymentOn
    private int stepNumber

    /**
     *
     * @param username - anypoint creds to deploy with
     * @param password - anypoint creds to deploy with
     * @param anypointOrganizationId - GUID/org ID. Right now this tool doesn't differentiate between biz groups and root orgs
     * @param logger - all messages will be logged like this. This is Jenkins plugins friendly (or you can supply System.out)
     * @param baseUrl - Base URL, optional
     * @param environmentsToDoDesignCenterDeploymentOn - Normally workflow wise you'd only want to do this on DEV
     */
    Deployer(String username,
             String password,
             String anypointOrganizationId,
             PrintStream logger,
             String baseUrl = 'https://anypoint.mulesoft.com',
             List<String> environmentsToDoDesignCenterDeploymentOn = ['DEV']) {
        this(new HttpClientWrapper(baseUrl,
                                   username,
                                   password,
                                   anypointOrganizationId,
                                   logger),
             logger,
             environmentsToDoDesignCenterDeploymentOn)
    }


    private Deployer(HttpClientWrapper httpClientWrapper,
                     PrintStream logger,
                     List<String> environmentsToDoDesignCenterDeploymentOn,
                     EnvironmentLocator environmentLocator = null,
                     ICloudHubDeployer cloudHubDeployer = null,
                     IOnPremDeployer onPremDeployer = null,
                     IDesignCenterDeployer designCenterDeployer = null) {
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
    }

    /**
     * Deploys a CloudHub application, end to end
     * @param appDeploymentRequest - Details about how to deploy your app
     * @param apiSpecification - How API specification details work. This can be optional. Doing so will automatically remove Design Center sync from enabled features
     * @param appVersion - Version of the app you are deploying (e.g. <version> from the POM)
     * @param enabledFeatures - Which features of this tool to turn on. All by default.
     */
    def deployApplication(CloudhubDeploymentRequest appDeploymentRequest,
                          String appVersion,
                          ApiSpecification apiSpecification = null,
                          List<Features> enabledFeatures = [Features.All]) {
        stepNumber = 0
        performCommonDeploymentTasks(apiSpecification,
                                     appVersion,
                                     appDeploymentRequest,
                                     appDeploymentRequest.environment,
                                     enabledFeatures)
        def skipReason = getFeatureSkipReason(enabledFeatures,
                                              Features.AppDeployment)
        executeStep('CloudHub app deployment',
                    skipReason) {
            cloudHubDeployer.deploy(appDeploymentRequest)
        }
    }

    /**
     * Deploys an on-prem application, end to end
     * @param appDeploymentRequest - Details about how to deploy your app
     * @param apiSpecification - How API specification details work. This can be optional. Doing so will automatically remove Design Center sync from enabled features
     * @param appVersion - Version of the app you are deploying (e.g. <version> from the POM)
     * @param enabledFeatures - Which features of this tool to turn on. All by default.
     */
    def deployApplication(OnPremDeploymentRequest appDeploymentRequest,
                          String appVersion,
                          ApiSpecification apiSpecification = null,
                          List<Features> enabledFeatures = [Features.All]) {
        stepNumber = 0
        performCommonDeploymentTasks(apiSpecification,
                                     appVersion,
                                     appDeploymentRequest,
                                     appDeploymentRequest.environment,
                                     enabledFeatures)
        def skipReason = getFeatureSkipReason(enabledFeatures,
                                              Features.AppDeployment)
        executeStep('on-prem app deployment',
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
                stuff()
                logger.println("${prefix} - DONE")
            }
            catch (e) {
                logger.println("${prefix} - FAILED due to ${e.cause.message}")
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
                                             String appVersion,
                                             FileBasedAppDeploymentRequest appDeploymentRequest,
                                             String environment,
                                             List<Features> enabledFeatures) {
        def isFeatureDisabled = { Features feature ->
            getFeatureSkipReason(enabledFeatures,
                                 feature)
        }
        String skipReason = null
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
                                                                appDeploymentRequest,
                                                                appVersion)
        }
    }
}
