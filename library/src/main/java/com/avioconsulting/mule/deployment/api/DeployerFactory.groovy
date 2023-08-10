package com.avioconsulting.mule.deployment.api

import com.avioconsulting.mule.deployment.api.models.credentials.Credential

class DeployerFactory implements IDeployerFactory {
    @Override
    IDeployer create(Credential credential,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn) {
        new Deployer(credential,
                     logger,
                     dryRunMode,
                     anypointOrganizationName,
                     'https://anypoint.mulesoft.com',
                     environmentsToDoDesignCenterDeploymentOn)
    }
}
