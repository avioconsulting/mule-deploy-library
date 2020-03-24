package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DeployerFactory
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.dsl.MuleDeployContext
import com.avioconsulting.mule.deployment.dsl.MuleDeployScript
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.groovy.control.CompilerConfiguration

@Mojo(name = 'muleDeploy', requiresProject = false)
class MuleDeployMojo extends AbstractMojo {
    @Parameter(required = true, property = 'anypoint.username')
    private String anypointUsername
    @Parameter(required = true, property = 'anypoint.password')
    private String anypointPassword
    @Parameter(property = 'anypoint.org.name')
    private String anypointOrganizationName
    @Parameter(defaultValue = 'Run', property = 'deploy.mode')
    private DryRunMode dryRunMode
    @Parameter(required = true, property = 'groovy.file')
    private File groovyFile
    @Parameter(defaultValue = 'DEV', property = 'design.center.deployments')
    private List<String> environmentsToDoDesignCenterDeploymentOn

    private IDeployerFactory deployerFactory = new DeployerFactory()

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        def compilerConfig = new CompilerConfiguration().with {
            scriptBaseClass = MuleDeployScript.name
            it
        }
        def shell = new GroovyShell(this.class.classLoader,
                                    compilerConfig)
        def context = shell.evaluate(groovyFile) as MuleDeployContext
        def deploymentPackage = context.createDeploymentPackage()
        log.info "Successfully processed ${groovyFile} through DSL"
        def logger = new MavenDeployerLogger(this.log)
        def deployer = deployerFactory.create(this.anypointUsername,
                                              this.anypointPassword,
                                              logger,
                                              this.dryRunMode,
                                              this.anypointOrganizationName,
                                              this.environmentsToDoDesignCenterDeploymentOn)
        if (this.dryRunMode == DryRunMode.OfflineValidate) {
            log.info 'Offline validate was specified, so not deloying'
            return
        }
        log.info 'Beginning deployment'
        deployer.deployApplication(deploymentPackage.deploymentRequest,
                                   deploymentPackage.apiSpecification,
                                   deploymentPackage.desiredPolicies,
                                   deploymentPackage.enabledFeatures)
    }
}
