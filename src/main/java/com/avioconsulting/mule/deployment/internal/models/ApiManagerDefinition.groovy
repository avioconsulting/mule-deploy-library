package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ApiManagerDefinition {
    String exchangeAssetId, exchangeAssetVersion, endpoint, environment, instanceLabel
    boolean isMule4OrAbove

    static ApiManagerDefinition createWithDefaultLabel(String exchangeAssetId,
                                                       exchangeAssetVersion,
                                                       endpoint,
                                                       environment,
                                                       boolean isMule4OrAbove) {
        new ApiManagerDefinition(exchangeAssetId,
                                 exchangeAssetVersion,
                                 endpoint,
                                 environment,
                                 "${environment} - Automated",
                                 isMule4OrAbove)
    }
}
