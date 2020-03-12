package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import org.apache.http.client.methods.HttpUriRequest

abstract class BaseDeployer {
    protected final EnvironmentLocator environmentLocator
    protected final PrintStream logger
    protected final int retryIntervalInMs
    protected final int maxTries
    protected final HttpClientWrapper clientWrapper

    BaseDeployer(int retryIntervalInMs,
                 int maxTries,
                 PrintStream logger,
                 HttpClientWrapper clientWrapper,
                 EnvironmentLocator environmentLocator) {
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
