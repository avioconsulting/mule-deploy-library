package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ExistingApiSpec extends ResolvedApiSpec {
    final String id

    ExistingApiSpec(String id,
                    String exchangeAssetId,
                    String exchangeAssetVersion,
                    String endpoint,
                    String environment,
                    boolean isMule4OrAbove) {
        super(exchangeAssetId,
              exchangeAssetVersion,
              endpoint,
              environment,
              isMule4OrAbove)
        this.id = id
    }

    ExistingApiSpec(String environment,
                    ApiGetResponse getResponse) {
        super(getResponse.assetId,
              getResponse.assetVersion,
              getResponse.endpoint.uri,
              // we want the label, not the GUID
              environment,
              getResponse.endpoint.muleVersion4OrAbove,
              getResponse.instanceLabel)
        this.id = getResponse.id
    }

    // aid in comparing using @EqualsAndHashCode annotation above
    ResolvedApiSpec getWithoutId() {
        return new ResolvedApiSpec(this,
                                   this.exchangeAssetVersion)
    }
}
