package com.avioconsulting.mule.deployment.dsl

class MuleDeployContext {
    def cloudHubApplication(Closure closure) {
        def context = new CloudhubContext()
        closure.delegate = context
        closure.call()
        def request = context.createDeploymentRequest()
        println "got request ${request}"
    }
}
