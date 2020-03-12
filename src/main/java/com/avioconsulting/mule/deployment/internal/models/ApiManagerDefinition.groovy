package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ApiManagerDefinition {
    String exchangeAssetId, exchangeAssetVersion, groupId, endpoint, environment, muleVersion

    def getInstanceLabel() {
        "${environment} - Automated"
    }
}
