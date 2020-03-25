package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.WorkerTypes
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings("GroovyAccessibility")
class CloudHubDeployerTest extends BaseTest {
    private CloudHubDeployer deployer
    private int statusCheckCount
    private int maxTries

    @Before
    void clean() {
        statusCheckCount = 0
        maxTries = 10
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new CloudHubDeployer(this.clientWrapper,
                                        environmentLocator,
                                        500,
                                        maxTries,
                                        new TestConsoleLogger(),
                                        dryRunMode)
    }

    static final Map<AppStatus, String> ReverseAppStatusMappings = AppStatusMapper.AppStatusMappings.collectEntries {
        k, v ->
            [v, k]
    } as Map<AppStatus, String>
    static final Map<DeploymentUpdateStatus, String> ReverseDeployUpdateStatusMappings = AppStatusMapper.DeployUpdateStatusMappings.collectEntries {
        k, v ->
            [v, k]
    } as Map<DeploymentUpdateStatus, String>

    static def getAppDeployResponsePayload(String appName) {
        [
                domain: appName
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('new-app')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',)

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('new-app')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('new-app')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',
                                                    [prop1: 'foo', prop2: 'bar'])

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  true,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env: 'TST'
                ]
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',
                                                    [:],
                                                    otherProperties)

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def otherProperties = [
                persistentQueues: true
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',
                                                    [:],
                                                    otherProperties)
        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env  : 'TST',
                        prop1: 'should not see this'
                ]
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',
                                                    [prop1: 'foo', prop2: 'bar'],
                                                    otherProperties)

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
    void perform_deployment_correct_request_upper_case() {
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
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'NEW-APP',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications'))
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
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Deploying),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications/client-new-app-dev'))
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
    void perform_deployment_space_in_app() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        // act
        def exception = shouldFail {
            new CloudhubDeploymentRequest('DEV',
                                          new CloudhubWorkerSpecRequest('3.9.1',
                                                                        false,
                                                                        1,
                                                                        WorkerTypes.Micro,
                                                                        AwsRegions.UsEast1),
                                          file,
                                          'theKey',
                                          'theClientId',
                                          'theSecret',
                                          'client',
                                          'some app name',
                                          '1.2.3')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }

    @Test
    void perform_deployment_fails_immediately() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 500
                def result = [
                        message: 'some message from CH'
                ]
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat exception.message,
                   is(equalTo('Unable to deploy application, got an HTTP 500 with a response of \'{"message":"some message from CH"}\''))
    }

    @Test
    void perform_deployment_existing_app_succeeds() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Deploying),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat statusCheckCount,
                   is(equalTo(3))
    }

    @Test
    void perform_deployment_existing_app_fails() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Deploying),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Failed))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat exception.message,
                   is(containsString('Deployment failed on 1 or more workers. Please see logs and messages as to why app did not start'))
    }

    @Test
    void perform_deployment_existing_app_failed_last_but_has_started_ok_once() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Failed),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Deploying),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('client-new-app-dev')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat statusCheckCount,
                   is(equalTo(3))
    }

    @Test
    void perform_deployment_existing_stopped_app() {
        // arrange
        def deployed = false
        def appStartRequested = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('applications/client-new-app-dev/status') && request.method() == HttpMethod.POST && deployed) {
                    appStartRequested = true
                    statusCode = 200
                } else if (uri == '/cloudhub/api/v2/applications/client-new-app-dev' && request.method() == HttpMethod.PUT) {
                    // deployment service returns this
                    statusCode = 200
                    deployed = true
                    result = getAppDeployResponsePayload('client-new-app-dev')
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
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat appStartRequested,
                   is(equalTo(true))
    }

    @Test
    void perform_deployment_existing_failed_app_never_successful_but_this_one_is() {
        // arrange
        def appStartRequested = false
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Failed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Failed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('applications/client-new-app-dev/status') && request.method() == HttpMethod.POST && deployed) {
                    appStartRequested = true
                    statusCode = 200
                } else if (uri == '/cloudhub/api/v2/applications/client-new-app-dev' && request.method() == HttpMethod.PUT) {
                    // deployment service returns this
                    statusCode = 200
                    deployed = true
                    result = getAppDeployResponsePayload('client-new-app-dev')
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
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat appStartRequested,
                   is(equalTo(true))
        assertThat deployed,
                   is(equalTo(true))
    }

    @Test
    void perform_deployment_existing_failed_app_never_successful_but_this_one_is_dry_run_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def appStartRequested = false
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Failed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Failed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                if (uri.endsWith('applications/client-new-app-dev/status') && request.method() == HttpMethod.POST && deployed) {
                    appStartRequested = true
                    statusCode = 200
                } else if (uri == '/cloudhub/api/v2/applications/client-new-app-dev' && request.method() == HttpMethod.PUT) {
                    // deployment service returns this
                    statusCode = 200
                    deployed = true
                    result = getAppDeployResponsePayload('client-new-app-dev')
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
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat appStartRequested,
                   is(equalTo(false))
        assertThat deployed,
                   is(equalTo(false))
        assertThat 'We should only check status once since we are in dry run mode',
                   statusCheckCount,
                   is(equalTo(1))
    }

    def mockDeploymentAndXStatusChecks(HttpServerRequest request,
                                       AppStatusPackage... appStatuses) {
        def uri = request.absoluteURI()
        if (mockAuthenticationOk(request)) {
            return true
        }
        if (mockEnvironments(request)) {
            return true
        }
        if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
            statusCheckCount++
            println "mock status invocation ${statusCheckCount}/${appStatuses.size()}"
            if (statusCheckCount <= appStatuses.length) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                              'application/json')
                    def result = null
                    def status = appStatuses[statusCheckCount - 1]
                    if (status.appStatus == AppStatus.NotFound) {
                        statusCode = 404
                    } else {
                        // deployment service returns this
                        statusCode = 200
                        result = [
                                domain: 'client-new-app-dev',
                                status: ReverseAppStatusMappings[status.appStatus]
                        ]
                        def deploymentUpdateStatusString = ReverseDeployUpdateStatusMappings[status.deploymentUpdateStatus]
                        if (deploymentUpdateStatusString) {
                            result['deploymentUpdateStatus'] = deploymentUpdateStatusString
                        }
                    }
                    end(JsonOutput.toJson(result))
                }
                return true
            }
        }
        return false
    }

    @Test
    void perform_deployment_succeeds_after_1_try() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Undeployed,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat statusCheckCount,
                   is(equalTo(4))
    }

    @Test
    void perform_deployment_succeeds_after_2_tries() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        deployer.deploy(request)

        // assert
        assertThat statusCheckCount,
                   is(equalTo(4))
    }

    @Test
    void perform_deployment_times_out() {
        // arrange
        def appStatusesWeWillReturn = [AppStatus.NotFound] + (1..maxTries).collect { AppStatus.Deploying }
        def appStatusPackagesWeWillReturn = appStatusesWeWillReturn.collect { status ->
            new AppStatusPackage(status,
                                 null)
        }
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               appStatusPackagesWeWillReturn.toArray(new AppStatusPackage[0]) as AppStatusPackage[])) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat statusCheckCount,
                   is(equalTo(appStatusesWeWillReturn.size()))
        assertThat exception.message,
                   is(equalTo('Deployment has not failed but app has not started after 10 tries!'))
    }

    @Test
    void perform_deployment_eventually_fails() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.NotFound,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Deploying,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Failed,
                                                                    null))) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppDeployResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat statusCheckCount,
                   is(equalTo(5))
        assertThat exception.message,
                   is(equalTo('Deployment failed on 1 or more workers. Please see logs and messages as to why app did not start'))
    }

    @Test
    void deploy_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            if (mockDeploymentAndXStatusChecks(request,
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    DeploymentUpdateStatus.Deploying),
                                               new AppStatusPackage(AppStatus.Started,
                                                                    null))) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                deployed = true
                def result = getAppDeployResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3',)
        // act
        deployer.deploy(request)

        // assert
        assertThat deployed,
                   is(equalTo(false))
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
                        status: 'UNDEPLOYED'
                ]))
            }
        }

        // act
        deployer.getAppStatus('DEV',
                              'theapp')

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/v2/applications/theapp'))
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
    void getAppStatus_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
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
                   is(equalTo(new AppStatusPackage(AppStatus.NotFound,
                                                   null)))
    }

    @Test
    void isMule4Request_no() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
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
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('4.2.2',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // act
        def result = deployer.isMule4Request(request)

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void startApplication() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
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
                // deployment service returns this
                statusCode = 200
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end()
            }
        }

        // act
        deployer.startApplication('DEV',
                                  'the-app')

        // assert
        assertThat url,
                   is(equalTo('/cloudhub/api/applications/the-app/status'))
        assertThat method,
                   is(equalTo('POST'))
        assertThat authToken,
                   is(equalTo('Bearer the token'))
        assertThat envId,
                   is(equalTo('def456'))
        def map = new JsonSlurper().parseText(rawBody)
        assertThat map,
                   is(equalTo([
                           status: 'start'
                   ]))
    }
}
