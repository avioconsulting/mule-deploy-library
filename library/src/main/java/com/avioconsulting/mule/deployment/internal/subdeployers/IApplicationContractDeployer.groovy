package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.RequestedContract
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec

interface IApplicationContractDeployer {

    def synchronizeApplicationContracts(ExistingApiSpec existingApiManagerDefinition, List<RequestedContract> applicationsNeedingContracts)

}