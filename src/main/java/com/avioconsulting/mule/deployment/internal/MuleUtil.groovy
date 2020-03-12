package com.avioconsulting.mule.deployment.internal

class MuleUtil {
    static String getFileName(String appName,
                              String appVersion,
                              String muleVersion) {
        return muleVersion.startsWith("3") ?
                String.format("%s-%s.zip",
                              appName,
                              appVersion) :
                String.format("%s-%s-mule-application.jar",
                              appName,
                              appVersion)
    }
}
