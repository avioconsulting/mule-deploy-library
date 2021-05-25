package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import org.apache.http.client.methods.HttpPost

class DesignCenterLock implements Closeable, DesignCenterHttpFunctionality {
    private final HttpClientWrapper clientWrapper
    private final ILogger logger
    private final String projectId
    private final String branchUrl

    DesignCenterLock(HttpClientWrapper clientWrapper,
                     ILogger logger,
                     String projectId,
                     String branch) {
        this.projectId = projectId
        this.logger = logger
        this.clientWrapper = clientWrapper
        this.branchUrl = getBranchUrl(clientWrapper,
                                      projectId,
                                      branch)
        logger.println 'Acquiring Design Center Lock'
        executeDesignCenterRequest(clientWrapper,
                                   new HttpPost("${branchUrl}/acquireLock"),
                                   'Acquire design center lock')
    }

    @Override
    void close() throws IOException {
        logger.println 'Releasing Design Center Lock'
        executeDesignCenterRequest(clientWrapper,
                                   new HttpPost("${branchUrl}/releaseLock"),
                                   'Release design center lock')
    }
}
