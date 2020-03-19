package com.avioconsulting.mule.deployment.dsl

class AutodiscoveryContext {
    String clientId
    String clientSecret

    def clientId(String clientId) {
        this.clientId = clientId
    }

    def clientSecret(String clientSecret) {
        this.clientSecret = clientSecret
    }
}
