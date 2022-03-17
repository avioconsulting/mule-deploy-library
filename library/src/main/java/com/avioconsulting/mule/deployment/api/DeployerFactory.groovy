package com.avioconsulting.mule.deployment.api

class DeployerFactory implements IDeployerFactory {
    @Override
    IDeployer create(String username,
                     String password,
                     String connectedAppId,
                     String connectedAppSecret,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn) {
        new Deployer(username,
                     password,
                     connectedAppId,
                     connectedAppSecret,
                     logger,
                     dryRunMode,
                     anypointOrganizationName,
                     'https://anypoint.mulesoft.com',
                     environmentsToDoDesignCenterDeploymentOn)
    }
}
