package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.models.AwsRegions
import com.avioconsulting.jenkins.mule.impl.models.WorkerTypes
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

class CloudHubDeployerTest implements HttpServerUtils {
    HttpServer httpServer
    private CloudHubDeployer deployer
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
        deployer = new CloudHubDeployer(this.clientWrapper,
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
    void check_deployment_requests_properly() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
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
                                        instances: [
                                                [
                                                        status: 'TERMINATED'
                                                ]
                                        ]
                                ],
                                [
                                        instances: [
                                                [
                                                        status: 'WRONG_ONE'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        deployer.getDeploymentStatus('DEV',
                                     'theapp')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications/theapp/deployments?orderByDate=DESC'))
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
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
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
                                        instances: [
                                                [
                                                        status: 'TERMINATED'
                                                ]
                                        ]
                                ],
                                [
                                        instances: [
                                                [
                                                        status: 'WRONG_ONE'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getDeploymentStatus('DEV',
                                                  'the-app')

        // assert
        assertThat status.toList(),
                   is(equalTo([DeploymentStatus.FAILED]))
    }

    @Test
    void check_deployment_status_started() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
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
                                        instances: [
                                                [
                                                        status: 'STARTED'
                                                ]
                                        ]
                                ],
                                [
                                        instances: [
                                                [
                                                        status: 'WRONG_ONE'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getDeploymentStatus('DEV',
                                                  'appid'
        )

        // assert
        assertThat status.toList(),
                   is(equalTo([DeploymentStatus.STARTED]))
    }

    @Test
    void check_deployment_status_starting() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
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
                                        instances: [
                                                [
                                                        status: 'DEPLOYING'
                                                ]
                                        ]
                                ],
                                [
                                        instances: [
                                                [
                                                        status: 'WRONG_ONE'
                                                ]
                                        ]
                                ]
                        ]
                ]))
            }
        }

        // act
        def status = deployer.getDeploymentStatus('DEV',
                                                  'appid'
        )

        // assert
        assertThat status.toList(),
                   is(equalTo([DeploymentStatus.STARTING]))
    }

    static def getDeploymentStatusJson(String worker1Status,
                                       String worker2Status) {
        [
                data: [
                        [
                                instances: [
                                        [
                                                status: worker1Status
                                        ],
                                        [
                                                status: worker2Status
                                        ]
                                ]
                        ],
                        [
                                instances: [
                                        [
                                                status: 'WRONG_ONE'
                                        ]
                                ]
                        ]
                ]
        ]
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'new-app'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'CLIENT',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_no_region() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'new-app'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'CLIENT',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_gav_new_app() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        String sentContentType = null
        Map sentBody = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'new-app'
                    ]
                    url = uri
                    method = request.method()
                    (authToken, orgId, envId) = capturedStandardHeaders(request)
                    sentContentType = request.getHeader('Content-Type')
                    request.bodyHandler { buffer ->
                        def rawBody = buffer.toString()
                        sentBody = new JsonSlurper().parseText(rawBody)
                    }
                }
                end(JsonOutput.toJson(result))
            }
        }

        // act
        deployer.deployFromExchange('DEV',
                                    'new-app',
                                    'CLIENT',
                                    'com.group',
                                    '1.0.0',
                                    'theKey',
                                    '3.9.1',
                                    false,
                                    WorkerTypes.Micro,
                                    1,
                                    'theClientId',
                                    'theSecret',
                                    AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentContentType,
                   is(equalTo('application/json; charset=UTF-8'))
        assertThat sentBody,
                   is(equalTo([
                           applicationSource: [
                                   groupId   : 'com.group',
                                   artifactId: 'new-app',
                                   version   : '1.0.0',
                                   source    : 'EXCHANGE'
                           ],
                           applicationInfo  : [
                                   domain               : 'client-new-app-dev',
                                   muleVersion          : [
                                           version: '3.9.1'
                                   ],
                                   region               : 'us-east-1',
                                   monitoringAutoRestart: true,
                                   workers              : [
                                           type  : [
                                                   name: 'Micro'
                                           ],
                                           amount: 1
                                   ],
                                   persistentQueues     : false,
                                   properties           : [
                                           env                              : 'dev',
                                           'crypto.key'                     : 'theKey',
                                           'anypoint.platform.client_id'    : 'theClientId',
                                           'anypoint.platform.client_secret': 'theSecret'
                                   ],
                                   fileName             : 'new-app-1.0.0.zip'
                           ],
                           autoStart        : true
                   ]))
    }

    @Test
    void perform_deployment_correct_request_new_app_property_overrides_runtime_manager() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'new-app'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'CLIENT',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1,
                                [prop1: 'foo', prop2: 'bar'])

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret',
                                   prop1                            : 'foo',
                                   prop2                            : 'bar'
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'new-app'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'CLIENT',
                                stream,
                                zipFile.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1,
                                [existing: 'changed'],
                                [:],
                                'api.dev.properties')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
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
    void perform_deployment_correct_request_new_app_persistent_queues() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                true,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : true,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_other_props() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env: 'TST'
                ]
        ] as Map<String, String>

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1,
                                [:],
                                otherProperties)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : true,
                           properties           : [
                                   env                              : 'TST',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_other_props_only_ch_settings() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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
        def otherProperties = [
                persistentQueues: true
        ] as Map<String, String>

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1,
                                [:],
                                otherProperties)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : true,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_correct_request_new_app_other_props_both() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env  : 'TST',
                        prop1: 'should not see this'
                ]
        ] as Map<String, String>

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1,
                                [prop1: 'foo', prop2: 'bar'],
                                otherProperties)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : true,
                           properties           : [
                                   env                              : 'TST',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret',
                                   prop1                            : 'foo',
                                   prop2                            : 'bar'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_upper_case() {
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
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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

        // act
        deployer.deployFromFile('DEV',
                                'NEW-APP',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_space_in_app() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        // act
        def exception = shouldFail {
            deployer.deployFromFile('DEV',
                                    'some app name',
                                    'client',
                                    stream,
                                    file.name,
                                    'theKey',
                                    '3.9.1',
                                    false,
                                    WorkerTypes.Micro,
                                    1,
                                    'theClientId',
                                    'theSecret',
                                    AwsRegions.UsEast1)
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }

    @Test
    void perform_deployment_fails_immediately() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    statusCode = 404
                } else {
                    // deployment service returns this
                    statusCode = 500
                    result = [
                            message: 'some message from CH'
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        def exception = shouldFail {
            deployer.deployFromFile('DEV',
                                    'new-app',
                                    'client',
                                    stream,
                                    file.name,
                                    'theKey',
                                    '3.9.1',
                                    false,
                                    WorkerTypes.Micro,
                                    1,
                                    'theClientId',
                                    'theSecret',
                                    AwsRegions.UsEast1)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Unable to deploy application, got an HTTP 500 with a response of \'{"message":"some message from CH"}\''))
    }

    @Test
    void perform_deployment_existing_app() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        def firstCheck = true
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    // existing app check + status is the same
                    if (firstCheck) {
                        statusCode = 200
                        firstCheck = false
                        result = getDeploymentStatusJson('STARTED',
                                                         'STARTED')
                    } else {
                        statusCode = 200
                        // we test most of this in other methods
                        result = getDeploymentStatusJson('STARTED',
                                                         'STARTED')
                    }
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications/client-new-app-dev'))
        assertThat method,
                   is(equalTo('PUT'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_existing_app_failed_last_but_has_started_ok_once() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        MultiMap sentFormAttributes = null
        String rawBody = null
        def firstCheck = true
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    // existing app check + status is the same
                    if (firstCheck) {
                        statusCode = 200
                        firstCheck = false
                        result = [
                                data: [
                                        [
                                                instances: [
                                                        [
                                                                status: 'STARTED'
                                                        ]
                                                ]
                                        ],
                                        [
                                                instances: [
                                                        [
                                                                status: 'TERMINATED'
                                                        ]
                                                ]
                                        ]
                                ]
                        ]
                    } else {
                        statusCode = 200
                        // we test most of this in other methods
                        result = [
                                data: [
                                        [
                                                instances: [
                                                        [
                                                                status: 'STARTED'
                                                        ]
                                                ]
                                        ],
                                        [
                                                instances: [
                                                        [
                                                                status: 'TERMINATED'
                                                        ]
                                                ]
                                        ],
                                        [
                                                instances: [
                                                        [
                                                                status: 'STARTED'
                                                        ]
                                                ]
                                        ]
                                ]
                        ]
                    }
                } else {
                    // deployment service returns this
                    statusCode = 200
                    result = [
                            domain: 'client-new-app-dev'
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

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications/client-new-app-dev'))
        assertThat method,
                   is(equalTo('PUT'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        assertThat orgId,
                   is(equalTo('the-org-id'))
        assertThat sentFormAttributes.names().toList().sort(),
                   is(equalTo(['appInfoJson', 'autoStart']))
        assertThat sentFormAttributes.get('autoStart'),
                   is(equalTo('true'))
        def map = new JsonSlurper().parseText(sentFormAttributes.get('appInfoJson'))
        assertThat map,
                   is(equalTo([
                           domain               : 'client-new-app-dev',
                           muleVersion          : [
                                   version: '3.9.1'
                           ],
                           region               : 'us-east-1',
                           monitoringAutoRestart: true,
                           workers              : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           persistentQueues     : false,
                           properties           : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
        assertThat rawBody,
                   is(containsString('Content-Disposition: form-data; name="file"; filename="some_file.txt"'))
    }

    @Test
    void perform_deployment_existing_stopped_app() {
        // arrange
        def firstCheck = true
        def appDeleteRequested = false
        def appDeleteComplete = false
        def firstCheckAfterDeleteComplete = false
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    if (firstCheck) {
                        statusCode = 200
                        firstCheck = false
                        result = [
                                status: 'UNDEPLOYED'
                        ]
                    } else if (appDeleteRequested && !firstCheckAfterDeleteComplete) {
                        // the deletion operation takes a second, simulate that
                        firstCheckAfterDeleteComplete = true
                        statusCode = 200
                        result = getDeploymentStatusJson('TERMINATED',
                                                         'TERMINATED')
                    } else if (appDeleteRequested && firstCheckAfterDeleteComplete) {
                        // now our app is deleted
                        appDeleteComplete = true
                        statusCode = 404
                    } else {
                        statusCode = 500
                        result = [
                                message: 'Not sure how we got in this state'
                        ]
                    }
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    assert deployed: 'We should not be calling this until we have done our POST'
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'DELETE') {
                    appDeleteRequested = true
                    statusCode = 200
                } else if (uri.endsWith('applications') && request.method().name() == 'POST') {
                    // deployment service returns this
                    if (!appDeleteComplete) {
                        println 'App was not deleted first! Returning 500'
                        statusCode = 500
                        result = [
                                message: 'You did not delete the app first!'
                        ]
                    } else {
                        statusCode = 200
                        deployed = true
                        result = [
                                domain: 'client-new-app-dev'
                        ]
                    }
                } else {
                    statusCode = 500
                    result = [
                            message: "Not sure how we got in this state ${request.method()} ${request.uri()}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        // our mock assertions should do the work here
    }

    @Test
    void perform_deployment_existing_failed_app() {
        // arrange
        def firstCheck = true
        def appDeleteRequested = false
        def appDeleteComplete = false
        def firstCheckAfterDeleteComplete = false
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            if (uri == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
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
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                    if (firstCheck) {
                        statusCode = 200
                        firstCheck = false
                        result = [
                                status: 'DEPLOY_FAILED'
                        ]
                    } else if (appDeleteRequested && !firstCheckAfterDeleteComplete) {
                        // the deletion operation takes a second, simulate that
                        firstCheckAfterDeleteComplete = true
                        statusCode = 200
                        result = getDeploymentStatusJson('TERMINATED',
                                                         'TERMINATED')
                    } else if (appDeleteRequested && firstCheckAfterDeleteComplete) {
                        // now our app is deleted
                        appDeleteComplete = true
                        statusCode = 404
                    } else {
                        statusCode = 500
                        result = [
                                message: 'Not sure how we got in this state'
                        ]
                    }
                } else if (uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET') {
                    assert deployed: 'We should not be calling this until we have done our POST'
                    statusCode = 200
                    // we test most of this in other methods
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'DELETE') {
                    appDeleteRequested = true
                    statusCode = 200
                } else if (uri.endsWith('applications') && request.method().name() == 'POST') {
                    // deployment service returns this
                    if (!appDeleteComplete) {
                        println 'App was not deleted first! Returning 500'
                        statusCode = 500
                        result = [
                                message: 'You did not delete the app first!'
                        ]
                    } else {
                        statusCode = 200
                        deployed = true
                        result = [
                                domain: 'client-new-app-dev'
                        ]
                    }
                } else {
                    statusCode = 500
                    result = [
                            message: "Not sure how we got in this state ${request.method()} ${request.uri()}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        // our mock assertions should do the work here
    }

    def mockInitialDeployment(HttpServerRequest request) {
        def uri = request.absoluteURI()
        if (uri == 'http://localhost:8080/accounts/login') {
            return mockAuthenticationOk(request)
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
            } else if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
                statusCode = 404
            } else {
                // deployment service returns this
                statusCode = 200
                result = [
                        domain: 'client-new-app-dev'
                ]
            }
            end(JsonOutput.toJson(result))
        }
    }

    @Test
    void perform_deployment_succeeds_after_1_try() {
        // arrange
        def deployStatusCheckCount = 0
        def appStatusCheckCount = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            def isAppStatusCheck = uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET'
            def isDeployStatusCheck = uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET'
            // 'outsource' everything for the initial deployment to mockInitialDeployment
            if (isDeployStatusCheck) {
                deployStatusCheckCount++
            }
            if (isAppStatusCheck) {
                appStatusCheckCount++
            }
            // let mockInitialDeployment handle the 1st status check
            if (!isDeployStatusCheck || isAppStatusCheck) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (isDeployStatusCheck) {
                    statusCode = 200
                    result = getDeploymentStatusJson('STARTED',
                                                     'STARTED')
                } else {
                    statusCode = 500
                    result = [
                            message: "How did we get here ${request.method()} - ${uri} - statusCheckCount ${deployStatusCheckCount}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat 'We should check status twice. The initial one for the app (app) and then to see if app started',
                   appStatusCheckCount,
                   is(equalTo(1))
        assertThat 'We should check status twice. The initial one for the app (app) and then to see if app started',
                   deployStatusCheckCount,
                   is(equalTo(1))
    }

    @Test
    void perform_deployment_succeeds_after_2_tries() {
        // arrange
        def deployStatusCheckCount = 0
        def appStatusCheckCount = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            def isAppStatusCheck = uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET'
            def isDeployStatusCheck = uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET'
            // 'outsource' everything for the initial deployment to mockInitialDeployment
            if (isDeployStatusCheck) {
                deployStatusCheckCount++
            }
            if (isAppStatusCheck) {
                appStatusCheckCount++
            }
            // let mockInitialDeployment handle the 1st status check
            if (!isDeployStatusCheck || isAppStatusCheck) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (isDeployStatusCheck) {
                    statusCode = 200
                    result = deployStatusCheckCount == 1 ? getDeploymentStatusJson('DEPLOYING',
                                                                                   'DEPLOYING') :
                            getDeploymentStatusJson('STARTED',
                                                    'STARTED')
                } else {
                    statusCode = 500
                    result = [
                            message: "How did we get here ${request.method()} - ${uri} - statusCheckCount ${statusCheckCount}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        deployer.deployFromFile('DEV',
                                'new-app',
                                'client',
                                stream,
                                file.name,
                                'theKey',
                                '3.9.1',
                                false,
                                WorkerTypes.Micro,
                                1,
                                'theClientId',
                                'theSecret',
                                AwsRegions.UsEast1)

        // assert
        assertThat 'We should check status 3 times. The initial one for the app and then 2 to see if app started',
                   deployStatusCheckCount,
                   is(equalTo(2))
        assertThat 'We should check status 3 times. The initial one for the app and then 2 to see if app started',
                   appStatusCheckCount,
                   is(equalTo(1))
    }

    @Test
    void perform_deployment_times_out() {
        // arrange
        def statusCheckCount = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            def isAppStatusCheck = uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET'
            def isDeployStatusCheck = uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET'
            // 'outsource' everything for the initial deployment to mockInitialDeployment
            if (isDeployStatusCheck) {
                statusCheckCount++
            }
            // let mockInitialDeployment handle the 1st status check
            if (!isDeployStatusCheck || isAppStatusCheck) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (isDeployStatusCheck) {
                    statusCode = 200
                    result = getDeploymentStatusJson('DEPLOYING',
                                                     'DEPLOYING')
                } else {
                    statusCode = 500
                    result = [
                            message: "How did we get here ${request.method()} - ${uri} - statusCheckCount ${statusCheckCount}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        def exception = shouldFail {
            deployer.deployFromFile('DEV',
                                    'new-app',
                                    'client',
                                    stream,
                                    file.name,
                                    'theKey',
                                    '3.9.1',
                                    false,
                                    WorkerTypes.Micro,
                                    1,
                                    'theClientId',
                                    'theSecret',
                                    AwsRegions.UsEast1)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Deployment has not failed but app has not started after 10 tries!'))
    }

    @Test
    void perform_deployment_eventually_fails() {
        // arrange
        def statusCheckCount = 0
        withHttpServer { HttpServerRequest request ->
            def uri = request.absoluteURI()
            def isAppStatusCheck = uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET'
            def isDeployStatusCheck = uri.endsWith('applications/client-new-app-dev/deployments?orderByDate=DESC') && request.method().name() == 'GET'
            // 'outsource' everything for the initial deployment to mockInitialDeployment
            if (isDeployStatusCheck) {
                statusCheckCount++
            }
            // let mockInitialDeployment handle the 1st status check
            if (!isDeployStatusCheck || isAppStatusCheck) {
                return mockInitialDeployment(request)
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (isDeployStatusCheck) {
                    statusCode = 200
                    result = statusCheckCount == 2 ? getDeploymentStatusJson('DEPLOYING',
                                                                             'DEPLOYING') :
                            getDeploymentStatusJson('TERMINATED',
                                                    'TERMINATED')
                } else {
                    statusCode = 500
                    result = [
                            message: "How did we get here ${request.method()} - ${uri} - statusCheckCount ${statusCheckCount}"
                    ]
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)

        // act
        def exception = shouldFail {
            deployer.deployFromFile('DEV',
                                    'new-app',
                                    'client',
                                    stream,
                                    file.name,
                                    'theKey',
                                    '3.9.1',
                                    false,
                                    WorkerTypes.Micro,
                                    1,
                                    'theClientId',
                                    'theSecret',
                                    AwsRegions.UsEast1)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Deployment failed on 1 or more workers. Please see logs and messages as to why app did not start'))
    }

    @Test
    void getAppStatus_correct_request() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
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
                        status: 'UNDEPLOYED'
                ]))
            }
        }

        // act
        deployer.getAppStatus('DEV',
                              'theapp')

        // assert
        assertThat url,
                   is(equalTo('http://localhost:8080/cloudhub/api/v2/applications/theapp'))
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
    void getAppStatus_undeployed() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        status: 'UNDEPLOYED'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Undeployed))
    }

    @Test
    void getAppStatus_failed() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        status: 'DEPLOY_FAILED'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Failed))
    }

    @Test
    void getAppStatus_started() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        status: 'STARTED'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Started))
    }

    @Test
    void getAppStatus_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 404
                end()
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.NotFound))
    }

    @Test
    void getAppStatus_unknown() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (request.absoluteURI() == 'http://localhost:8080/accounts/login') {
                return mockAuthenticationOk(request)
            }
            if (request.absoluteURI() == 'http://localhost:8080/accounts/api/organizations/the-org-id/environments') {
                return mockEnvironments(request)
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        status: 'FOOBAR'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Unknown))
    }
}
