package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger

interface IDeployerFactory {
    Deployer create(String username,
                    String password,
                    ILogger logger,
                    DryRunMode dryRunMode,
                    String anypointOrganizationName,
                    List<String> environmentsToDoDesignCenterDeploymentOn)
}
