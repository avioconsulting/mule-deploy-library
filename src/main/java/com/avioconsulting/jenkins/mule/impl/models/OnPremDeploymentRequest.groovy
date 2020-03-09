package com.avioconsulting.jenkins.mule.impl.models

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class OnPremDeploymentRequest implements FileBasedDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    final String appName, targetServerOrClusterName
    /**
     * The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     */
    final String fileName
    /**
     * VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    final String overrideByChangingFileInZip
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    final InputStream app


    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            String fileName,
                            InputStream app,
                            Map<String, String> appProperties = [:]) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        this.environment = environment
        this.appName = appName
        this.targetServerOrClusterName = targetServerOrClusterName
        this.fileName = fileName
        this.appProperties = appProperties
        this.app = app
    }

    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            String fileName,
                            InputStream app,
                            Map<String, String> appProperties,
                            String overrideByChangingFileInZip) {
        this(environment,
             appName,
             targetServerOrClusterName,
             fileName,
             app,
             appProperties)
        this.overrideByChangingFileInZip = overrideByChangingFileInZip
    }

    private String getConfigJson() {
        def map = [
                'mule.agent.application.properties.service': [
                        applicationName: appName,
                        properties     : overrideByChangingFileInZip ? [:] : appProperties
                ]
        ]
        JsonOutput.toJson(map)
    }

    private InputStream getFixedApp() {
        overrideByChangingFileInZip ? modifyZipFileWithNewProperties() : app
    }

    HttpEntity getUpdateHttpPayload() {
        def configJson = getConfigJson()
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody('configuration',
                             configJson)
                .addBinaryBody('file',
                               fixedApp,
                               ContentType.APPLICATION_OCTET_STREAM,
                               fileName)
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
                               fixedApp,
                               ContentType.APPLICATION_OCTET_STREAM,
                               fileName)
                .build()
    }
}
