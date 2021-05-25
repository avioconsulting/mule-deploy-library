package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.DLBIPWhiteListPolicy
import com.avioconsulting.mule.deployment.dsl.BaseContext

class DLBIPWhiteListPolicyContext extends BaseContext {
    String version
    List<String> ipsToAllow = []
    private PathsContext paths = new PathsContext()
    private Integer dwListIndexToUse = null
    private boolean pathsCalled = false

    DLBIPWhiteListPolicy createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        def errors = findErrors('policies.policy.dlbIPWhiteListPolicy')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your policy spec is not complete. The following errors exist:\n${errorList}")
        }
        new DLBIPWhiteListPolicy(ipsToAllow,
                                 dwListIndexToUse,
                                 version,
                                 pathListing)
    }

    def invokeMethod(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        if (name == 'ipsToAllow' && args.size() == 1 && args[0] instanceof String) {
            ipsToAllow << args[0]
            return
        }
        super.invokeMethod(name,
                           args)
    }

    def ipToAllow(String ip) {
        ipsToAllow << ip
        return null
    }

    @Override
    List<String> findOptionalProperties() {
        ['version']
    }
}
