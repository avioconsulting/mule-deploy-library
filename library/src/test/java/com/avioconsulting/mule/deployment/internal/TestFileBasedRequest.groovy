package com.avioconsulting.mule.deployment.internal

import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest

class TestFileBasedRequest extends FileBasedAppDeploymentRequest {
    private final File zipFile

    TestFileBasedRequest(File zipFile) {
        this.zipFile = zipFile
    }

    @Override
    File getFile() {
        zipFile
    }

    @Override
    def setAutoDiscoveryId(String autoDiscoveryId) {
        return null
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
