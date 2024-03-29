package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class OnPremDeploymentRequestTest implements MavenInvoke {

    @BeforeAll
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
                                                  new ApplicationName('some-app-name', null, null),
                                                  '1.2.3')

        // assert
        assertThat request.appName.baseAppName,
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
                                                  builtFile,
                                                  new ApplicationName('mule-deploy-lib-v4-test-app', null, null),)

        // assert
        request.with {
            assertThat appName.baseAppName,
                       is(equalTo('mule-deploy-lib-v4-test-app'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
        }
    }

    @Test
    void spaces_in_name() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')

        // act
        def exception = shouldFail {
            (new ApplicationName('some app name', null, null)).normalizedAppName
        }

        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Name must be alphanumeric with dashes allowed within"))
    }
}
