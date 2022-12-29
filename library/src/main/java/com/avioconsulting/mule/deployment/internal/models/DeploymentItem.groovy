package com.avioconsulting.mule.deployment.internal.models

class DeploymentItem {

    String id
    String name
    String deploymentStatus
    String appStatus
    String targetId

    DeploymentItem (String id, String name, String deploymentStatus, String appStatus, String targetId) {
        this.id = id
        this.name = name
        this.deploymentStatus = deploymentStatus
        this.appStatus = appStatus
        this.targetId = targetId
    }
}
