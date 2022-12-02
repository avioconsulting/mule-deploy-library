package com.avioconsulting.mule.deployment.api.models

enum UpdateStrategies {
    /**
     * 0.1 vCores
     */
    rolling,
    /**
     * 0.2 vCores
     */
    recreate
}
