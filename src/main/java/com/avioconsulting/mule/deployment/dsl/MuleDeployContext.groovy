package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.dsl.policies.PolicyListContext

class MuleDeployContext extends BaseContext {
    String version
    private final DeployerContext settings
    private ApiSpecContext apiSpecification = new ApiSpecContext()
    private PolicyListContext policies = new PolicyListContext()
    private CloudhubContext cloudHubApplication = new CloudhubContext()
    private OnPremContext onPremApplication = new OnPremContext()
    private FeaturesContext enabledFeatures = new FeaturesContext()

    MuleDeployContext(IDeployerFactory deployerFactory = null) {
        this.settings = new DeployerContext(deployerFactory ?: new DefaultDeployerFactory())
    }

    def findErrors() {
        List<String> errors = super.findErrors()
        if (!hasFieldBeenSet('settings')) {
            errors << '- settings missing'
        }
        if (!cloudHubSet && !onPremSet) {
            errors << '- Either onPremApplication or cloudHubApplication should be supplied'
        }
        return errors
    }

    private boolean isCloudHubSet() {
        hasFieldBeenSet('cloudHubApplication')
    }

    private boolean isOnPremSet() {
        hasFieldBeenSet('onPremApplication')
    }

    private ApiSpecification createApiSpec() {
        hasFieldBeenSet('apiSpecification') ? apiSpecification.createRequest() : null
    }

    private List<Features> obtainFeatures() {
        hasFieldBeenSet('enabledFeatures') ? enabledFeatures.createFeatureList() : [Features.All]
    }

    def removeFeature(Features feature,
                      List<Features> currentFeatures) {
        if (currentFeatures == [Features.All]) {
            currentFeatures = Features.values() - [Features.All]
        }
        currentFeatures - [feature]
    }

    def performDeployment() {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your file is not complete. The following errors exist:\n${errorList}")
        }
        def deployer = settings.buildDeployer(System.out)
        if (onPremSet && cloudHubSet) {
            throw new Exception('You cannot deploy both a CloudHub and on-prem application!')
        }
        def policyList = policies.createPolicyList()
        def features = obtainFeatures()
        if (!hasFieldBeenSet('policies')) {
            // don't want to even touch policies if this is the case
            features = removeFeature(Features.PolicySync,
                                     features)
        }
        if (cloudHubSet) {
            deployer.deployApplication(cloudHubApplication.createDeploymentRequest(),
                                       createApiSpec(),
                                       policyList,
                                       features)
        } else {
            deployer.deployApplication(onPremApplication.createDeploymentRequest(),
                                       createApiSpec(),
                                       policyList,
                                       features)
        }
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
