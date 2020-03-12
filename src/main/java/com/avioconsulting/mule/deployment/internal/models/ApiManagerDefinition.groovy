package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ApiManagerDefinition {
    String exchangeAssetId, exchangeAssetVersion, endpoint, environment, muleVersion

    def getInstanceLabel() {
        "${environment} - Automated"
    }
}
