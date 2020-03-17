package com.avioconsulting.mule.deployment.api.models

import groovy.transform.Immutable

@Immutable
class PolicyPathApplication {
    List<HttpMethod> httpMethods
    String regex
}
