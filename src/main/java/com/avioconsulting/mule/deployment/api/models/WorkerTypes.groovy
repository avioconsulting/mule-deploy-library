package com.avioconsulting.mule.deployment.api.models

// right now, all of the 'codes' Mulesoft expects are valid ENUM identifiers. If that changes
// an approach similar to the AwsRegions ENUM in this project can be followed
enum WorkerTypes {
    /**
     * 0.1 vCores
     */
    Micro,
    /**
     * 0.2 vCores
     */
    Small,
    /**
     * 1 vCore
     */
    Medium,
    /**
     * 2 vCores
     */
    Large,
    /**
     * 4 vCores
     */
    xLarge
}
