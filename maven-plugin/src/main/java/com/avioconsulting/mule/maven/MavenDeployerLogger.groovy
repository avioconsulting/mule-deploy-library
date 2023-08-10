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
        doLog(message)
    }

    def error(String message,
              Throwable error = null) {
        doLog(message,
                true,
                error)
    }

    private def doLog(String message,
                      boolean isError = false,
                      Throwable exception = null) {
        def prefix = logContext.join('/')
        message = "${prefix ? prefix + ' - ' : ''}${message}".toString()
        if (isError && exception) {
            mavenLogger.error(message,
                    exception)
        } else if (isError && !exception) {
            mavenLogger.error(message)
        } else {
            mavenLogger.info(message)
        }
    }
}
