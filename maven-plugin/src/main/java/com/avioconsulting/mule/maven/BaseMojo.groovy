package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.dsl.DeploymentPackage
import com.avioconsulting.mule.deployment.dsl.MuleDeployContext
import com.avioconsulting.mule.deployment.dsl.MuleDeployScript
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.control.CompilerConfiguration

abstract class BaseMojo extends AbstractMojo {
    @Parameter(property = 'groovy.file', defaultValue = 'muleDeploy.groovy')
    protected File groovyFile
    @Parameter(defaultValue = '${project}')
    protected MavenProject mavenProject

    Map<String, String> getAdditionalProperties() {
        [:]
    }

    protected ParamsWrapper getParamsWrapper() {
        def artifact = mavenProject.attachedArtifacts.find { a ->
            a.classifier == 'mule-application'
        }?.file
        def props = System.getProperties().findAll { String k, v ->
            (k as String).startsWith('muleDeploy.')
        }.collectEntries { k, v ->
            def withoutPrefix = (k as String).replaceFirst('muleDeploy\\.',
                                                           '')
            [withoutPrefix, v]
        }
        if (artifact) {
            log.info "Adding ${artifact} path as projectFile in your DSL"
            props['appArtifact'] = artifact.absolutePath
        }
        new ParamsWrapper(props + additionalProperties)
    }

    protected DeploymentPackage processDsl() {
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
            def wrapper = getParamsWrapper()
            log.info "Will resolve `params` in DSL using: ${wrapper.allProperties}"
            binding.setVariable('params',
                                wrapper)
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
