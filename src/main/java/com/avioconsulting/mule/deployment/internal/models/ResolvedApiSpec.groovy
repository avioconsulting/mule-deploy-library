package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ResolvedApiSpec extends ApiSpec {
    final String exchangeAssetVersion

    ResolvedApiSpec(String exchangeAssetId,
                    String exchangeAssetVersion,
                    String endpoint,
                    String environment,
                    boolean isMule4OrAbove,
                    String instanceLabel = null) {
        super(exchangeAssetId,
              endpoint,
              environment,
              isMule4OrAbove,
              instanceLabel)
        this.exchangeAssetVersion = exchangeAssetVersion
    }
}
