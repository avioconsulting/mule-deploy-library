package com.avioconsulting.jenkins.mule.impl

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HttpClientWrapperTest implements HttpServerUtils {
    HttpServer httpServer
    int port
    private HttpClientWrapper clientWrapper

    @Before
    void startServer() {
        httpServer = Vertx.vertx().createHttpServer()
        port = 8080
        clientWrapper = new HttpClientWrapper("http://localhost:${port}",
                                              'the user',
                                              'the password',
                                              'the-org-id',
                                              System.out)
    }

    @After
    void stopServer() {
        clientWrapper.close()
        httpServer.close()
    }

    @Test
    void authenticate_correct_request() {
        // arrange
        String url = null
        String method = null
        String contentType = null
        Map sentJson = null
        String authHeader = null
        withHttpServer { HttpServerRequest request ->
            url = request.absoluteURI()
            method = request.method().name()
            contentType = request.getHeader('Content-Type')
            authHeader = request.getHeader('Authorization')
            request.bodyHandler { body ->
                sentJson = new JsonSlurper().parseText(body.toString())
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                statusCode = 200
                end(JsonOutput.toJson([
                        access_token: 'the token'
                ]))
            }
        }

        // act
        clientWrapper.authenticate()

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/accounts/login'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat contentType,
                   is(equalTo('application/json'))
        assertThat sentJson,
                   is(equalTo([
                           username: 'the user',
                           password: 'the password'
                   ]))
        assertThat 'We have not authenticated yet',
                   authHeader,
                   is(nullValue())
    }

    @Test
    void authenticate_succeeds() {
        // arrange
        def tokenOnNextRequest = null
        withHttpServer { HttpServerRequest request ->
            request.response().with {
                if (request.absoluteURI() != 'http://localhost:8080/accounts/login') {
                    tokenOnNextRequest = request.getHeader('Authorization')
                }
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        access_token: 'the token'
                ]))
            }
        }

        // act
        clientWrapper.authenticate()
        def response = clientWrapper.execute(new HttpGet("${clientWrapper.baseUrl}/foobar"))
        response.close()

        // assert
        assertThat tokenOnNextRequest,
                   is(equalTo('Bearer the token'))
    }

    @Test
    void only_one_auth_required() {
        // arrange
        def tokenFetches = 0
        withHttpServer { HttpServerRequest request ->
            request.response().with {
                if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                    tokenFetches++
                }
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        access_token: 'the token'
                ]))
            }
        }

        // act
        def response = clientWrapper.execute(new HttpGet("${clientWrapper.baseUrl}/foobar"))
        response.close()
        response = clientWrapper.execute(new HttpGet("${clientWrapper.baseUrl}/foobar"))
        response.close()

        // assert
        assertThat tokenFetches,
                   is(equalTo(1))
    }

    @Test
    void authenticate_fails() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            request.response().with {
                statusCode = 401
                end('Unauthorized')
            }
        }

        // act
        def exception = shouldFail {
            clientWrapper.authenticate()
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Unable to authenticate to Anypoint as 'the user', got an HTTP 401 with a response of 'Unauthorized'".toString()))
    }
}
