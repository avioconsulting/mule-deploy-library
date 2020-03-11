package com.avioconsulting.mule.deployment

import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest

trait HttpServerUtils {
    abstract HttpServer getHttpServer()

    abstract int getPort()

    def withHttpServer(Handler<HttpServerRequest> closure) {
        httpServer.requestHandler(closure).listen(port)
        def exception = null
        5.times {
            if (exception == null) {
                return
            }
            try {
                def socket = new Socket('localhost',
                                        port)
                exception = null
                socket.close()
            }
            catch (e) {
                exception = e
                println 'Server not up yet, sleeping and trying again'
                Thread.sleep(100)
            }
        }
        if (exception) {
            throw new Exception("Tried 5 times and was unable to connect to server",
                                exception)
        }
    }

    def mockAuthenticationOk(HttpServerRequest request) {
        def mocked = false
        if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
            def response = request.response()
            response.statusCode = 200
            response.putHeader('Content-Type',
                               'application/json')
            response.end(JsonOutput.toJson([
                    access_token: 'the token'
            ]))
            mocked = true
        } else if (request.absoluteURI() == 'http://localhost:8080/accounts/api/me') {
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
        if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
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
