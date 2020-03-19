package com.avioconsulting.mule.deployment.dsl

import org.junit.Assert
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class CloudhubContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            applicationName 'the-app'
            appVersion '1.2.3'
            workerSpecs {
                muleVersion '4.2.2'
            }
            file 'path/to/file.jar'
            cryptoKey 'theKey'
            autodiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            cloudHubAppPrefix 'AVI'
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.deploymentRequest

        // assert
        assertThat request.environment,
                   is(equalTo('DEV'))
        assertThat request.appName,
                   is(equalTo('the-app'))
        assertThat request.appVersion,
                   is(equalTo('1.2.3'))
        def workerSpecs = request.workerSpecRequest
        assertThat workerSpecs.muleVersion,
                   is(equalTo('4.2.2'))
        assertThat request.file,
                   is(equalTo(new File('path/to/file.jar')))
        assertThat request.cryptoKey,
                   is(equalTo('theKey'))
        assertThat request.anypointClientId,
                   is(equalTo('the_client_id'))
        assertThat request.anypointClientSecret,
                   is(equalTo('the_client_secret'))
        assertThat request.cloudHubAppPrefix,
                   is(equalTo('AVI'))
    }

    @Test
    void include_optional() {
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

    @Test
    void repeat_section() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
