package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.IDeployer

class DeployerContext extends BaseContext {
    String username, password, organizationName
    private final IDeployerFactory deployerFactory

    DeployerContext(IDeployerFactory deployerFactory) {
        this.deployerFactory = deployerFactory
    }

    IDeployer buildDeployer(PrintStream logger) {
        def errors = findErrors('settings')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your settings are not complete. The following errors exist:\n${errorList}")
        }
        deployerFactory.create(this.username,
                               this.password,
                               logger,
                               this.organizationName)
    }

    @Override
    List<String> findOptionalProperties() {
        ['organizationName']
    }
}
