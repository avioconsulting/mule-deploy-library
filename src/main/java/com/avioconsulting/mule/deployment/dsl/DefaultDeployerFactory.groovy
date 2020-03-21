package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.IDeployer

class DefaultDeployerFactory implements IDeployerFactory {
    @Override
    IDeployer create(String username,
                     String password,
                     PrintStream logger,
                     String anypointOrganizationName) {
        new Deployer(username,
                     password,
                     logger,
                     anypointOrganizationName)
    }
}
