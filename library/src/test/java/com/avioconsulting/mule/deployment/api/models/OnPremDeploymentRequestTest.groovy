package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class OnPremDeploymentRequestTest implements MavenInvoke {
    @BeforeClass
    static void setup() {
        buildApp()
    }

    @Test
    void explicit() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')

        // act
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                                                  'some-app-name',
                                                  '1.2.3')

        // assert
        assertThat request.appName,
                   is(equalTo('some-app-name'))
        assertThat request.appVersion,
                   is(equalTo('1.2.3'))
    }

    @Test
    void derived_app_version_and_name() {
        // arrange

        // act
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  builtFile)

        // assert
        request.with {
            assertThat appName,
                       is(equalTo('mule4testapp'))
            assertThat appVersion,
                       is(equalTo('1.0.0'))
        }
    }

    @Test
    void spaces_in_name() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')

        // act
        def exception = shouldFail {
            new OnPremDeploymentRequest('DEV',
                                        'clustera',
                                        file,
                                        'some app name',
                                        '1.2.3')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }
}
