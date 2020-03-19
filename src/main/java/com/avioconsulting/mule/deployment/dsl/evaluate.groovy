package com.avioconsulting.mule.deployment.dsl

class Context {
    def muleDeploy(Closure closure) {
        println 'we got called'
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
