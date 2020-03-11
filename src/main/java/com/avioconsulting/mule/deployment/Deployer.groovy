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
                                     appDeploymentRequest)
        executeStep(false,
                    'CloudHub app deployment',
                    null) {
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
                                     appDeploymentRequest)
        executeStep(false,
                    'on-prem app deployment',
                    null) {
            onPremDeployer.deploy(appDeploymentRequest)
        }
    }

    private def executeStep(boolean skip,
                            String description,
                            String skipReason,
                            Closure stuff) {
        stepNumber++
        def prefix = "---------------- Step ${stepNumber}: ${description}"
        if (skip) {
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

    private def performCommonDeploymentTasks(ApiSpecification apiSpecification,
                                             String appVersion,
                                             FileBasedAppDeploymentRequest appDeploymentRequest) {
        executeStep(false,
                    'Design Center Deployment',
                    'foo') {
            designCenterDeployer.synchronizeDesignCenterFromApp(apiSpecification,
                                                                appDeploymentRequest,
                                                                appVersion)
        }
    }
}
