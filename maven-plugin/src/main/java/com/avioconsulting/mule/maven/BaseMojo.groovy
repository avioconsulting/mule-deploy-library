package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.dsl.DeploymentPackage
import com.avioconsulting.mule.deployment.dsl.MuleDeployContext
import com.avioconsulting.mule.deployment.dsl.MuleDeployScript
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.control.CompilerConfiguration

abstract class BaseMojo extends AbstractMojo {
    @Parameter(defaultValue = 'Run', property = 'deploy.mode')
    protected DryRunMode dryRunMode
    @Parameter(required = true, property = 'groovy.file')
    protected File groovyFile
    @Parameter(defaultValue = '${project}')
    protected MavenProject mavenProject

    protected DeploymentPackage processDsl(File artifact,
                                           ParamsWrapper paramsWrapper) {
        try {
            // allows us to use our 'MuleDeployScript'/MuleDeployContext class to interpret the user's DSL file
            def compilerConfig = new CompilerConfiguration().with {
                scriptBaseClass = MuleDeployScript.name
                it
            }
            def shell = new GroovyShell(this.class.classLoader,
                                        compilerConfig)
            // in case they specify additional runtime settings not in source control via -D maven command line args
            def binding = new Binding()
            binding.setVariable('params',
                                paramsWrapper)
            binding.setVariable('projectFile',
                                artifact?.absolutePath)
            shell.context = binding
            // last line of MuleDeployScript.muleDeploy method returns this
            def context = shell.evaluate(groovyFile) as MuleDeployContext
            return context.createDeploymentPackage()
        }
        catch (e) {
            def exception = e.cause ?: e
            log.error("Unable to process DSL because ${exception.class} ${exception.message}")
            throw new Exception('Unable to process DSL',
                                e)
        }
    }
}
