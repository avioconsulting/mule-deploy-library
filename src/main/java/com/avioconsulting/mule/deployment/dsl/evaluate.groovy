package com.avioconsulting.mule.deployment.dsl

class Context {
    def muleDeploy(Closure closure) {
        def context = new MuleDeployContext()
        closure.delegate = context
        closure.call()
    }
}

class MuleDeployContext {
    def cloudHubApplication(Closure closure) {
        def context = new CloudhubContext()
        closure.delegate = context
        closure.call()
    }
}

class CloudhubContext {
    def environment(String environmentName) {

    }

    def methodMissing(String name, def args) {
        println "got call ${name}"
    }
}

def shell = new GroovyShell()
def file = new File(args[0])
assert file.absoluteFile.exists()
def code = file.text
code = "{-> ${code}}"
def closure = shell.evaluate(code)
closure.delegate = new Context()
closure()
