package com.avioconsulting.mule.deployment.api

interface IDeployerFactory {
    IDeployer create(String username,
                     String password,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn)
}
