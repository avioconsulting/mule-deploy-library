package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class AppStatusPackage {
    AppStatus appStatus
    DeploymentUpdateStatus deploymentUpdateStatus

    @Override
    String toString() {
        "ApplicationStatus(MainStatus=${appStatus}, DeploymentUpdateStatus=${deploymentUpdateStatus})"
    }
}
