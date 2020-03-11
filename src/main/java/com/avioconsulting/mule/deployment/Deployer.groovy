package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.ApiSpecification
import com.avioconsulting.mule.deployment.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.models.Features
import com.avioconsulting.mule.deployment.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.subdeployers.CloudHubDeployer
import com.avioconsulting.mule.deployment.subdeployers.ICloudHubDeployer
import com.avioconsulting.mule.deployment.subdeployers.IOnPremDeployer
import com.avioconsulting.mule.deployment.subdeployers.OnPremDeployer

/***
 * Top level deployer. This is what most of your interaction should be with
 */
class Deployer {
    private final PrintStream logger
    private final EnvironmentLocator environmentLocator
    private final HttpClientWrapper clientWrapper
    private final ICloudHubDeployer cloudHubDeployer
    private final IOnPremDeployer onPremDeployer

    /**
     *
     * @param username - anypoint creds to deploy with
     * @param password - anypoint creds to deploy with
     * @param anypointOrganizationId - GUID/org ID. Right now this tool doesn't differentiate between biz groups and root orgs
     * @param logger - all messages will be logged like this. This is Jenkins plugins friendly (or you can supply System.out)
     * @param baseUrl - Base URL, optional
     */
    Deployer(String username,
             String password,
             String anypointOrganizationId,
             PrintStream logger,
             String baseUrl = 'https://anypoint.mulesoft.com') {
        this(new HttpClientWrapper(baseUrl,
                                   username,
                                   password,
                                   anypointOrganizationId,
                                   logger),
             logger)
    }


    private Deployer(HttpClientWrapper httpClientWrapper,
                     PrintStream logger,
                     EnvironmentLocator environmentLocator = null,
                     ICloudHubDeployer cloudHubDeployer = null,
                     IOnPremDeployer onPremDeployer = null) {
        this.logger = logger
        this.clientWrapper = httpClientWrapper
        this.environmentLocator = environmentLocator ?: new EnvironmentLocator(this.clientWrapper,
                                                                               logger)
        this.cloudHubDeployer = cloudHubDeployer ?: new CloudHubDeployer(this.clientWrapper,
                                                                         this.environmentLocator,
                                                                         logger)
        this.onPremDeployer = onPremDeployer ?: new OnPremDeployer(this.clientWrapper,
                                                                   this.environmentLocator,
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
        logger.println('Step 1: Deploying application to CloudHub')
        cloudHubDeployer.deploy(appDeploymentRequest)
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
        logger.println('Step 1: Deploying application to CloudHub')
        onPremDeployer.deploy(appDeploymentRequest)
    }
}
