package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import org.apache.http.client.methods.HttpUriRequest

abstract class BaseDeployer {
    protected final EnvironmentLocator environmentLocator
    protected final ILogger logger
    protected final int retryIntervalInMs
    protected final int maxTries
    protected final HttpClientWrapper clientWrapper
    protected final DryRunMode dryRunMode
    protected final String RUNTIME_FABRIC_TARGET_TYPE = "runtime-fabric"
    protected final String PRIVATE_SPACE_TARGET_TYPE = "private-space"
    protected final String SHARED_SPACE_TARGET_TYPE = "shared-space"

    BaseDeployer(int retryIntervalInMs,
                 int maxTries,
                 ILogger logger,
                 HttpClientWrapper clientWrapper,
                 EnvironmentLocator environmentLocator,
                 DryRunMode dryRunMode) {
        this.dryRunMode = dryRunMode
        this.clientWrapper = clientWrapper
        this.logger = logger
        this.maxTries = maxTries
        this.retryIntervalInMs = retryIntervalInMs
        this.environmentLocator = environmentLocator
    }

    def addStandardStuff(HttpUriRequest request,
                         String environmentName) {
        def environmentId = environmentLocator.getEnvironmentId(environmentName)
        request.addHeader('X-ANYPNT-ENV-ID',
                          environmentId)
        request.addHeader('X-ANYPNT-ORG-ID',
                          clientWrapper.anypointOrganizationId)
    }
}
