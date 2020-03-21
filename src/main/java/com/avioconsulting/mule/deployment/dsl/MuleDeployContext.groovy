package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.dsl.policies.PolicyListContext

class MuleDeployContext extends BaseContext {
    String version
    private final DeployerContext settings
    private ApiSpecContext apiSpecification = new ApiSpecContext()
    private PolicyListContext policies = new PolicyListContext()
    private CloudhubContext cloudHubApplication = new CloudhubContext()
    private OnPremContext onPremApplication = new OnPremContext()

    MuleDeployContext(IDeployerFactory deployerFactory = null) {
        this.settings = new DeployerContext(deployerFactory ?: new DefaultDeployerFactory())
    }

    def performDeployment() {
        def deployer = settings.buildDeployer(System.out)
        def cloudHubSet = hasFieldBeenSet('cloudHubApplication')
        if (hasFieldBeenSet('onPremApplication') && cloudHubSet) {
            throw new Exception('You cannot deploy both a CloudHub and on-prem application!')
        }
        def appRequest = cloudHubSet ?
                cloudHubApplication.createDeploymentRequest() :
                onPremApplication.createDeploymentRequest()
        deployer.deployApplication(appRequest,
                                   apiSpecification.createRequest(),
                                   policies.createPolicyList(),
                                   [Features.All])
    }

    def version(String version) {
        if (version != '1.0') {
            throw new Exception("Only version 1.0 of the DSL is supported and you are using ${version}")
        }
        super.methodMissing('version',
                            version)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
