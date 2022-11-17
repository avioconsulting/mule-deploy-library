package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DeployerFactory
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.api.models.credentials.ConnectedAppCredential
import com.avioconsulting.mule.deployment.api.models.credentials.Credential
import com.avioconsulting.mule.deployment.api.models.credentials.UsernamePasswordCredential
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'deploy', requiresProject = false)
class MuleDeployMojo extends BaseMojo {
    @Parameter(defaultValue = 'Run', property = 'deploy.mode')
    private DryRunMode dryRunMode
    @Parameter(property = 'anypoint.username')
    private String anypointUsername
    @Parameter(property = 'anypoint.password')
    private String anypointPassword
    @Parameter(property = 'anypoint.connected-app.id')
    private String anypointConnectedAppId
    @Parameter(property = 'anypoint.connected-app.secret')
    private String anypointConnectedAppSecret
    @Parameter(property = 'anypoint.org.name')
    private String anypointOrganizationName
    @Parameter(defaultValue = 'DEV', property = 'design.center.deployments')
    private List<String> environmentsToDoDesignCenterDeploymentOn
    private IDeployerFactory deployerFactory = new DeployerFactory()

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        logger.println "DryRun Mode is set to " + dryRunMode
        if (dryRunMode != DryRunMode.OfflineValidate) {
            validateCredentials()
        }
        def deploymentPackage = processDsl()
        logger.println "Successfully processed ${groovyFile} through DSL"
        if (this.dryRunMode == DryRunMode.OfflineValidate) {
            logger.println 'Offline validate was specified, so not deploying'
            return
        }
        logger.println 'Beginning deployment'
        Credential credential = new UsernamePasswordCredential(this.anypointUsername, this.anypointPassword)
        if(this.anypointConnectedAppId != null && this.anypointConnectedAppSecret != null) {
            credential = new ConnectedAppCredential(this.anypointConnectedAppId, this.anypointConnectedAppSecret)
        }
        def deployer = deployerFactory.create(credential,
                                              logger,
                                              this.dryRunMode,
                                              this.anypointOrganizationName,
                                              this.environmentsToDoDesignCenterDeploymentOn)
        try {
            deployer.deployApplication(deploymentPackage.deploymentRequest,
                                       deploymentPackage.apiSpecifications,
                                       deploymentPackage.desiredPolicies,
                                       deploymentPackage.requestedContracts,
                                       deploymentPackage.enabledFeatures)
        }
        catch (e) {
            def exception = e.cause ?: e
            logger.error("Unable to perform deployment because ${exception.class} ${exception.message}")
            throw new Exception('Unable to perform deployment',
                                e)
        }
    }

    void validateCredentials(){
        String format = "%s Run ':help -Pdetail=true' goal for parameter details."
        if((this.anypointUsername == null && this.anypointConnectedAppId == null) ||
                (this.anypointUsername != null && this.anypointConnectedAppId != null)) {
            throw new Exception(String.format(format, "Either (anypointUsername and anypointPassword) " +
                    "or (anypointConnectedAppId and anypointConnectedAppSecret) must be defined."))
        }
        if(this.anypointUsername != null && this.anypointPassword == null) {
            throw new Exception(String.format(format, "'anypointPassword' must be set when using 'anypointUsername'."))
        }
        if(this.anypointConnectedAppId != null && this.anypointConnectedAppSecret == null) {
            throw new Exception(String.format(format, "'anypointConnectedAppSecret' must be set when using 'anypointConnectedAppId'."))
        }
    }
}
