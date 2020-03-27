package com.avioconsulting.mule.deployment.api.models

import groovy.transform.Immutable

@Immutable
class PomInfo {
    String groupId, artifactId, version
    Map<String, String> props
}
