package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DeployerFactory
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.dsl.DeploymentPackage
import com.avioconsulting.mule.deployment.dsl.MuleDeployContext
import com.avioconsulting.mule.deployment.dsl.MuleDeployScript
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.control.CompilerConfiguration

@Mojo(name = 'muleDeploy', requiresProject = false)
class MuleDeployMojo extends AbstractMojo {
    @Parameter(property = 'anypoint.username')
    private String anypointUsername
    @Parameter(property = 'anypoint.password')
    private String anypointPassword
    @Parameter(property = 'anypoint.org.name')
    private String anypointOrganizationName
    @Parameter(defaultValue = 'Run', property = 'deploy.mode')
    private DryRunMode dryRunMode
    @Parameter(required = true, property = 'groovy.file')
    private File groovyFile
    @Parameter(defaultValue = 'DEV', property = 'design.center.deployments')
    private List<String> environmentsToDoDesignCenterDeploymentOn
    @Parameter(defaultValue = '${project}')
    private MavenProject mavenProject

    private IDeployerFactory deployerFactory = new DeployerFactory()

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        def artifact = mavenProject.attachedArtifacts.find { a ->
            a.classifier == 'mule-application'
        }?.file
        if (artifact) {
            log.info "Adding ${artifact} path as projectFile in your DSL"
        }
        if (dryRunMode != DryRunMode.OfflineValidate && !(anypointUsername || anypointPassword)) {
            throw new Exception("In order to ${dryRunMode}, credentials must be supplied via the anypointUsername <config> item/anypoint.username property and the anypointPassword <config> item/anypoint.password property")
        }
        def deploymentPackage = processDsl(artifact)
        log.info "Successfully processed ${groovyFile} through DSL"
        def logger = new MavenDeployerLogger(this.log)
        def deployer = deployerFactory.create(this.anypointUsername,
                                              this.anypointPassword,
                                              logger,
                                              this.dryRunMode,
                                              this.anypointOrganizationName,
                                              this.environmentsToDoDesignCenterDeploymentOn)
        if (this.dryRunMode == DryRunMode.OfflineValidate) {
            log.info 'Offline validate was specified, so not deploying'
            return
        }
        log.info 'Beginning deployment'
        try {
            deployer.deployApplication(deploymentPackage.deploymentRequest,
                                       deploymentPackage.apiSpecification,
                                       deploymentPackage.desiredPolicies,
                                       deploymentPackage.enabledFeatures)
        }
        catch (e) {
            def exception = e.cause ?: e
            log.error("Unable to perform deployment because ${exception.class} ${exception.message}")
            throw new Exception('Unable to perform deployment',
                                e)
        }
    }

    private DeploymentPackage processDsl(File artifact) {
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
                                new ParamsWrapper())
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
