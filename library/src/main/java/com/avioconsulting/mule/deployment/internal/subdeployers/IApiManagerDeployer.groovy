package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec

interface IApiManagerDeployer {
    /**
     * Creates a definition if it does not exist and updates if it does
     * @param desiredApiManagerDefinition - desired specs
     * @param appVersion - version of application being deployed
     * @return Updated, looked up API definition with ID
     */
    ExistingApiSpec synchronizeApiDefinition(ApiSpec desiredApiManagerDefinition,
                                             String appVersion)

    /**
     * Retrieves the API specification from the API manager server based on asset ID and environment. If more than one
     * API definition is found, will use the apiMajorVersion and instanceLabel to define which one is correct.
     * @param desiredApiManagerDefinition - desired specification
     * @return Existing API from API manager server. If does not exist, will return null
     */
    ExistingApiSpec getExistingApiDefinition(ApiSpec desiredApiManagerDefinition)
}
