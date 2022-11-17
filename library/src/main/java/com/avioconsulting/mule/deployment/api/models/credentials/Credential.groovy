package com.avioconsulting.mule.deployment.api.models.credentials;

/**
 * Abstract Base class for defining anypoint platform access credentials
 */
abstract class Credential {

    abstract String getPrincipal()
}
