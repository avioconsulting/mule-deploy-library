package com.avioconsulting.mule.deployment.dsl

abstract class MuleDeployScript extends Script {
    def muleDeploy(Closure closure) {
        def context = new MuleDeployContext()
        closure.delegate = context
        closure.call()
    }
}
