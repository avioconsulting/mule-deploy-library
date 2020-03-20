package com.avioconsulting.mule.deployment.dsl

abstract class BaseContext {
    private List<String> fieldsThatHaveBeenSet = []

    def findErrors(String prefix = null) {
        this.getProperties().findAll { k, v ->
            k != 'class' && v == null
        }.collect { k, v ->
            prefix ? "${prefix}.${k}" : k
        }.sort().collect { error -> "- ${error} missing" }
    }

    def methodMissing(String name, def args) {
        if (fieldsThatHaveBeenSet.contains(name)) {
            throw new Exception("Field '${name}' has already been set!")
        }
        fieldsThatHaveBeenSet << name
        def argument = args[0]
        if (argument instanceof Closure) {
            def context = this[name]
            argument.delegate = context
            // avoid trying to resolve via higher level closures
            argument.resolveStrategy = Closure.DELEGATE_FIRST
            argument.call()
        } else {
            this[name] = argument
        }
    }
}
