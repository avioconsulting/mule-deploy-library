package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec

interface IPolicyDeployer {
    def synchronizePolicies(ExistingApiSpec apiSpec,
                            List<Policy> desiredPolicies)
}
