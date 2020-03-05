package com.avioconsulting.jenkins.mule.impl

enum OnPremDeploymentStatus {
    RECEIVED,
    STARTING,
    FAILED,
    STARTED,
    STOPPING,
    UNKNOWN,
    NOT_FOUND,
    NEVER_STARTED
}
