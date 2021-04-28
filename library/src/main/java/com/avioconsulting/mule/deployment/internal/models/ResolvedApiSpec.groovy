package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(callSuper = true)
class ResolvedApiSpec extends ApiSpec {
    final String exchangeAssetVersion

    ResolvedApiSpec(String exchangeAssetId,
                    String exchangeAssetVersion,
                    String endpoint,
                    String environment,
                    String apiMajorVersion,
                    boolean isMule4OrAbove,
                    String instanceLabel = null) {
        super(exchangeAssetId,
              endpoint,
              environment,
              apiMajorVersion,
              isMule4OrAbove,
              instanceLabel)
        this.exchangeAssetVersion = exchangeAssetVersion
    }

    ResolvedApiSpec(ApiSpec spec,
                    String exchangeAssetVersion) {
        super(spec.exchangeAssetId,
              spec.endpoint,
              spec.environment,
              spec.apiMajorVersion,
              spec.isMule4OrAbove,
              spec.instanceLabel)
        this.exchangeAssetVersion = exchangeAssetVersion
    }
}
