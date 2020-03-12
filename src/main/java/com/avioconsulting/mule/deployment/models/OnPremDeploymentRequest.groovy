package com.avioconsulting.mule.deployment.models

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
     * THe original filename, before any propert
     */
    final String originalFileName

    private boolean modifiedPropertiesViaZip

    /**
     * Standard deployment request. See properties for parameter info.
     */
    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            File file,
                            Map<String, String> appProperties = [:]) {
        this(environment,
             appName,
             targetServerOrClusterName,
             file,
             appProperties,
             null)
    }

    /**
     * Construct a request designed to override properties in a file. This is a niche case. See properties for parameter info.
     * @param overrideByChangingFileInZip VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            File file,
                            Map<String, String> appProperties,
                            String overrideByChangingFileInZip) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        this.environment = environment
        this.appName = appName
        this.targetServerOrClusterName = targetServerOrClusterName
        this.appProperties = appProperties
        this.file = overrideByChangingFileInZip ? modifyFileProps(overrideByChangingFileInZip,
                                                                  appProperties,
                                                                  file) : file
        this.modifiedPropertiesViaZip = overrideByChangingFileInZip != null
    }

    private String getConfigJson() {
        def map = [
                'mule.agent.application.properties.service': [
                        applicationName: appName,
                        // don't want to use ARM props if we took care of this in a file
                        properties     : modifiedPropertiesViaZip ? [:] : appProperties
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
}
