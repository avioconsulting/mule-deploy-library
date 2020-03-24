package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger

class DeployerFactory implements IDeployerFactory {
    @Override
    Deployer create(String username,
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
