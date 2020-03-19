package com.avioconsulting.mule.deployment.api.models

abstract class FileBasedAppDeploymentRequest {
    boolean isMule4Request() {
        isMule4Request(file)
    }

    static boolean isMule4Request(File file) {
        file.name.endsWith('.jar')
    }

    abstract File getFile()

    abstract def setAutoDiscoveryId(String autoDiscoveryId)
}
