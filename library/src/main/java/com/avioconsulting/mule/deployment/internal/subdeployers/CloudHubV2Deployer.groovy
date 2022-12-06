package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class CloudHubV2Deployer extends BaseDeployer implements ICloudHubDeployer {
    CloudHubV2Deployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       ILogger logger,
                       DryRunMode dryRunMode) {
        this(clientWrapper,
             environmentLocator,
             // for CloudHub, the deploy cycle is longer so we wait longer
             10000,
             100,
             logger,
             dryRunMode)
    }

    CloudHubV2Deployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       int retryIntervalInMs,
                       int maxTries,
                       ILogger logger,
                       DryRunMode dryRunMode) {
        super(retryIntervalInMs,
              maxTries,
              logger,
              clientWrapper,
              environmentLocator,
              dryRunMode)
    }

    def deploy(CloudhubDeploymentRequest deploymentRequest) {

    }

    private HttpEntityEnclosingRequestBase getDeploymentHttpRequest(AppStatus existingAppStatus,
                                                                    CloudhubDeploymentRequest deploymentRequest) {

    }

    private def doDeployment(HttpEntityEnclosingRequestBase request,
                             CloudhubDeploymentRequest deploymentRequest) {

    }

    def deleteApp(String environment,
                  String appName,
                  String defaultFailReason = 'remove failed app deployment') {

    }

    def waitForAppToStart(String environment,
                          String appName,
                          AppStatusPackage baselineStatus) {

    }

    AppStatusPackage getAppStatus(String environmentName,
                                  String appName) {

    }

    @Override
    boolean isMule4Request(CloudhubDeploymentRequest deploymentRequest) {
        // TODO: Is using this (which leans on JAR vs. ZIP) ok?
        deploymentRequest.isMule4Request()
    }

    def startApplication(String environment,
                         String appName) {

    }
}
