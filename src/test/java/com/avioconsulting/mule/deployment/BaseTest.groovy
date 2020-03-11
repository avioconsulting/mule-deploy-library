package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import org.junit.After
import org.junit.Before

class BaseTest {
    protected HttpServer httpServer
    protected HttpClientWrapper clientWrapper
    protected Handler<HttpServerRequest> closure

    @Before
    void startServer() {
        httpServer = createServer()
        closure = null
        clientWrapper = new HttpClientWrapper("http://localhost:${httpServer.actualPort()}",
                                              'the user',
                                              'the password',
                                              'the-org-id',
                                              System.out)
    }

    @After
    void stopServer() {
        try {
            clientWrapper.close()
        }
        catch (e) {
            println "could not close ${e}"
        }
        try {
            def test = this
            println 'Closing mock web server'
            // closing is async
            httpServer.close {
                synchronized (test) {
                    test.notify()
                }
            }
            println 'Waiting for web server to close'
            synchronized (test) {
                test.wait()
            }
            println 'Web server closed'
        }
        catch (e) {
            println "could not close ${e}"
        }
    }

    def withHttpServer(Handler<HttpServerRequest> closure) {
        this.closure = closure
    }

    HttpServer createServer() {
        // lets us switch our handler in each test
        def closureForClosure = { HttpServerRequest request ->
            if (!closure) {
                request.response().with {
                    statusCode = 500
                    end('You did not set this.closure in your test!')
                }
                return
            }
            closure.handle(request)
        }
        def test = this
        // 0 means any available port
        def server = Vertx.vertx()
                .createHttpServer()
                .requestHandler(closureForClosure)
                .listen(0,
                        {
                            // listen is an async operation but we can/will block until it's up
                            synchronized (test) {
                                test.notify()
                            }
                        })
        println 'Waiting for server to come up'
        synchronized (test) {
            test.wait()
        }
        println 'Server is up'
        server
    }

    def mockAuthenticationOk(HttpServerRequest request) {
        def mocked = false
        if (request.uri() == '/accounts/login') {
            def response = request.response()
            response.statusCode = 200
            response.putHeader('Content-Type',
                               'application/json')
            response.end(JsonOutput.toJson([
                    access_token: 'the token'
            ]))
            mocked = true
        } else if (request.uri() == '/accounts/api/me') {
            def response = request.response()
            response.statusCode = 200
            response.putHeader('Content-Type',
                               'application/json')
            response.end(JsonOutput.toJson([
                    user: [
                            id      : 'the_id',
                            username: 'the_username'
                    ]
            ]))
            mocked = true
        }
        return mocked
    }

    def mockEnvironments(HttpServerRequest request) {
        def mocked = false
        if (request.uri() == '/accounts/api/organizations/the-org-id/environments') {
            mocked = true
            def response = request.response()
            response.statusCode = 200
            response.putHeader('Content-Type',
                               'application/json')
            response.end(JsonOutput.toJson([
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
        mocked
    }

    List<String> capturedStandardHeaders(HttpServerRequest request) {
        ['Authorization',
         'X-ANYPNT-ORG-ID',
         'X-ANYPNT-ENV-ID'].collect { header ->
            request.getHeader(header)
        }
    }
}
