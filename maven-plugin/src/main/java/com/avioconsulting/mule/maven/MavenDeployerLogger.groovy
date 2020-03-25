package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.ILogger
import org.apache.maven.plugin.logging.Log

class MavenDeployerLogger implements ILogger {
    private final Log mavenLogger
    private final List<String> logContext

    MavenDeployerLogger(Log mavenLogger) {
        this.mavenLogger = mavenLogger
        this.logContext = []
    }

    def withLogContext(String context,
                       Closure closure) {
        logContext << context
        try {
            closure()
        } finally {
            logContext.pop()
        }
    }

    @Override
    def println(String message) {
        doLog(message,
              false)
    }

    def error(String message) {
        doLog(message,
              true)
    }

    private def doLog(String message,
                      boolean error) {
        def prefix = logContext.join('/')
        message = "${prefix ? prefix + ' - ' : ''}${message}".toString()
        if (error) {
            mavenLogger.error(message)
        } else {
            mavenLogger.info(message)
        }
    }
}
