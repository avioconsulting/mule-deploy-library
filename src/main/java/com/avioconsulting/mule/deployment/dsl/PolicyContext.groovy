package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.policies.Policy

class PolicyContext extends BaseContext {
    String groupId, assetId, version
    Map<String, String> config

    Policy createPolicyModel() {
        new Policy(this.groupId ?: Policy.mulesoftGroupId,
                   this.assetId,
                   this.version,
                   this.config)
    }

    @Override
    List<String> findOptionalProperties() {
        return null
    }
}
