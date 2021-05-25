package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployer
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.*
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.ArtifactHandler
import org.apache.maven.project.MavenProject
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings(value = ['GroovyVariableNotAssigned', 'GroovyAccessibility'])
class MuleDeployMojoTest implements MavenInvoke {
    def logger = new TestLogger()

    @BeforeClass
    static void setupApp() {
        buildApp()
    }

    @Before
    void cleanup() {
        logger.errors.clear()
    }

    @Test
    void gets_correct_params() {
        // arrange
        String actualUser, actualPass, actualOrg
        ILogger actualLogger
        DryRunMode actualDryRunMode
        List<String> actualEnvs
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
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
                    actualUser = username
                    actualPass = password
                    actualLogger = logger
                    actualDryRunMode = dryRunMode
                    actualEnvs = environmentsToDoDesignCenterDeploymentOn
                    actualOrg = anypointOrganizationName
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
        def mojo = getMojo(mock,
                           dslText,
                           'the user',
                           'the pass',
                           // we don't want this thing to actually run
                           DryRunMode.OnlineValidate,
                           'the org',
                           ['TST'])

        // act
        mojo.execute()

        // assert
        assertThat actualUser,
                   is(equalTo('the user'))
        assertThat actualPass,
                   is(equalTo('the pass'))
        assertThat actualOrg,
                   is(equalTo('the org'))
        assertThat actualDryRunMode,
                   is(equalTo(DryRunMode.OnlineValidate))
        assertThat actualEnvs,
                   is(equalTo(['TST']))
    }

    MuleDeployMojo getMojo(IDeployerFactory mockDeployerFactory,
                           String groovyFileText,
                           String user = 'our user',
                           String pass = 'our pass',
                           DryRunMode dryRunMode = DryRunMode.Run,
                           String orgName = null,
                           List<String> envs = ['DEV'],
                           MavenProject mockMavenProject = null) {
        def groovyFile = new File('stuff.groovy')
        groovyFile.text = groovyFileText
        new MuleDeployMojo().with {
            it.anypointUsername = user
            it.anypointPassword = pass
            it.dryRunMode = dryRunMode
            it.groovyFile = groovyFile
            it.anypointOrganizationName = orgName
            it.environmentsToDoDesignCenterDeploymentOn = envs
            it.log = this.logger
            it.mavenProject = mockMavenProject ?: ([:] as MavenProject)
            it.deployerFactory = mockDeployerFactory
            it
        }
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
        def mojo = getMojo(mock,
                           dslText)

        // act
        mojo.execute()

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
        def mojo = getMojo(mock,
                           dslText,
                           null,
                           null,
                           DryRunMode.OfflineValidate)
        // act
        mojo.execute()

        // assert
        assertThat deployed,
                   is(equalTo(false))
    }

    @Test
    void online_validate_missing_creds() {
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
        def mojo = getMojo(mock,
                           dslText,
                           null,
                           null,
                           DryRunMode.OnlineValidate)
        // act
        def exception = shouldFail {
            mojo.execute()
        }

        // assert
        assertThat deployed,
                   is(equalTo(false))
        assertThat exception.message,
                   is(containsString('In order to OnlineValidate, credentials must be supplied via the anypointUsername <config> item/anypoint.username property and the anypointPassword <config> item/anypoint.password property'))
    }

    @Test
    void dsl_params() {
        // arrange
        System.setProperty('muleDeploy.env',
                           'foobar')
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
        file params.appArtifact
        cryptoKey 'theKey'
        autoDiscovery {
            clientId 'the_client_id'
            clientSecret 'the_client_secret'
        }
        cloudHubAppPrefix 'AVI'
    }
}
"""
        def handler = [:] as ArtifactHandler
        def mockProject = [
                getAttachedArtifacts: {
                    [
                            new DefaultArtifact('thegroup',
                                                'theart',
                                                '1.0.0',
                                                'compile',
                                                'jar',
                                                'mule-application',
                                                handler).with {
                                it.setFile(builtFile)
                                it
                            }
                    ]
                }
        ] as MavenProject
        def mojo = getMojo(mock,
                           dslText,
                           'user',
                           'pass',
                           DryRunMode.Run,
                           null,
                           ['DEV'],
                           mockProject)

        // act
        mojo.execute()

        // assert
        assert actualApp instanceof CloudhubDeploymentRequest
        actualApp.with {
            assertThat environment,
                       is(equalTo('foobar'))
            assertThat file.name,
                       is(equalTo('mule-deploy-lib-v4-test-app-2.2.9-mule-application.jar'))
            assertThat workerSpecRequest.muleVersion,
                       is(equalTo('4.1.5'))
        }
    }

    @Test
    void file_not_found() {
        // arrange
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
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
        def mojo = getMojo(mock,
                           dslText)
        mojo.groovyFile = new File('foobar')

        // act
        def exception = shouldFail {
            mojo.execute()
        }

        // assert
        assertThat exception.message,
                   is(containsString('Unable to process DSL'))
        assertThat logger.errors.size(),
                   is(equalTo(1))
        assertThat logger.errors[0],
                   is(containsString('Unable to process DSL because class java.io.FileNotFoundException'))
    }

    @Test
    void deployment_failure() {
        // arrange
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecificationList apiSpecification,
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
        def mojo = getMojo(mock,
                           dslText)

        // act
        def exception = shouldFail {
            mojo.execute()
        }

        // assert
        assertThat exception.message,
                   is(containsString('Unable to perform deployment'))
        assertThat logger.errors.size(),
                   is(equalTo(1))
        assertThat logger.errors[0],
                   is(equalTo('Unable to perform deployment because class java.lang.Exception some deployment problem'))
    }
}
