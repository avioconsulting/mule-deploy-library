package com.avioconsulting.mule.deployment.dsl

import org.codehaus.groovy.control.CompilerConfiguration

class TopLevelRunner {
    static void main(String[] args) {
        def compilerConfig = new CompilerConfiguration().with {
            scriptBaseClass = MuleDeployScript.name
            it
        }
        def shell = new GroovyShell(this.class.classLoader,
                                    compilerConfig)
        def file = new File(args[0])
        assert file.absoluteFile.exists()
        def code = file.text
        shell.evaluate(code)
    }
}
