package com.avioconsulting.mule.deployment.api.models

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class OnPremDeploymentRequest extends FileBasedAppDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    /**
     * Actual name of your application WITHOUT any kind of customer/environment prefix or suffix. Spaces in the name are not allowed and will be rejected.
     */
    final String appName
    /**
     * Name (NOT ID) of server, cluster, or server group
     */
    final String targetServerOrClusterName
    /**
     * The file to deploy. The name of this file will also be used for the Runtime Manager settings pane
     */
    final File file
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * The original filename
     */
    final String originalFileName

    /**
     * Standard deployment request. See properties for parameter info.
     */
    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            File file,
                            Map<String, String> appProperties = [:]) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        this.environment = environment
        this.appName = appName
        this.targetServerOrClusterName = targetServerOrClusterName
        this.appProperties = appProperties
        this.file = file
    }

    private String getConfigJson() {
        def map = [
                'mule.agent.application.properties.service': [
                        applicationName: appName,
                        properties     : appProperties
                ]
        ]
        JsonOutput.toJson(map)
    }

    HttpEntity getUpdateHttpPayload() {
        def configJson = getConfigJson()
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody('configuration',
                             configJson)
                .addBinaryBody('file',
                               this.file,
                               ContentType.APPLICATION_OCTET_STREAM,
                               this.file.name)
                .build()
    }

    HttpEntity getHttpPayload(String serverId) {
        def configJson = getConfigJson()
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody('targetId',
                             serverId)
                .addTextBody('artifactName',
                             appName)
                .addTextBody('configuration',
                             configJson)
                .addBinaryBody('file',
                               this.file,
                               ContentType.APPLICATION_OCTET_STREAM,
                               this.file.name)
                .build()
    }

    @Override
    def setAutoDiscoveryId(String autoDiscoveryId) {
        return null
    }
}
