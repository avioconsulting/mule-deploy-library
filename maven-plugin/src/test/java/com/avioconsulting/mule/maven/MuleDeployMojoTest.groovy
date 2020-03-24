package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.IDeployer
import com.avioconsulting.mule.deployment.api.IDeployerFactory
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings(value = ['GroovyVariableNotAssigned', 'GroovyAccessibility'])
class MuleDeployMojoTest {
    def logger = new TestLogger()

    @Before
    void cleanup() {
        logger.errors.clear()
    }

    MuleDeployMojo getMojo(IDeployerFactory mockDeployerFactory,
                           String groovyFileText,
                           String user = 'our user',
                           String pass = 'our pass',
                           DryRunMode dryRunMode = DryRunMode.Run,
                           String orgName = null,
                           List<String> envs = ['DEV']) {
        def groovyFile = new File('stuff.groovy')
        groovyFile.text = groovyFileText
        new MuleDeployMojo().with {
            it.anypointUsername = user
            it.anypointPassword = pass
            it.dryRunMode = dryRunMode
            it.groovyFile = groovyFile
            it.anypointOrganizationName = orgName
            it.environmentsToDoDesignCenterDeploymentOn = envs
            it.log = logger
            it.deployerFactory = mockDeployerFactory
            it
        }
    }

    @Test
    void gets_correct_params() {
        // arrange
        String actualUser, actualPass, actualOrg
        ILogger actualLogger
        DryRunMode actualDryRunMode
        List<String> actualEnvs
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
                    return null
                }
        ] as IDeployerFactory
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(mock,
                           dslText,
                           'the user',
                           'the pass',
                           // we don't want this thing to actually run
                           DryRunMode.OfflineValidate,
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
                   is(equalTo(DryRunMode.OfflineValidate))
        assertThat actualEnvs,
                   is(equalTo(['TST']))
    }

    @Test
    void runs() {
        // arrange
        FileBasedAppDeploymentRequest actualApp
        ApiSpecification actualApiSpec
        List<Features> actualFeatures
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecification apiSpecification,
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
        file 'path/to/file.jar'
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
                   is(nullValue())
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
                                     ApiSpecification apiSpecification,
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
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(mock,
                           dslText,
                           'user',
                           'pass',
                           DryRunMode.OfflineValidate)
        // act
        mojo.execute()

        // assert
        assertThat deployed,
                   is(equalTo(false))
    }

    @Test
    void dsl_params() {
        // arrange
        System.setProperty('env',
                           'foobar')
        FileBasedAppDeploymentRequest actualApp
        def mockDeployer = [
                deployApplication: { FileBasedAppDeploymentRequest appDeploymentRequest,
                                     ApiSpecification apiSpecification,
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
    
    onPremApplication {
        environment params.env
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(mock,
                           dslText)

        // act
        mojo.execute()

        // assert
        assert actualApp instanceof OnPremDeploymentRequest
        assertThat actualApp.environment,
                   is(equalTo('foobar'))
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
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
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
        file 'path/to/file.jar'
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
        assertThat logger.errors,
                   is(equalTo([
                           'Unable to perform deployment because class java.lang.Exception some deployment problem'
                   ]))
    }
}
