package com.avioconsulting.mule.deployment.api

import com.avioconsulting.mule.deployment.api.models.credentials.Credential

interface IDeployerFactory {
    IDeployer create(Credential credential,
                     ILogger logger,
                     DryRunMode dryRunMode,
                     String anypointOrganizationName,
                     List<String> environmentsToDoDesignCenterDeploymentOn)
}
