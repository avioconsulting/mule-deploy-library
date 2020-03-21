package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.IDeployer

class MuleDeployContext {
    private final IDeployer deployer

    MuleDeployContext(IDeployer deployer) {
        this.deployer = deployer
    }

    def performDeployment() {

    }
}
