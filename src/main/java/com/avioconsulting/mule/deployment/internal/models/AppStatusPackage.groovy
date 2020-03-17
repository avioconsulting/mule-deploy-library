package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class AppStatusPackage {
    AppStatus appStatus
    DeploymentUpdateStatus deploymentUpdateStatus
}
