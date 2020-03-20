package com.avioconsulting.mule.deployment.dsl

import org.junit.Assert
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings(["UnnecessaryQualifiedReference", "GroovyAssignabilityCheck"])
class OnPremContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new OnPremContext()
        def closure = {
            environment 'DEV'
            applicationName 'the-app'
            appVersion '1.2.3'
            file 'path/to/file.jar'
            targetServerOrClusterName 'server1'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            assertThat environment,
                       is(equalTo('DEV'))
            assertThat appName,
                       is(equalTo('the-app'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            assertThat file,
                       is(equalTo(new File('path/to/file.jar')))
            assertThat targetServerOrClusterName,
                       is(equalTo('server1'))
        }
    }

    @Test
    void with_optional() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void missing_required() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
