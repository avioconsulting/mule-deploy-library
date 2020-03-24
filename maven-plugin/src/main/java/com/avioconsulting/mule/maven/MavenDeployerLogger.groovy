package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.ILogger
import org.apache.maven.plugin.logging.Log

class MavenDeployerLogger implements ILogger {
    private final Log mavenLogger

    MavenDeployerLogger(Log mavenLogger) {
        this.mavenLogger = mavenLogger
    }

    @Override
    def println(String message) {
        mavenLogger.info(message)
    }
}
