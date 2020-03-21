package com.avioconsulting.mule.deployment.dsl

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings(["UnnecessaryQualifiedReference", "GroovyAssignabilityCheck", "GroovyAccessibility"])
class DeployerContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new DeployerContext()
        def closure = {
            username 'the_username'
            password 'the_password'
        }
        closure.delegate = context
        closure.call()

        // act
        def deployer = context.buildDeployer(System.out)

        // assert
        deployer.clientWrapper.with {
            assertThat username,
                       is(equalTo('the_username'))
            assertThat password,
                       is(equalTo('the_password'))
        }
    }

    @Test
    void with_optional() {
        // arrange
        def context = new DeployerContext()
        def closure = {
            username 'the_username'
            password 'the_password'
            organizationName 'the_org'
        }
        closure.delegate = context
        closure.call()

        // act
        def deployer = context.buildDeployer(System.out)

        // assert
        deployer.clientWrapper.with {
            assertThat username,
                       is(equalTo('the_username'))
            assertThat password,
                       is(equalTo('the_password'))
            assertThat anypointOrganizationName,
                       is(equalTo('the_org'))
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new DeployerContext()
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
