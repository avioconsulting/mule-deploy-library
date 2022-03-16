package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DeployerFactory
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployerFactory
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
    @Parameter(property = 'anypoint.connected-app-id')
    private String anypointConnectedAppId
    @Parameter(property = 'anypoint.connected-app-secret')
    private String anypointConnectedAppSecret
    @Parameter(property = 'anypoint.org.name')
    private String anypointOrganizationName
    @Parameter(defaultValue = 'DEV', property = 'design.center.deployments')
    private List<String> environmentsToDoDesignCenterDeploymentOn
    private IDeployerFactory deployerFactory = new DeployerFactory()

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        if (dryRunMode != DryRunMode.OfflineValidate && !(anypointUsername || anypointPassword)) {
            throw new Exception("In order to ${dryRunMode}, credentials must be supplied via the anypointUsername <config> item/anypoint.username property and the anypointPassword <config> item/anypoint.password property")
        }
        def deploymentPackage = processDsl()
        logger.println "Successfully processed ${groovyFile} through DSL"
        if (this.dryRunMode == DryRunMode.OfflineValidate) {
            logger.println 'Offline validate was specified, so not deploying'
            return
        }
        logger.println 'Beginning deployment'
        def deployer = deployerFactory.create(this.anypointUsername,
                                              this.anypointPassword,
                                              this.anypointConnectedAppId,
                                              this.anypointConnectedAppSecret,
                                              logger,
                                              this.dryRunMode,
                                              this.anypointOrganizationName,
                                              this.environmentsToDoDesignCenterDeploymentOn)
        if (this.anypointUsername == null && this.anypointPassword == null && this.anypointConnectedAppId == null && this.anypointConnectedAppSecret == null) {
            throw new Exception("Either anypoint.username and anypoint.password or anypoint.connected-app-id and anypoint.connected-app-secret must be defined..")
        }
        try {
            deployer.deployApplication(deploymentPackage.deploymentRequest,
                                       deploymentPackage.apiSpecifications,
                                       deploymentPackage.desiredPolicies,
                                       deploymentPackage.enabledFeatures)
        }
        catch (e) {
            def exception = e.cause ?: e
            logger.error("Unable to perform deployment because ${exception.class} ${exception.message}")
            throw new Exception('Unable to perform deployment',
                                e)
        }
    }
}
