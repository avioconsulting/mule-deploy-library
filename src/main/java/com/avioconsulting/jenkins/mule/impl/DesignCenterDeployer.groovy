package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.FileBasedDeploymentRequest
import com.avioconsulting.jenkins.mule.impl.models.RamlFile

class DesignCenterDeployer {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger

    DesignCenterDeployer(HttpClientWrapper clientWrapper,
                         PrintStream logger) {

        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    List<RamlFile> getRamlFilesFromApp(FileBasedDeploymentRequest deploymentRequest) {
        []
    }
}
