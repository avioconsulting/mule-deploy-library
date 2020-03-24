package com.avioconsulting.mule.deployment.dsl

// see the Maven plugin for an example of using this
// also see https://groovy-lang.org/integrating.html
// needs to be abstract
abstract class MuleDeployScript extends Script {
    def muleDeploy(Closure closure) {
        def context = new MuleDeployContext()
        closure.delegate = context
        closure.call()
        // this allows whatever is 'using' the DSL to get the context and resulting models back out of all of this
        return context
    }
}
