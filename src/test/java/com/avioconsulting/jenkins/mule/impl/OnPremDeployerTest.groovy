package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.EnvironmentLocator
import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.OnPremDeploymentRequest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class OnPremDeployerTest implements HttpServerUtils {
    HttpServer httpServer
    private OnPremDeployer deployer
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
        def envLocator = new EnvironmentLocator(clientWrapper,
                                                System.out)
        deployer = new OnPremDeployer(this.clientWrapper,
                                      envLocator,
                                      500,
                                      10,
                                      System.out)
    }

    @After
    void stopServer() {
        clientWrapper.close()
        httpServer.close()
    }

    @Test
    void locate_server_request_is_correct() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            url = request.absoluteURI()
            method = request.method()
            (authToken, orgId, envId) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'serverb'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'servera'
                                ]
                        ]
                ]))
            }
        }

        // act
        deployer.locateServer('DEV',
                              'servera')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/servers'))
        assertThat method,
                   is(equalTo('GET'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
    }

    @Test
    void locate_server_or_cluster_found_server() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : '123',
                                        name: 'serverb'
                                ],
                                [
                                        id  : '456',
                                        name: 'servera'
                                ]
                        ]
                ]))
            }
        }

        // act
        def serverId = deployer.locateServer('DEV',
                                             'servera')

        // assert
        assertThat serverId,
                   is(equalTo('456'))
    }

    @Test
    void locate_server_or_cluster_found_cluster() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'serverb'
                                ],
                                [
                                        id         : 'def456',
                                        name       : 'servera',
                                        clusterId  : 'cluster1',
                                        clusterName: 'clustera'
                                ]
                        ]
                ]))
            }
        }

        // act
        def serverId = deployer.locateServer('DEV',
                                             'clustera')

        // assert
        assertThat serverId,
                   is(equalTo('cluster1'))
    }

    @Test
    void locate_server_or_cluster_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'serverb'
                                ],
                                [
                                        id         : 'def456',
                                        name       : 'servera',
                                        clusterId  : 'cluster1',
                                        clusterName: 'clustera'
                                ]
                        ]
                ]))
            }
        }

        // act
        def exception = shouldFail {
            deployer.locateServer('DEV',
                                  'foobar')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Unable to find server/cluster 'foobar'. Valid servers/clusters are [serverb, servera, clustera]"))
    }

    @Test
    void locate_app_correct_request() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            url = request.absoluteURI()
            method = request.method()
            (authToken, orgId, envId) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'app1'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'the-app'
                                ]
                        ]
                ]))
            }
        }

        // act
        deployer.locateApplication('DEV',
                                   'the-app')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications'))
        assertThat method,
                   is(equalTo('GET'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
    }

    @Test
    void locate_app_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'app1'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'the-app'
                                ]
                        ]
                ]))
            }
        }

        // act
        def appId = deployer.locateApplication('DEV',
                                               'non-existent-app')

        // assert
        assertThat 'app not being there is not an exception',
                   appId,
                   is(nullValue())
    }

    @Test
    void locate_app_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                [
                                        id  : 'abc123',
                                        name: 'app1'
                                ],
                                [
                                        id  : 'def456',
                                        name: 'the-app'
                                ]
                        ]
                ]))
            }
        }

        // act
        def appId = deployer.locateApplication('DEV',
                                               'the-app')

        // assert
        assertThat appId,
                   is(equalTo('def456'))
    }

    @Test
    void perform_deployment_correct_request_new_app() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'def456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/1234') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'some_file.txt')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid1',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.bodyHandler { buffer ->
                        rawBody = buffer.toString()
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['artifactName', 'configuration', 'targetId']))
        assertThat sentFormAttributes.get('artifactName'),
                   is(equalTo('new-app'))
        assertThat sentFormAttributes.get('targetId'),
                   is(equalTo('cluster1'))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'new-app',
                                   properties     : [:]
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_property_overrides_via_runtime_manager() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'def456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/1234') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'some_file.txt')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid1',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.bodyHandler { buffer ->
                        rawBody = buffer.toString()
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream,
                                                  [prop1: 'foo', prop2: 'bar'])

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['artifactName', 'configuration', 'targetId']))
        assertThat sentFormAttributes.get('artifactName'),
                   is(equalTo('new-app'))
        assertThat sentFormAttributes.get('targetId'),
                   is(equalTo('cluster1'))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'new-app',
                                   properties     : [
                                           prop1: 'foo',
                                           prop2: 'bar'
                                   ]
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_property_overrides_via_zip_file() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        def newZipFile = new File('target/temp/newOurApp.zip')
        if (newZipFile.exists()) {
            assert newZipFile.delete()
        }
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'def456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/1234') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'ourapp.zip')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid1',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.uploadHandler { upload ->
                        println "got ZIP file " + upload.filename()
                        upload.streamToFileSystem(newZipFile.absolutePath)
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def stream = new FileInputStream(zipFile)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'testapp',
                                                  'clustera',
                                                  zipFile.name,
                                                  stream,
                                                  [existing: 'changed'],
                                                  'api.dev.properties')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['artifactName', 'configuration', 'targetId']))
        assertThat sentFormAttributes.get('artifactName'),
                   is(equalTo('testapp'))
        assertThat sentFormAttributes.get('targetId'),
                   is(equalTo('cluster1'))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'testapp',
                                   properties     : [:]
                           ]
                   ]))
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        antBuilder.unzip(src: newZipFile.absolutePath,
                         dest: destination)
        def newProps = new Properties()
        newProps.load(new FileInputStream(new File(destination,
                                                   'classes/api.dev.properties')))
        assertThat newProps,
                   is(equalTo([
                           existing: 'changed',
                   ]))
    }

    @Test
    void perform_deployment_space_in_app_name() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        def exception = shouldFail {
            new OnPremDeploymentRequest('DEV',
                                        'some app name',
                                        'clustera',
                                        file.name,
                                        stream)
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }

    @Test
    void perform_deployment_correct_request_existing_app() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'app456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/app456') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'some_file.txt')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid2',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.bodyHandler { buffer ->
                        rawBody = buffer.toString()
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'the-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications/app456'))
        assertThat method,
                   is(equalTo('PATCH'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['configuration']))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'the-app',
                                   properties     : [:]
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_existing_app_property_overrides_via_runtime_manager() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'app456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/app456') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'some_file.txt')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid2',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.bodyHandler { buffer ->
                        rawBody = buffer.toString()
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'the-app',
                                                  'clustera',
                                                  file.name,
                                                  stream,
                                                  [prop1: 'foo', prop2: 'bar'])

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications/app456'))
        assertThat method,
                   is(equalTo('PATCH'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['configuration']))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'the-app',
                                   properties     : [
                                           prop1: 'foo',
                                           prop2: 'bar'
                                   ]
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_existing_app_property_overrides_via_zip_file() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        def newZipFile = new File('target/temp/newOurApp.zip')
        if (newZipFile.exists()) {
            assert newZipFile.delete()
        }
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('environments')) {
                    result = [
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
                    ]
                } else if (uri.endsWith('servers')) {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'serverb'
                                    ],
                                    [
                                            id         : 'def456',
                                            name       : 'servera',
                                            clusterId  : 'cluster1',
                                            clusterName: 'clustera'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
                    result = [
                            data: [
                                    [
                                            id  : 'abc123',
                                            name: 'app1'
                                    ],
                                    [
                                            id  : 'app456',
                                            name: 'the-app'
                                    ]
                            ]
                    ]
                } else if (uri.endsWith('applications/app456') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getAppStatusJson('STARTED',
                                              'STARTED',
                                              'STARTED',
                                              'ourapp.zip')
                } else {
                    // deployment service returns this
                    statusCode = 202
                    result = [
                            data: [
                                    id             : 1234,
                                    serverArtifacts: [
                                            [
                                                    id           : 'artid2',
                                                    desiredStatus: 'UPDATED'
                                            ]
                                    ]
                            ]
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                    request.uploadHandler { upload ->
                        println "got ZIP file " + upload.filename()
                        upload.streamToFileSystem(newZipFile.absolutePath)
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def stream = new FileInputStream(zipFile)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'the-app',
                                                  'clustera',
                                                  zipFile.name,
                                                  stream,
                                                  [existing: 'changed'],
                                                  'api.dev.properties')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications/app456'))
        assertThat method,
                   is(equalTo('PATCH'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['configuration']))
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'the-app',
                                   properties     : [:]
                           ]
                   ]))
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        antBuilder.unzip(src: newZipFile.absolutePath,
                         dest: destination)
        def newProps = new Properties()
        newProps.load(new FileInputStream(new File(destination,
                                                   'classes/api.dev.properties')))
        assertThat newProps,
                   is(equalTo([
                           existing: 'changed',
                   ]))
    }

    @Test
    void check_deployment_requests_properly_gets_status_updated() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            url = request.absoluteURI()
            method = request.method()
            (authToken, orgId, envId) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                id             : 'appid',
                                serverArtifacts: [
                                        [
                                                id           : 'wrongone',
                                                desiredStatus: 'STARTED',
                                                artifact     : [
                                                        fileName: 'wrongone.zip'
                                                ]
                                        ],
                                        [
                                                id           : 'artid',
                                                desiredStatus: 'UPDATED',
                                                artifact     : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'appid',
                                           'theFile.zip')

        // assert
        assertThat status.toList(),
                   is(equalTo([OnPremDeploymentStatus.RECEIVED]))
        assertThat url,
                   is(equalTo('http://localhost:8080/hybrid/api/v1/applications/appid'))
        assertThat method,
                   is(equalTo('GET'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
    }

    @Test
    void check_deployment_status_failed() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                id             : 'appid',
                                serverArtifacts: [
                                        [
                                                id           : 'wrongone',
                                                desiredStatus: 'STARTED',
                                                artifact     : [
                                                        fileName: 'wrong.zip'
                                                ]
                                        ],
                                        [
                                                id                : 'artid',
                                                desiredStatus     : 'STARTED',
                                                lastReportedStatus: 'DEPLOYMENT_FAILED',
                                                artifact          : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'appid',
                                           'theFile.zip')

        // assert
        assertThat status.toList(),
                   is(equalTo([OnPremDeploymentStatus.FAILED]))
    }

    @Test
    void check_deployment_status_started() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                id             : 'appid',
                                serverArtifacts: [
                                        [
                                                id           : 'wrongone',
                                                desiredStatus: 'STARTED',
                                                artifact     : [
                                                        fileName: 'wrong.zip'
                                                ]
                                        ],
                                        [
                                                id                : 'artid',
                                                desiredStatus     : 'STARTED',
                                                lastReportedStatus: 'STARTED',
                                                artifact          : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'appid',
                                           'theFile.zip')

        // assert
        assertThat status.toList(),
                   is(equalTo([OnPremDeploymentStatus.STARTED]))
    }

    @Test
    void check_deployment_status_starting() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                id             : 'appid',
                                serverArtifacts: [
                                        [
                                                id           : 'wrongone',
                                                desiredStatus: 'STARTED',
                                                artifact     : [
                                                        fileName: 'wrong.zip'
                                                ]
                                        ],
                                        [
                                                id                : 'artid',
                                                desiredStatus     : 'STARTED',
                                                lastReportedStatus: 'STARTING',
                                                artifact          : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'appid',
                                           'theFile.zip')

        // assert
        assertThat status.toList(),
                   is(equalTo([OnPremDeploymentStatus.STARTING]))
    }

    @Test
    void check_deployment_status_multiple_servers() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            mockAuthenticationOk(request)
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        data: [
                                id             : 'appid',
                                serverArtifacts: [
                                        [
                                                id           : 'wrongone',
                                                desiredStatus: 'STARTED',
                                                artifact     : [
                                                        fileName: 'wrong.zip'
                                                ]
                                        ],
                                        [
                                                id                : 'artid1',
                                                desiredStatus     : 'STARTED',
                                                lastReportedStatus: 'STARTING',
                                                artifact          : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ],
                                        [
                                                id                : 'artid2',
                                                desiredStatus     : 'STARTED',
                                                lastReportedStatus: 'STARTED',
                                                artifact          : [
                                                        fileName: 'theFile.zip'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'appid',
                                           'theFile.zip')

        // assert
        assertThat status.toList(),
                   is(equalTo([
                           OnPremDeploymentStatus.STARTING,
                           OnPremDeploymentStatus.STARTED
                   ]))
    }

    def mockInitialDeployment(HttpServerRequest request) {
        def uri = request.absoluteURI()
        mockAuthenticationOk(request)
        if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
            return mockEnvironments(request)
        }
        def result = null
        if (uri.endsWith('environments')) {
            result = [
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
            ]
        } else if (uri.endsWith('servers')) {
            result = [
                    data: [
                            [
                                    id  : 'abc123',
                                    name: 'serverb'
                            ],
                            [
                                    id         : 'def456',
                                    name       : 'servera',
                                    clusterId  : 'cluster1',
                                    clusterName: 'clustera'
                            ]
                    ]
            ]
        } else if (uri.endsWith('applications') && request.method().name() == 'GET') {
            result = [
                    data: [
                            [
                                    id  : 'abc123',
                                    name: 'app1'
                            ],
                            [
                                    id  : 'def456',
                                    name: 'the-app'
                            ]
                    ]
            ]
        } else if (uri.endsWith('applications') && request.method().name() == 'POST') {
            result = [
                    data: [
                            id             : 1234,
                            serverArtifacts: [
                                    // comes back blank for new apps so be conservative here
                            ]
                    ]
            ]
        }

        request.response().with {
            statusCode = 200
            putHeader('Content-Type',
                      'application/json')
            end(JsonOutput.toJson(result))
        }
    }

    def getAppStatusJson(String desired,
                         String reported1,
                         String reported2,
                         String fileName) {
        def serverArtifacts = [
                [
                        id           : 'wrongone',
                        desiredStatus: desired,
                        artifact     : [
                                fileName: 'wrong.zip'
                        ]
                ],
                [
                        id                : 'artid1',
                        desiredStatus     : desired,
                        lastReportedStatus: reported1,
                        artifact          : [
                                fileName: fileName
                        ]
                ],
                [
                        id                : 'artid2',
                        desiredStatus     : desired,
                        lastReportedStatus: reported2,
                        artifact          : [
                                fileName: fileName
                        ]
                ]
        ]
        [
                data: [
                        id             : '1234',
                        serverArtifacts: serverArtifacts
                ]
        ]
    }

    @Test
    void perform_deployment_succeeds_after_1_try() {
        // arrange
        def tries = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (!(uri.endsWith('applications/1234') && request.method().name() == 'GET')) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = tries == 0 ?
                        getAppStatusJson('UPDATED',
                                         'IDK',
                                         'IDK',
                                         'some_file.txt') :
                        getAppStatusJson('STARTED',
                                         'STARTED',
                                         'STARTED',
                                         'some_file.txt')
                tries++
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        deployer.deploy(request)

        // assert
    }

    @Test
    void perform_deployment_succeeds_after_2_tries() {
        // arrange
        def tries = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (!(uri.endsWith('applications/1234') && request.method().name() == 'GET')) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (tries == 0) {
                    result = getAppStatusJson('UPDATED',
                                              'IDK',
                                              'IDK',
                                              'some_file.txt')
                } else {
                    result = tries == 1 ?
                            getAppStatusJson('STARTED',
                                             'STARTED',
                                             'STARTING',
                                             'some_file.txt') :
                            getAppStatusJson('STARTED',
                                             'STARTED',
                                             'STARTED',
                                             'some_file.txt')
                }
                tries++
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        deployer.deploy(request)
    }

    @Test
    void perform_deployment_times_out() {
        // arrange
        def tries = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (!(uri.endsWith('applications/1234') && request.method().name() == 'GET')) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = tries == 0 ?
                        getAppStatusJson('UPDATED',
                                         'IDK',
                                         'IDK',
                                         'some_file.txt') :
                        getAppStatusJson('STARTED',
                                         'STARTING',
                                         'STARTED',
                                         'some_file.txt')
                tries++
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Deployment has not failed but app has not started after 10 tries!'))
    }

    @Test
    void perform_deployment_eventually_fails() {
        // arrange
        def tries = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (!(uri.endsWith('applications/1234') && request.method().name() == 'GET')) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = tries == 0 ?
                        getAppStatusJson('UPDATED',
                                         'IDK',
                                         'IDK',
                                         'some_file.txt') :
                        getAppStatusJson('STARTED',
                                         'DEPLOYMENT_FAILED',
                                         'STARTED',
                                         'some_file.txt')
                tries++
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
                                                  'clustera',
                                                  file.name,
                                                  stream)

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Deployment failed on 1 or more nodes. Please see logs and messages as to why app did not start'))
    }
}
