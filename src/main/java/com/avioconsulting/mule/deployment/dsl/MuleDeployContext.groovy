package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.dsl.policies.PolicyListContext

class MuleDeployContext extends BaseContext {
    String version
    private final DeployerContext settings
    private ApiSpecContext apiSpecification = new ApiSpecContext()
    private PolicyListContext policies = new PolicyListContext()
    private CloudhubContext cloudHubApplication = new CloudhubContext()

    MuleDeployContext(IDeployerFactory deployerFactory = null) {
        this.settings = new DeployerContext(deployerFactory ?: new DefaultDeployerFactory())
    }

    def performDeployment() {
        def deployer = settings.buildDeployer(System.out)
        deployer.deployApplication(cloudHubApplication.createDeploymentRequest(),
                                   apiSpecification.createRequest(),
                                   policies.createPolicyList(),
                                   [Features.All])
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
