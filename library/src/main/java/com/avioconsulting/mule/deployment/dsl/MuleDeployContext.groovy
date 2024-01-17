package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.deployment.AppDeploymentRequest
import com.avioconsulting.mule.deployment.dsl.policies.PolicyListContext

class MuleDeployContext extends BaseContext {
    String version
    private ApiSpecListContext apiSpecifications = new ApiSpecListContext()
    private PolicyListContext policies = new PolicyListContext()
    private CloudhubContext cloudHubApplication = new CloudhubContext()
    private CloudhubV2Context cloudHubV2Application = new CloudhubV2Context()
    private RuntimeFabricContext runtimeFabricApplication = new RuntimeFabricContext()
    private OnPremContext onPremApplication = new OnPremContext()
    private FeaturesContext enabledFeatures = new FeaturesContext()

    def findErrors() {
        List<String> errors = super.findErrors()
        if (!cloudHubSet && !cloudHubV2Set && !onPremSet && !runtimeFabricSet) {
            errors << '- Either onPremApplication, cloudHubApplication, cloudHubV2Application or runtimeFabricApplication should be supplied'
        }
        return errors
    }

    private boolean isCloudHubSet() {
        hasFieldBeenSet('cloudHubApplication')
    }

    private boolean isCloudHubV2Set() {
        hasFieldBeenSet('cloudHubV2Application')
    }

    private boolean isRuntimeFabricSet() {
        hasFieldBeenSet('runtimeFabricApplication')
    }

    private boolean isOnPremSet() {
        hasFieldBeenSet('onPremApplication')
    }

    private List<Features> obtainFeatures() {
        hasFieldBeenSet('enabledFeatures') ? enabledFeatures.createFeatureList() : [Features.All]
    }

    void apiSpecification(Closure closure) {
        apiSpecifications.apiSpecification(closure)
    }

    static def removeFeatures(List<Features> featuresToRemove,
                             List<Features> currentFeatures) {
        if (!featuresToRemove.isEmpty()) {
            if (currentFeatures == [Features.All]) {
                currentFeatures = Features.values() - [Features.All]
            }
            currentFeatures = currentFeatures - featuresToRemove
        }

        currentFeatures
    }

    static def removeFeature(Features feature,
                             List<Features> currentFeatures) {
        removeFeatures([feature], currentFeatures)
    }

    DeploymentPackage createDeploymentPackage() {
        def errors = findErrors()
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your file is not complete. The following errors exist:\n${errorList}")
        }
        if (onPremSet && (cloudHubSet || cloudHubV2Set || runtimeFabricSet)) {
            throw new Exception('You cannot deploy both a CloudHub and on-prem application!')
        }
        def policyList = policies.createPolicyList()
        def features = obtainFeatures()
        if (!hasFieldBeenSet('policies')) {
            // don't want to even touch policies if this is the case
            features = removeFeature(Features.PolicySync,
                                     features)
        }

        AppDeploymentRequest deploymentRequest

        if (cloudHubSet) {
            deploymentRequest = cloudHubApplication.createDeploymentRequest()
        } else if (onPremSet) {
            deploymentRequest = onPremApplication.createDeploymentRequest()
        } else if (cloudHubV2Set) {
            deploymentRequest = cloudHubV2Application.createDeploymentRequest()
        } else if (runtimeFabricApplication) {
            deploymentRequest = runtimeFabricApplication.createDeploymentRequest()
        } else {
            // if the code falls here, you should go to findErrors method and add the missing condition
            throw new Exception("No application deployment was supplied!")
        }

        features = removeFeatures(deploymentRequest.getUnsupportedFeatures(), features)

        return new DeploymentPackage(deploymentRequest,
                apiSpecifications.createApiSpecList(deploymentRequest),
                policyList,
                features)
    }

    def version(String version) {
        if (version != '1.0') {
            throw new Exception("Only version 1.0 of the DSL is supported and you are using ${version}")
        }
        super.invokeMethod('version',
                            version)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}
