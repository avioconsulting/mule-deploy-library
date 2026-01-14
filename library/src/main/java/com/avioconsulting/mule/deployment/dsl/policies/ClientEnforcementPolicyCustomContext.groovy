package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.ClientEnforcementPolicyCustom
import com.avioconsulting.mule.deployment.dsl.BaseContext

class ClientEnforcementPolicyCustomContext extends BaseContext {
    String version
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false
    String clientIdExpression
    String clientSecretExpression

    ClientEnforcementPolicyCustom createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        new ClientEnforcementPolicyCustom( pathListing,
                this.clientIdExpression, this.clientSecretExpression,
                this.version)
    }

    def invokeMethod(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        super.invokeMethod(name,
                args)
    }

    @Override
    List<String> findOptionalProperties() {
        return null
    }
}
