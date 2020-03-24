package com.avioconsulting.mule.maven

import com.avioconsulting.mule.deployment.api.DryRunMode
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings(value = ['GroovyVariableNotAssigned', 'GroovyAccessibility'])
class MuleDeployMojoTest {
    def logger = new TestLogger()

    @Before
    void cleanup() {
        logger.errors.clear()
    }

    MuleDeployMojo getMojo(IDeployerFactory mockDeployerFactory,
                           String user,
                           String pass,
                           File groovyFile,
                           String orgName = null,
                           List<String> envs = ['DEV']) {
        new MuleDeployMojo().with {
            it.anypointUsername = user
            it.anypointPassword = pass
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
        PrintStream actualLogger
        DryRunMode actualDryRunMode
        List<String> actualEnvs
        def mock = [
                create: { String username,
                          String password,
                          PrintStream logger,
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
        def mojo = getMojo(mock,
                           'the user',
                           'the pass',
                           new File('stuff.groovy'),
                           'the org',
                           ['TST'])

        // act
        mojo.execute()

        // assert
        assertThat actualUser,
                   is(equalTo('stuff'))
        Assert.fail("write it")
    }

    @Test
    void runs() {
        // arrange
        // TODO: min params
        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void offline_validate() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void dsl_params() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
