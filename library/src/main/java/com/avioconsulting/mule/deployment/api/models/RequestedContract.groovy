package com.avioconsulting.mule.deployment.api.models

import groovy.json.JsonBuilder

class RequestedContract {
    String ourClientApplicationName
    String ourClientApplicationId, environmentId, organizationId, exchangeAssetId, version, versionGroup, apiManagerId

    RequestedContract(ourClientApplicationName, exchangeAssetId) {
        this.ourClientApplicationName = ourClientApplicationName
        this.exchangeAssetId = exchangeAssetId
    }

    String buildJsonRequest(Map jsonMap = new HashMap()) {
        def responseMap = [
                apiId         : this.apiManagerId,
                environmentId : this.environmentId,
                instanceType  : "api",
                acceptedTerms : true,
                organizationId: this.organizationId,
                groupId       : this.organizationId,
                assetId       : this.exchangeAssetId,
                version       : this.version,
                versionGroup  : this.versionGroup
        ]
        responseMap.putAll(jsonMap)
        return new JsonBuilder(responseMap).toString()
    }
}
