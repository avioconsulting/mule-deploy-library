package com.avioconsulting.mule.cli

import com.avioconsulting.mule.deployment.api.DryRunMode
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

import java.util.concurrent.Callable

@Command(name = 'deploy', description = 'Will deploy')
class DeployerCommandLine implements Callable<Integer> {
    @Parameters(index = '0', description = 'The path to your DSL file')
    private File groovyFile
    @Option(names = ['-u', '--anypoint-username'], required = true)
    private String anypointUsername
    @Option(names = ['-p', '--anypoint-password'], required = true)
    private String anypointPassword
    @Option(names = ['-o', '--anypoint-org-name'],
            description = 'The org/business group to use. If you do not specify it, the default for your user will be used')
    private String anypointOrganizationName
    @Option(names = ['-m', '--dry-run-mode'],
            defaultValue = 'Run',
            description = 'Choices are Run (normal run), OfflineValidate (check your DSL but 100% offline), or OnlineValidate (does GET operations but no changes)')
    private DryRunMode dryRunMode
    @Option(names = ['-e', '--design-center-environments'],
            defaultValue = 'DEV',
            description = 'Which environments to do the design center deployment during. Default is DEV only')
    private List<String> environmentsToDoDesignCenterDeploymentOn

    static void main(String... args) {
        def exitCode = new CommandLine(new DeployerCommandLine()).execute(args)
        System.exit(exitCode)
    }

    @Override
    Integer call() throws Exception {
        def logger = new SimpleLogger()
        logger.println 'hi'
        return 0
    }
}
