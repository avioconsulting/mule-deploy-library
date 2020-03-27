package com.avioconsulting.mule.deployment.api

class DeployerFactory implements IDeployerFactory {
    @Override
    IDeployer create(String username,
                     String password,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn) {
        new Deployer(username,
                     password,
                     logger,
                     dryRunMode,
                     anypointOrganizationName,
                     'https://anypoint.mulesoft.com',
                     environmentsToDoDesignCenterDeploymentOn)
    }
}
