package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.Deployer

class DeployerContext extends BaseContext {
    String username, password, organizationName

    Deployer buildDeployer(PrintStream logger) {
        def errors = findErrors('settings')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your settings are not complete. The following errors exist:\n${errorList}")
        }
        new Deployer(this.username,
                     this.password,
                     logger,
                     this.organizationName)
    }

    @Override
    List<String> findOptionalProperties() {
        ['organizationName']
    }
}
