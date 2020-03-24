package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode

interface IDeployerFactory {
    Deployer create(String username,
                    String password,
                    PrintStream logger,
                    DryRunMode dryRunMode,
                    String anypointOrganizationName,
                    List<String> environmentsToDoDesignCenterDeploymentOn)
}
