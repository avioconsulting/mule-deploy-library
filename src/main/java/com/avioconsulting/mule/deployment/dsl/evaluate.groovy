package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import org.codehaus.groovy.control.CompilerConfiguration

abstract class OurScript extends Script {
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
        def request = context.deploymentRequest
        println "got request ${request}"
    }
}

class CloudhubContext {
    private String environmentName

    CloudhubDeploymentRequest getDeploymentRequest() {
        new CloudhubDeploymentRequest(this.environmentName,
                                      'foo',
                                      new CloudhubWorkerSpecRequest('4.2.2'),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null)
    }

    def environment(String environmentName) {
        this.environmentName = environmentName
    }

    def methodMissing(String name, def args) {
        println "got call ${name}"
    }
}

def compilerConfig = new CompilerConfiguration().with {
    scriptBaseClass = OurScript.name
    it
}
def shell = new GroovyShell(this.class.classLoader,
                            compilerConfig)
def file = new File(args[0])
assert file.absoluteFile.exists()
def code = file.text
def r = shell.evaluate(code)
