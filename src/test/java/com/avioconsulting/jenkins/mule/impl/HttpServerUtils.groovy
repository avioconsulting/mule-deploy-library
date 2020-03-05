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
        def response = request.response()
        response.statusCode = 200
        response.putHeader('Content-Type',
                           'application/json')
        response.end(JsonOutput.toJson([
                access_token: 'the token'
        ]))
    }

    List<String> capturedStandardHeaders(HttpServerRequest request) {
        ['Authorization',
         'X-ANYPNT-ORG-ID',
         'X-ANYPNT-ENV-ID'].collect { header ->
            request.getHeader(header)
        }
    }
}
