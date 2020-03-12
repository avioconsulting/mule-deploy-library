package com.avioconsulting.mule.deployment.internal.models

import groovy.transform.Immutable

@Immutable
class ApiGetEndpointInfo {
    String uri
    boolean muleVersion4OrAbove
}
