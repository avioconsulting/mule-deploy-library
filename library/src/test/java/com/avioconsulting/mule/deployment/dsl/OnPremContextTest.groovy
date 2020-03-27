package com.avioconsulting.mule.deployment.dsl

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

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
        def context = new OnPremContext()
        def closure = {
            environment 'DEV'
            applicationName 'the-app'
            appVersion '1.2.3'
            file 'path/to/file.jar'
            targetServerOrClusterName 'server1'
            appProperties foo: 'bar'
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
            assertThat appProperties,
                       is(equalTo([foo: 'bar']))
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new OnPremContext()
        def closure = {
//            environment 'DEV'
//            applicationName 'the-app'
//            appVersion '1.2.3'
//            file 'path/to/file.jar'
//            targetServerOrClusterName 'server1'
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentRequest()
        }

        // assert
        assertThat exception.message,
                   is(equalTo("""Your deployment request is not complete. The following errors exist:
- appVersion missing
- applicationName missing
- environment missing
- file missing
- targetServerOrClusterName missing
""".trim()))
    }
}
