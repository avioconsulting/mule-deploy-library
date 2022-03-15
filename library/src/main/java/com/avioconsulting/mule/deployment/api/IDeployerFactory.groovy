package com.avioconsulting.mule.deployment.api

interface IDeployerFactory {
    IDeployer create(String username,
                     String password,
                     String connectedAppId,
                     String connectedAppSecret,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn)
}
