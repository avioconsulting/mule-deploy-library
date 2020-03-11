package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.ApiSpecification
import com.avioconsulting.mule.deployment.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.models.Features
import com.avioconsulting.mule.deployment.models.OnPremDeploymentRequest

/***
 * Top level deployer. This is what most of your interaction should be with
 */
class Deployer {
    private final PrintStream logger
    private final EnvironmentLocator environmentLocator
    private final HttpClientWrapper clientWrapper

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
        this.clientWrapper = new HttpClientWrapper(baseUrl,
                                                   username,
                                                   password,
                                                   anypointOrganizationId,
                                                   logger)
        this.environmentLocator = new EnvironmentLocator(this.clientWrapper,
                                                         logger)
        this.logger = logger
    }

    private Deployer(HttpClientWrapper httpClientWrapper,
                     EnvironmentLocator environmentLocator,
                     PrintStream logger) {
        this.logger = logger
        this.clientWrapper = httpClientWrapper
        this.environmentLocator = environmentLocator
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

    }
}
