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
    @Parameter(defaultValue = 'Run', property = 'deploy.mode')
    private DryRunMode dryRunMode
    @Parameter(required = true, property = 'groovy.file')
    private File groovyFile
    @Parameter(property = 'dsl.params')
    private Map<String, String> dslParams

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        this.log.info "would deploy with ${dryRunMode}"
    }
}
