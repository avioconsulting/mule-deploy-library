package com.avioconsulting.mule.cli

import com.avioconsulting.mule.deployment.api.DeployerFactory
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.credentials.ConnectedAppCredential
import com.avioconsulting.mule.deployment.api.models.credentials.Credential
import com.avioconsulting.mule.deployment.api.models.credentials.UsernamePasswordCredential
import com.avioconsulting.mule.deployment.dsl.DeploymentPackage
import com.avioconsulting.mule.deployment.dsl.MuleDeployContext
import com.avioconsulting.mule.deployment.dsl.MuleDeployScript
import org.codehaus.groovy.control.CompilerConfiguration
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

import java.util.concurrent.Callable

@Command(name = 'deploy',
        description = 'Will deploy using your Mule DSL',
        versionProvider = MavenVersionProvider)
class DeployerCommandLine implements Callable<Integer> {
    @Parameters(index = '0', description = 'The path to your DSL file')
    private File groovyFile

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    CredentialOptGroup credential;

    @Option(names = ['-o', '--anypoint-org-name'],
            description = 'The org/business group to use. If you do not specify it, the default for your user will be used')
    private String anypointOrganizationName
    @Option(names = ['-m', '--dry-run-mode'],
            defaultValue = 'Run',
            description = 'Choices are Run (normal run), OfflineValidate (checks your DSL but is offline), or OnlineValidate (does GET operations but no changes)')
    private DryRunMode dryRunMode
    @Option(names = ['-e', '--design-center-environments'],
            defaultValue = 'DEV',
            description = 'Which environments to do the design center deployment during. Default is DEV only')
    private List<String> environmentsToDoDesignCenterDeploymentOn
    @Option(names = ['-a', '--arg'],
            description = 'Other arguments to use for params in your DSL. e.g. -a env=DEV will set params.env in your DSL')
    private Map<String, String> otherArguments = [:]
    @Option(names = ['-V', '--version'],
            versionHelp = true,
            description = 'print version info')
    private boolean versionRequested
    private static IDeployerFactory deployerFactory = new DeployerFactory()
    private static ILogger logger = new SimpleLogger()

    static class CredentialOptGroup {
        @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1", order = 1, heading = "Basic auth credentials of Anypoint Platform%n", headingKey = "Basic Credentials")
        UsernamePasswordCredentialOptGroup usernamePasswordCredential

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1", order = 2, heading = "Connected App credentials%n", headingKey = "Connected App Credentials")
        ConnectedAppCredentialOptGroup connectedAppCredential

        Credential getCoreCredential(){
            return usernamePasswordCredential != null ? usernamePasswordCredential.getCoreCredential() : connectedAppCredential.getCoreCredential()
        }
     }

    static abstract class BaseCredential {
        abstract Credential getCoreCredential()
    }
    static class UsernamePasswordCredentialOptGroup extends BaseCredential {
        @Option(names = ['-u', '--anypoint-username'], required = true, order = 1)
        String anypointUsername
        @Option(names = ['-p', '--anypoint-password'], required = true, order = 2)
        String anypointPassword

        Credential getCoreCredential() {
            return new UsernamePasswordCredential(anypointUsername,anypointPassword)
        }
    }

    static class ConnectedAppCredentialOptGroup {
        @Option(names = ['-caid', '--anypoint-connected-app-id'], required = true, order = 1)
        String anypointConnectedAppId
        @Option(names = ['-casec', '--anypoint-connected-app-secret'], required = true, order = 2)
        String anypointConnectedAppSecret

        Credential getCoreCredential() {
            return new ConnectedAppCredential(anypointConnectedAppId,anypointConnectedAppSecret)
        }
    }


    static void main(String... args) {
        def commandLine = new CommandLine(new DeployerCommandLine())
        if (commandLine.versionHelpRequested) {
            commandLine.printVersionHelp(System.out)
            return
        }
        def exitCode = commandLine.execute(args)
        System.exit(exitCode)
    }

    @Override
    Integer call() throws Exception {
        def deploymentPackage = processDsl()
        logger.println "Successfully processed ${groovyFile} through DSL"
        def deployer = deployerFactory.create(this.credential.getCoreCredential(),
                                              logger,
                                              this.dryRunMode,
                                              this.anypointOrganizationName,
                                              this.environmentsToDoDesignCenterDeploymentOn)
        if (this.dryRunMode == DryRunMode.OfflineValidate) {
            logger.println 'Offline validate was specified, so not deploying'
            return
        }
        logger.println 'Beginning deployment'
        try {
            deployer.deployApplication(deploymentPackage.deploymentRequest,
                                       deploymentPackage.apiSpecifications,
                                       deploymentPackage.desiredPolicies,
                                       deploymentPackage.enabledFeatures)
            logger.println 'Deployment completed'
        }
        catch (e) {
            def exception = e.cause ?: e
            System.err.println("Unable to perform deployment because ${exception.class} ${exception.message}")
            throw new Exception('Unable to perform deployment',
                                e)
        }
        return 0
    }

    private DeploymentPackage processDsl() {
        try {
            // allows us to use our 'MuleDeployScript'/MuleDeployContext class to interpret the user's DSL file
            def compilerConfig = new CompilerConfiguration().with {
                scriptBaseClass = MuleDeployScript.name
                it
            }
            // in case they specify additional runtime settings not in source control via -D maven command line args
            def binding = new Binding()
            def wrapper = new ParamsWrapper(this.otherArguments)
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
            System.err.println("Unable to process DSL because ${exception.class} ${exception.message}")
            throw new Exception('Unable to process DSL',
                                e)
        }
    }
}
