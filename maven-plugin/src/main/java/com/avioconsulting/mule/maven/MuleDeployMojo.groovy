package com.avioconsulting.mule.maven


import com.avioconsulting.mule.deployment.api.DryRunMode
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

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
        this.log.info "would deploy with ${dryRunMode}"
        environmentsToDoDesignCenterDeploymentOn.each { e ->
            log.info "with env ${e}"
        }
    }
}
