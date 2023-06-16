package com.avioconsulting.mule.deployment.internal

import com.avioconsulting.mule.deployment.api.models.deployment.FileBasedAppDeploymentRequest

class TestFileBasedRequest extends FileBasedAppDeploymentRequest {
    private final File zipFile

    TestFileBasedRequest(File zipFile, String appName, String appVersion, String environment) {
        super(zipFile, appName, appVersion, environment)
        this.zipFile = zipFile
    }

    @Override
    File getFile() {
        zipFile
    }

    @Override
    String getAppVersion() {
        '1.2.3'
    }

    @Override
    String getEnvironment() {
        'DEV'
    }
}
