package com.avioconsulting.mule.deployment.internal.subdeployers


import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import org.apache.http.client.methods.HttpPost

class DesignCenterLock implements Closeable, DesignCenterHttpFunctionality {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger
    private final String projectId
    private final String masterUrl

    DesignCenterLock(HttpClientWrapper clientWrapper,
                     PrintStream logger,
                     String projectId) {
        this.projectId = projectId
        this.logger = logger
        this.clientWrapper = clientWrapper
        masterUrl = getMasterUrl(clientWrapper,
                                 projectId)
        logger.println 'Acquiring Design Center Lock'
        executeDesignCenterRequest(clientWrapper,
                                   new HttpPost("${masterUrl}/acquireLock"),
                                   'Acquire design center lock')
    }

    @Override
    void close() throws IOException {
        logger.println 'Releasing Design Center Lock'
        executeDesignCenterRequest(clientWrapper,
                                   new HttpPost("${masterUrl}/releaseLock"),
                                   'Release design center lock')
    }
}
