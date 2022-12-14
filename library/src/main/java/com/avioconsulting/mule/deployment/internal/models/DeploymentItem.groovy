package com.avioconsulting.mule.deployment.internal.models

class DeploymentItem {

    String id
    String name
    String status
    String appStatus
    String targetId
    String targetProvider

    DeploymentItem (String id, String name, String status, String appStatus, String targetId, String targetProvider) {
        this.id = id
        this.name = name
        this.status = status
        this.appStatus = appStatus
        this.targetId = targetId
        this.targetProvider = targetProvider
    }
}
