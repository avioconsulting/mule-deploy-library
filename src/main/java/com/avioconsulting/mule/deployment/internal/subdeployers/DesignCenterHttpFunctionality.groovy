package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.http.LazyHeader
import org.apache.http.client.methods.HttpUriRequest

trait DesignCenterHttpFunctionality {
    def executeDesignCenterRequest(HttpClientWrapper clientWrapper,
                                   HttpUriRequest request,
                                   String failureContext,
                                   Closure resultHandler = null) {
        request.with {
            setHeader('X-ORGANIZATION-ID',
                      clientWrapper.anypointOrganizationId)
            setHeader('cache-control',
                      'no-cache')
            // At the time we run this, we might not yet have the ownerGuid value since that happens during authentication
            setHeader(new LazyHeader('X-OWNER-ID',
                                     {
                                         clientWrapper.ownerGuid
                                     }))
        }
        return clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                                    failureContext,
                                                                    resultHandler)
    }

    def getMasterUrl(HttpClientWrapper clientWrapper,
                     String projectId) {
        "${clientWrapper.baseUrl}/designcenter/api-designer/projects/${projectId}/branches/master"
    }
}
