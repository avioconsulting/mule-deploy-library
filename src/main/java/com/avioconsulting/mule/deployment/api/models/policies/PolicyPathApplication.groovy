package com.avioconsulting.mule.deployment.api.models.policies

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import groovy.transform.Immutable

@Immutable
class PolicyPathApplication {
    List<HttpMethod> httpMethods
    String regex
}
