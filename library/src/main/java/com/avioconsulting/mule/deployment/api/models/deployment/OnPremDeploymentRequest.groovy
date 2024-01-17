package com.avioconsulting.mule.deployment.api.models.deployment

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class OnPremDeploymentRequest extends FileBasedAppDeploymentRequest {
    /**
     * Name (NOT ID) of server, cluster, or server group
     */
    final String targetServerOrClusterName
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties

    /**
     * Standard deployment request. See properties for parameter info.
     */
    OnPremDeploymentRequest(String environment,
                            String targetServerOrClusterName,
                            File file,
                            ApplicationName appName,
                            String appVersion = null,
                            Map<String, String> appProperties = [:]) {
        super(file, appName, appVersion, environment)
        this.targetServerOrClusterName = targetServerOrClusterName
        this.appProperties = appProperties
    }

    private String getConfigJson() {
        def map = [
                'mule.agent.application.properties.service': [
                        applicationName: appName.baseAppName,
                        properties     : appProperties + this.autoDiscoveries
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
                             appName.baseAppName)
                .addTextBody('configuration',
                             configJson)
                .addBinaryBody('file',
                               this.file,
                               ContentType.APPLICATION_OCTET_STREAM,
                               this.file.name)
                .build()
    }
}
