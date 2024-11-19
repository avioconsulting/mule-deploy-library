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
    @Lazy
    protected MavenDeployerLogger logger = {
        new MavenDeployerLogger(this.log)
    }()

    Map<String, String> getAdditionalProperties() {
        [:]
    }

    /**
     * Remove the prefix "muleDeploy." from all command-line parameters that starts with this string
     * @return #{ParamsWrapper} - All properties from command-line (already formatted) plus the additionalProperties var
     */
    protected ParamsWrapper getParamsWrapper() {
        def artifact = mavenProject.artifact

        if (artifact == null || artifact.classifier != 'mule-application') {
            logger.println "Didn't find mavenProject.artifact with appropriate classifier. Falling back to attached artifacts."
            artifact = mavenProject.attachedArtifacts.find { a ->
                a.classifier == 'mule-application'
            }
        }

        def props = [:]

        if (artifact) {
            logger.println "Adding ${artifact.file} path as appArtifact in your DSL and setting groupId, artifactId and artifactVersion"
            props['appArtifact'] = artifact.file.absolutePath
            props['groupId'] = artifact.groupId
            props['artifactId'] = artifact.id
            props['artifactVersion'] = artifact.version
        }

        def sysProps = System.getProperties().findAll { String k, v ->
            (k as String).startsWith('muleDeploy.') || (k as String).startsWith('md.')
        }.collectEntries { k, v ->
            def withoutPrefix = (k as String).replaceFirst('muleDeploy\\.',
                    '')
            withoutPrefix = withoutPrefix.replaceFirst('md\\.',
                    '')
            [withoutPrefix, v]
        }



        new ParamsWrapper(props + sysProps + additionalProperties)
    }

    protected DeploymentPackage processDsl() {
        try {
            // allows us to use our 'MuleDeployScript'/MuleDeployContext class to interpret the user's DSL file
            def compilerConfig = new CompilerConfiguration().with {
                scriptBaseClass = MuleDeployScript.name
                it
            }
            // in case they specify additional runtime settings not in source control via -D maven command line args
            def binding = new Binding()
            def wrapper = getParamsWrapper()
            logger.println "Will resolve `params` in DSL using: ${wrapper.allProperties}"
            binding.setVariable('params',
                    wrapper)
            def shell = new GroovyShell(this.class.classLoader,
                    binding,
                    compilerConfig)
            // last line of MuleDeployScript.muleDeploy method returns this
            def context = shell.evaluate(groovyFile) as MuleDeployContext
            return context.createDeploymentPackage()
        }
        catch (e) {
            def exception = e.cause ?: e
            logger.error("Unable to process DSL because ${exception.class} ${exception.message}",
                    exception)
            throw new Exception('Unable to process DSL',
                    e)
        }
    }
}
