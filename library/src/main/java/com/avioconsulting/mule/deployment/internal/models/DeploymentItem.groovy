package com.avioconsulting.mule.deployment.internal.models

class DeploymentItem {

    String id
    String name
    String deploymentStatus
    String appStatus
    String targetId
    String targetProvider

    DeploymentItem (String id, String name, String deploymentStatus, String appStatus, String targetId, String targetProvider) {
        this.id = id
        this.name = name
        this.deploymentStatus = deploymentStatus
        this.appStatus = appStatus
        this.targetId = targetId
        this.targetProvider = targetProvider
    }
}
