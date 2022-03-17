package com.avioconsulting.mule.cli

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployer
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.*
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import org.apache.commons.io.FileUtils
import org.junit.BeforeClass
import org.junit.Test
import picocli.CommandLine

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeployerCommandLineTest implements MavenInvoke {
    @BeforeClass
    static void setupApp() {
        buildApp()
    }

    DeployerCommandLine executeCommandLine(IDeployerFactory mockDeployerFactory,
                                           String groovyFileText,
                                           String user = 'our user',
                                           String pass = 'our pass',
                                           String connId = null,
                                           String connSecret = null,
                                           DryRunMode dryRunMode = DryRunMode.Run,
                                           String orgName = null,
                                           Map<String, String> otherArgs = [:]) {
        def groovyFile = new File('stuff.groovy')
        if (!groovyFileText) {
            FileUtils.deleteQuietly(groovyFile)
        } else {
            groovyFile.text = groovyFileText
        }
        DeployerCommandLine.deployerFactory = mockDeployerFactory
        def args
        if (connId != null && connSecret != null)
            args = [
                    '-i',
                    "\"${connId}\"".toString(),
                    '-s',
                    "\"${connSecret}\"".toString(),
                    '-m',
                    dryRunMode.name()
            ]
        else
            args = [
                    '-u',
                    "\"${user}\"".toString(),
                    '-p',
                    "\"${pass}\"".toString(),
                    '-m',
                    dryRunMode.name(),
            ]
        if (orgName) {
            args += [
                    '-o',
                    "\"${orgName}\"".toString()
            ]
        }
        otherArgs.each { k, v ->
            args << '-a'
            args << "${k}=${v}".toString()
        }
        args += [
                groovyFile.absolutePath
        ]
        println "Calling with args ${args}"
        def exitCode = new CommandLine(new DeployerCommandLine()).execute(args.toArray(new String[0]))
        assert exitCode == 0
    }

    @Test
    void runs() {
        // arrange
        FileBasedAppDeploymentRequest actualApp
        ApiSpecificationList actualApiSpec
        List<Features> actualFeatures
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                    actualApp = appDeploymentRequest
                    actualApiSpec = apiSpecification
                    actualFeatures = enabledFeatures
                }
        ] as IDeployer
        def mock = [
                create: { String username,
                          String password,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file '${builtFile}'
        targetServerOrClusterName 'theServer'
    }
}
"""

        // act
        executeCommandLine(mock,
                           dslText)

        // assert
        assertThat 'No policies since we omitted that section',
                   actualFeatures,
                   is(equalTo([Features.AppDeployment, Features.DesignCenterSync, Features.ApiManagerDefinitions]))
        assertThat actualApiSpec,
                   is(equalTo([]))
        assert actualApp instanceof OnPremDeploymentRequest
        actualApp.with {
            assertThat it.appName,
                       is(equalTo('the-app'))
            assertThat it.environment,
                       is(equalTo('DEV'))
        }
    }

    @Test
    void runs_with_connected_application() {
        // arrange
        FileBasedAppDeploymentRequest actualApp
        ApiSpecificationList actualApiSpec
        List<Features> actualFeatures
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                    actualApp = appDeploymentRequest
                    actualApiSpec = apiSpecification
                    actualFeatures = enabledFeatures
                }
        ] as IDeployer
        def mock = [
                create: { String connectedAppId,
                          String connectedAppSecret,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file '${builtFile}'
        targetServerOrClusterName 'theServer'
    }
}
"""

        // act
        executeCommandLine(mock,
                dslText)

        // assert
        assertThat 'No policies since we omitted that section',
                actualFeatures,
                is(equalTo([Features.AppDeployment, Features.DesignCenterSync, Features.ApiManagerDefinitions]))
        assertThat actualApiSpec,
                is(equalTo([]))
        assert actualApp instanceof OnPremDeploymentRequest
        actualApp.with {
            assertThat it.appName,
                    is(equalTo('the-app'))
            assertThat it.environment,
                    is(equalTo('DEV'))
        }
    }

    @Test
    void offline_validate() {
        // arrange
        def deployed = false
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                    deployed = true
                }
        ] as IDeployer
        def mock = [
                create: { String username,
                          String password,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file '${builtFile}'
        targetServerOrClusterName 'theServer'
    }
}
"""
        // act
        executeCommandLine(mock,
                           dslText,
                           'user',
                           'pass',
                           null,
                           null,
                           DryRunMode.OfflineValidate)

        // assert
        assertThat deployed,
                   is(equalTo(false))
    }

    @Test
    void dsl_params() {
        // arrange
        FileBasedAppDeploymentRequest actualApp
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                    actualApp = appDeploymentRequest
                }
        ] as IDeployer
        def mock = [
                create: { String username,
                          String password,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    cloudHubApplication {
        environment params.env
        applicationName 'the-app'
        appVersion '1.2.3'
        workerSpecs {
            muleVersion params.env == 'DEV' ? '4.2.2' : '4.1.5'
        }
        file '${builtFile}'
        cryptoKey 'theKey'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        cloudHubAppPrefix 'AVI'
    }
}
"""

        // act
        executeCommandLine(mock,
                           dslText,
                           'our user',
                           'our pass',
                           null,
                           null,
                           DryRunMode.Run,
                           null,
                           [
                                   env: 'foobar'
                           ])

        // assert
        assert actualApp instanceof CloudhubDeploymentRequest
        actualApp.with {
            assertThat environment,
                       is(equalTo('foobar'))
            assertThat workerSpecRequest.muleVersion,
                       is(equalTo('4.1.5'))
        }
    }

    @Test
    void file_not_found() {
        // arrange
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecification apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                }
        ] as IDeployer
        def mock = [
                create: { String username,
                          String password,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory

        // act
        shouldFail {
            executeCommandLine(mock,
                               null)
        }

        // assert
    }

    @Test
    void deployment_failure() {
        // arrange
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecification apiSpecification,
                                     List<Policy> desiredPolicies,
                                     List<Features> enabledFeatures ->
                    throw new Exception('some deployment problem')
                }
        ] as IDeployer
        def mock = [
                create: { String username,
                          String password,
                          ILogger logger,
                          DryRunMode dryRunMode,
                          String anypointOrganizationName,
                          List<String> environmentsToDoDesignCenterDeploymentOn ->
                    return mockDeployer
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file '${builtFile}'
        targetServerOrClusterName 'theServer'
    }
}
"""

        // act
        shouldFail {
            executeCommandLine(mock,
                               dslText)
        }

        // assert
    }
}
