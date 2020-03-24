package com.avioconsulting.mule

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = 'muleDeploy', requiresProject = false)
class MuleDeployMojo extends AbstractMojo {
    private String anypointUsername
    private String anypointPassword
    private String dryRunMode
    private File groovyFile
    private Map<String, String> dslParams

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {

    }
}
