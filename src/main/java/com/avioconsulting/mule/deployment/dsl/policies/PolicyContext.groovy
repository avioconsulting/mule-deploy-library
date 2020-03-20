package com.avioconsulting.mule.deployment.dsl.policies


import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.dsl.BaseContext
import com.avioconsulting.mule.deployment.dsl.policies.PathsContext


class PolicyContext extends BaseContext {
    String groupId, assetId, version
    Map<String, String> config
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false

    Policy createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        def errors = findErrors('policies.policy')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your policy spec is not complete. The following errors exist:\n${errorList}")
        }
        new Policy(this.groupId ?: Policy.mulesoftGroupId,
                   this.assetId,
                   this.version,
                   this.config,
                   pathListing)
    }

    def methodMissing(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        super.methodMissing(name,
                            args)
    }

    @Override
    List<String> findOptionalProperties() {
        // groupId will be Mulesoft's if not supplied
        ['groupId']
    }
}
