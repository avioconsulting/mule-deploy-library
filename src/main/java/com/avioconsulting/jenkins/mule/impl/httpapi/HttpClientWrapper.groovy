package com.avioconsulting.jenkins.mule.impl.httpapi

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext

class HttpClientWrapper implements HttpRequestInterceptor {
    private final String username
    private final String password
    private String accessToken
    private String ownerGuid
    private final PrintStream logger
    final String baseUrl
    private final CloseableHttpClient httpClient
    final String anypointOrganizationId

    HttpClientWrapper(String baseUrl,
                      String username,
                      String password,
                      String anypointOrganizationId,
                      PrintStream logger) {
        this.anypointOrganizationId = anypointOrganizationId
        this.password = password
        this.username = username
        this.logger = logger
        this.baseUrl = baseUrl
        this.httpClient = HttpClients.custom()
                .addInterceptorFirst(this)
                .build()
    }

    HttpClientWrapper(String username,
                      String password,
                      String anypointOrganizationId,
                      PrintStream logger) {
        this('https://anypoint.mulesoft.com',
             username,
             password,
             anypointOrganizationId,
             logger)
    }

    /**
     *
     * @param username
     * @param password
     * @return - an auth token
     */
    private def authenticate() {
        fetchAccessToken()
        fetchUserInfo()
    }

    private def fetchUserInfo() {
        logger.println('Fetching user information')
        def request = new HttpGet("${baseUrl}/accounts/api/me")
        executeWithSuccessfulCloseableResponse(request,
                                               'fetch user info') { result ->
            this.ownerGuid = result.user.id
        }
    }

    private def fetchAccessToken() {
        logger.println "Authenticating to Anypoint as user '${username}'"
        def payload = [
                username: username,
                password: password
        ]
        def request = new HttpPost("${baseUrl}/accounts/login").with {
            setEntity(new StringEntity(JsonOutput.toJson(payload)))
            addHeader('Content-Type',
                      'application/json')
            it
        }
        httpClient.execute(request).with { response ->
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               "authenticate to Anypoint as '${username}'")
            logger.println 'Successfully authenticated'
            accessToken = result.access_token
        }
    }

    static def assertSuccessfulResponse(CloseableHttpResponse response,
                                        String failureContext) {
        def status = response.statusLine.statusCode
        if (status < 200 || status > 299) {
            throw new Exception("Unable to ${failureContext}, got an HTTP ${status} with a response of '${response.entity.content.text}'")
        }
    }

    static def assertSuccessfulResponseAndReturnJson(CloseableHttpResponse response,
                                                     String failureContext) {
        assertSuccessfulResponse(response,
                                 failureContext)
        def contentType = response.getFirstHeader('Content-Type')
        assert contentType?.value?.contains('application/json'): "Expected a JSON response but got ${contentType}!"
        new JsonSlurper().parse(response.entity.content)
    }

    def executeWithSuccessfulCloseableResponse(HttpUriRequest request,
                                               String failureContext,
                                               Closure closure) {
        execute(request).withCloseable { response ->
            def parsed = assertSuccessfulResponseAndReturnJson(response,
                                                               failureContext)
            closure(parsed)
        }
    }

    def close() {
        httpClient.close()
    }

    CloseableHttpResponse execute(HttpUriRequest request) {
        if (!accessToken) {
            authenticate()
        }
        httpClient.execute(request)
    }

    String getOwnerGuid() {
        this.ownerGuid
    }

    @Override
    void process(HttpRequest httpRequest,
                 HttpContext httpContext) throws HttpException, IOException {
        if (accessToken) {
            httpRequest.setHeader('Authorization',
                                  "Bearer ${accessToken}")
        }
    }
}
