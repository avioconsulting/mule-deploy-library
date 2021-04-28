package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(callSuper = true)
@ToString
class ExistingApiSpec extends ResolvedApiSpec {
    final String id

    ExistingApiSpec(String id,
                    String exchangeAssetId,
                    String exchangeAssetVersion,
                    String endpoint,
                    String environment,
                    String apiMajorVersion,
                    boolean isMule4OrAbove) {
        super(exchangeAssetId,
              exchangeAssetVersion,
              endpoint,
              environment,
              apiMajorVersion,
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
              getResponse.productVersion,
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
