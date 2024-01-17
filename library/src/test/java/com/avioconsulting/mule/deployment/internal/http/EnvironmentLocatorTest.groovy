package com.avioconsulting.mule.deployment.internal.http

import com.avioconsulting.mule.deployment.BaseTest
import groovy.json.JsonOutput
import io.vertx.core.http.HttpServerRequest
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class EnvironmentLocatorTest extends BaseTest {
    @Test
    void getEnvironmentId_found() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            url = request.uri()
            method = request.method()
            authToken = request.getHeader('Authorization')
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'Design'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'DEV'
                                ]
                        ]
                ]))
            }
        }

        // act
        def environmentId = environmentLocator.getEnvironmentId('DEV')

        // assert
        assertThat url,
                   is(equalTo('/accounts/api/organizations/the-org-id/environments'))
        assertThat method,
                   is(equalTo('GET'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat environmentId,
                   is(equalTo('def456'))
    }

    @Test
    void getEnvironmentId_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'Design'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'DEV'
                                ]
                        ]
                ]))
            }
        }

        // act
        def exception = shouldFail {
            environmentLocator.getEnvironmentId('FOO')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Unable to find environment 'FOO'. Valid environments are [Design, DEV]"))
    }

    @Test
    void getEnvironmentId_401() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            request.response().with {
                statusCode = 401
                putHeader('Content-Type',
                          'application/json')
                end()
            }
        }

        // act
        def exception = shouldFail {
            environmentLocator.getEnvironmentId('FOO')
        }

        // assert
        assertThat exception.message,
                   is(containsString("Unable to Retrieve environments (check to ensure your org ID, the-org-id, is correct and the credentials you are using have the right permissions.)"))
    }
}
