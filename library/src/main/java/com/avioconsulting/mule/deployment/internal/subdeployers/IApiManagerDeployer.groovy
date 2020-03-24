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
}
