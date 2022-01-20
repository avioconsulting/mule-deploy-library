package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.graphql.GetAssetsQuery
import com.avioconsulting.mule.deployment.api.models.Version
import groovy.json.JsonOutput
import okio.Okio
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

trait ApiManagerFunctionality {
    abstract EnvironmentLocator getEnvironmentLocator()

    abstract HttpClientWrapper getClientWrapper()

    abstract ILogger getLogger()

    int getMajorVersionNumber(String majorVersionStringWithV) {
        // Remove any leading characters before the digits
        String version = majorVersionStringWithV.replaceAll("^\\D+", "")
        if(version.indexOf('.') >= 0) {
            version.subSequence(0, version.indexOf('.')) as int
        } else {
            return version as int
        }
    }

    Version parseVersion(String version) {
        String qualifier = null
        String semVer
        if(version.contains('-')) {
            (semVer, qualifier) = version.split('-')
        } else {
            semVer = version
        }

        def split = semVer.split('\\.')
        assert split.size() == 3: "Expected version ${semVer} to have only 3 parts!"
        new Version(Integer.parseInt(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]),
                    qualifier,
            null,
            null)
    }

    String getApiManagerUrl(String restOfUrl,
                            String environment) {
        def environmentId = environmentLocator.getEnvironmentId(environment)
        "${clientWrapper.baseUrl}/apimanager/api/v1/organizations/${clientWrapper.anypointOrganizationId}/environments/${environmentId}/apis${restOfUrl}"
    }

    List<GetAssetsQuery.Asset> getExchangeAssets(String assetId) {
        def query = new GetAssetsQuery(assetId,
                                       clientWrapper.anypointOrganizationId)
        def requestPayload = [
                query    : query.queryDocument(),
                variables: query.variables().marshal()
        ]
        logger.println "Searching for assets for Exchange asset '${assetId}'"
        def request = new HttpPost("${clientWrapper.baseUrl}/graph/api/v1/graphql").with {
            setEntity(new StringEntity(JsonOutput.toJson(requestPayload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        clientWrapper.execute(request).withCloseable { response ->
            clientWrapper.assertSuccessfulResponse(response,
                                                   'GraphQL query')
            def source = Okio.source(response.entity.content)
            def bufferedSource = Okio.buffer(source)
            def dataOptional = query.parse(bufferedSource).data()
            dataOptional.isPresent() ? dataOptional.get().assets : []
        }
    }
}
