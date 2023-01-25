package com.avioconsulting.mule.deployment.api.models

enum UpdateStrategy {
    /**
     * Maintains availability by updating replicas incrementally.
     * Requires one additional replica’s worth of resources to succeed.
     */
    rolling,
    /**
     * Terminates replicas before re-deployment.
     * Re-deployment is quicker than rolling and doesn’t require additional resources.
     */
    recreate
}
