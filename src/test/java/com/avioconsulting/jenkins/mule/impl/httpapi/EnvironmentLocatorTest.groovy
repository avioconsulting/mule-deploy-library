package com.avioconsulting.jenkins.mule.impl.httpapi

import com.avioconsulting.jenkins.mule.impl.HttpServerUtils
import com.avioconsulting.jenkins.mule.impl.httpapi.EnvironmentLocator
import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import groovy.json.JsonOutput
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class EnvironmentLocatorTest implements HttpServerUtils {
    HttpServer httpServer
    int port
    private HttpClientWrapper clientWrapper
    private EnvironmentLocator envLocator

    @Before
    void startServer() {
        httpServer = Vertx.vertx().createHttpServer()
        port = 8080
        clientWrapper = new HttpClientWrapper("http://localhost:${port}",
                                              'the user',
                                              'the password',
                                              'the-org-id',
                                              System.out)
        envLocator = new EnvironmentLocator(clientWrapper,
                                            System.out)
    }

    @After
    void stopServer() {
        clientWrapper.close()
        httpServer.close()
    }

    @Test
    void getEnvironmentId_found() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            url = request.absoluteURI()
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
        def environmentId = envLocator.getEnvironmentId('DEV')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/accounts/api/organizations/the-org-id/environments'))
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
            mockAuthenticationOk(request)
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
            envLocator.getEnvironmentId('FOO')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Unable to find environment 'FOO'. Valid environments are [Design, DEV]"))
    }

    @Test
    void getEnvironmentId_401() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            request.response().with {
                statusCode = 401
                putHeader('Content-Type',
                          'application/json')
                end()
            }
        }

        // act
        def exception = shouldFail {
            envLocator.getEnvironmentId('FOO')
        }

        // assert
        assertThat exception.message,
                   is(containsString("Unable to Retrieve environments (check to ensure your org ID, the-org-id, is correct and the credentials you are using have the right permissions.)"))
    }
}
