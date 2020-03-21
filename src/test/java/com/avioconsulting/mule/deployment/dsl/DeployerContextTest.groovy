package com.avioconsulting.mule.deployment.dsl

import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings(["UnnecessaryQualifiedReference", "GroovyAssignabilityCheck", "GroovyAccessibility"])
class DeployerContextTest {
    private DeployerContext context
    String username, password, org
    PrintStream logger

    @Before
    void setup() {
        username = password = org = null
        logger = null
        def mockFactory = [
                create: { String user,
                          String pass,
                          PrintStream log,
                          String anypointOrganizationName ->
                    username = user
                    password = pass
                    logger = log
                    org = anypointOrganizationName
                    // we don't care about the result, just the call
                    return null
                }
        ] as IDeployerFactory
        context = new DeployerContext(mockFactory)
    }

    @Test
    void required_only() {
        // arrange
        def closure = {
            username 'the_username'
            password 'the_password'
        }
        closure.delegate = context
        closure.call()

        // act
        context.buildDeployer(System.out)

        // assert
        assertThat username,
                   is(equalTo('the_username'))
        assertThat password,
                   is(equalTo('the_password'))
        assertThat logger,
                   is(equalTo(System.out))
    }

    @Test
    void with_optional() {
        // arrange
        def closure = {
            username 'the_username'
            password 'the_password'
            organizationName 'the_org'
        }
        closure.delegate = context
        closure.call()

        // act
        context.buildDeployer(System.out)

        // assert
        assertThat username,
                   is(equalTo('the_username'))
        assertThat password,
                   is(equalTo('the_password'))
        assertThat org,
                   is(equalTo('the_org'))
    }

    @Test
    void missing_required() {
        // arrange
        def closure = {
//            username 'the_username'
//            password 'the_password'
//            organizationName 'the_org'
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.buildDeployer(System.out)
        }

        // assert
        assertThat exception.message,
                   is(equalTo("""Your settings are not complete. The following errors exist:
- settings.password missing
- settings.username missing
""".trim()))
    }
}
