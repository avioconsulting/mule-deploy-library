package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.internal.models.OnPremDeploymentStatus
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpServerRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class OnPremDeployerTest extends BaseTest {
    private OnPremDeployer deployer

    @BeforeEach
    void clean() {
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new OnPremDeployer(this.clientWrapper,
                                      environmentLocator,
                                      500,
                                      10,
                                      new TestConsoleLogger(),
                                      dryRunMode)
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
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
                   is(equalTo('/hybrid/api/v1/servers'))
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
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
                new ApplicationName('the-app', null, null))

        // assert
        assertThat url,
                   is(equalTo('/hybrid/api/v1/applications'))
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
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
                                               new ApplicationName('non-existent-app', null, null))

        // assert
        assertThat 'app not being there is not an exception',
                   appId,
                   is(nullValue())
    }

    @Test
    void locate_app_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
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
                new ApplicationName('the-app', null, null))

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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/hybrid/api/v1/applications'))
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
    void perform_deployment_correct_request_auto_discovery() {
        // arrange
        MultiMap sentFormAttributes = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
                    request.expectMultipart = true
                    sentFormAttributes = request.formAttributes()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')
        request.setAutoDiscoveryId('the.auto.disc.prop',
                                   '1234')

        // act
        deployer.deploy(request)

        // assert
        def configMap = new JsonSlurper().parseText(sentFormAttributes.get('configuration'))
        assertThat configMap,
                   is(equalTo([
                           'mule.agent.application.properties.service': [
                                   applicationName: 'new-app',
                                   properties     : [
                                           'the.auto.disc.prop': '1234'
                                   ]
                           ]
                   ]))
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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3',
                                                  [prop1: 'foo', prop2: 'bar'])

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/hybrid/api/v1/applications'))
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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('the-app', null, null),
                                                  '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/hybrid/api/v1/applications/app456'))
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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('the-app', null, null),
                                                  '1.2.3',
                                                  [prop1: 'foo', prop2: 'bar'])

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/hybrid/api/v1/applications/app456'))
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
    void check_deployment_requests_properly_gets_status_updated() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
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
                   is(equalTo('/hybrid/api/v1/applications/appid'))
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
        if (mockAuthenticationOk(request)) {
            return
        }
        if (mockEnvironments(request)) {
            return
        }
        def result = null
        if (uri.endsWith('servers')) {
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

    static def getAppStatusJson(String desired,
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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                                                  new ApplicationName('new-app', null, null),
                                                  '1.2.3')

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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

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
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Deployment failed on 1 or more nodes. Please see logs and messages as to why app did not start'))
    }

    @Test
    void isMule4Request_no() {
        // arrange
        def file = new File('src/test/resources/some_file.zip')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

        // act
        def result = deployer.isMule4Request(request)

        // assert
        assertThat result,
                   is(equalTo(false))
    }

    @Test
    void isMule4Request_yes() {
        // arrange
        def file = new File('src/test/resources/some_file.jar')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                new ApplicationName('new-app', null, null),
                                                  '1.2.3')

        // act
        def result = deployer.isMule4Request(request)

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void deploy_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result
                if (uri.endsWith('servers')) {
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
                    deployed = true
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
                    request.expectMultipart = true
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                                                    new ApplicationName('new-app', null, null),
                                                  '1.2.3')
        // act
        deployer.deploy(request)

        // assert
        assertThat deployed,
                   is(equalTo(false))
    }
}
