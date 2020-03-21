package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.IDeployer

interface IDeployerFactory {
    IDeployer create(String username,
                     String password,
                     PrintStream logger,
                     String anypointOrganizationName)
}
