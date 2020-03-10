package com.avioconsulting.jenkins.mule.impl

import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest

trait HttpServerUtils {
    abstract HttpServer getHttpServer()

    abstract int getPort()

    def withHttpServer(Handler<HttpServerRequest> closure) {
        httpServer.requestHandler(closure).listen(port)
        def connected = false
        5.times {
            if (connected) {
                return
            }
            try {
                def socket = new Socket('localhost',
                                        port)
                connected = true
                socket.close()
            }
            catch (e) {
                println 'Server not up yet, sleeping'
                Thread.sleep(100)
            }
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
        if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
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
    }

    List<String> capturedStandardHeaders(HttpServerRequest request) {
        ['Authorization',
         'X-ANYPNT-ORG-ID',
         'X-ANYPNT-ENV-ID'].collect { header ->
            request.getHeader(header)
        }
    }
}
