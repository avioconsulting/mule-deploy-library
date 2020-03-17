package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.WorkerTypes
import com.avioconsulting.mule.deployment.internal.models.AppStatus
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

class CloudHubDeployerTest extends BaseTest {
    private CloudHubDeployer deployer
    private int statusCheckCount
    private int maxTries

    @Before
    void setupDeployer() {
        statusCheckCount = 0
        maxTries = 10
        deployer = new CloudHubDeployer(this.clientWrapper,
                                        environmentLocator,
                                        500,
                                        maxTries,
                                        System.out)
    }

    static final Map<AppStatus, String> ReverseAppStatusMappings = CloudHubDeployer.AppStatusMappings.collectEntries {
        k, v ->
            [v, k]
    }

    static def getAppResponsePayload(String appName,
                                     AppStatus appStatus = null) {
        def map = [
                domain: appName
        ]
        // reason for this is we're sharing this method between app status GETs and app deployment POST/PUTs
        // but we don't really check or care about the status response from the POST/PUT
        if (appStatus) {
            map['status'] = ReverseAppStatusMappings[appStatus]
        }
        return map
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
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('new-app')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('new-app')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('new-app')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('new-app')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.uploadHandler { upload ->
                    println "got ZIP file " + upload.filename()
                    upload.streamToFileSystem(newZipFile.absolutePath)
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
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    zipFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    [existing: 'changed'],
                                                    'api.dev.properties')

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
        def destination = new File('target/temp/modifiedapp')
        Exception problem = null
        5.times {
            if (destination.exists()) {
                assert destination.deleteDir()
            }
            try {
                antBuilder.unzip(src: newZipFile.absolutePath,
                                 dest: destination)
                def newProps = new Properties()
                newProps.load(new FileInputStream(new File(destination,
                                                           'classes/api.dev.properties')))
                assertThat newProps,
                           is(equalTo([
                                   existing: 'changed',
                           ]))
                problem = null
            }
            catch (e) {
                problem = e
                println 'Problem with zip, waiting 500ms and retrying due to async web server'
                Thread.sleep(500)
            }
        }
        if (problem) {
            throw problem
        }
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  true,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
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
                                                    'new-app',
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def otherProperties = [
                persistentQueues: true
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
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
                                                    'new-app',
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
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'NEW-APP',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
    void perform_deployment_space_in_app() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        // act
        def exception = shouldFail {
            new CloudhubDeploymentRequest('DEV',
                                          'some app name',
                                          new CloudhubWorkerSpecRequest('3.9.1',
                                                                        false,
                                                                        1,
                                                                        WorkerTypes.Micro,
                                                                        AwsRegions.UsEast1),
                                          file,
                                          'theKey',
                                          'theClientId',
                                          'theSecret',
                                          'client')
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
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 500
                result = [
                        message: 'some message from CH'
                ]
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
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
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               // before we deploy
                                               AppStatus.Started,
                                               // after we deploy but before it starts
                                               AppStatus.Started,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
    void perform_deployment_existing_app_failed_last_but_has_started_ok_once() {
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
            if (mockDeploymentAndXStatusChecks(request,
                                               // before we deploy
                                               AppStatus.Started,
                                               // after we deploy but before it starts
                                               AppStatus.Started,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = null
                // deployment service returns this
                statusCode = 200
                result = getAppResponsePayload('client-new-app-dev')
                // apps that have had at least 1 successful deploy will show this
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                request.expectMultipart = true
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
    void perform_deployment_existing_stopped_app() {
        // arrange
        def deployed = false
        def appStartRequested = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               // before we deploy
                                               AppStatus.Undeployed,
                                               // after we deploy but before it starts
                                               AppStatus.Undeployed,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
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
                    result = getAppResponsePayload('client-new-app-dev')
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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        deployer.deploy(request)

        // assert
        assertThat appStartRequested,
                   is(equalTo(true))
    }

    @Test
    void perform_deployment_existing_failed_app() {
        // arrange
        def appStartRequested = false
        def deployed = false
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               // before we deploy
                                               AppStatus.Failed,
                                               // after we deploy but before it starts
                                               AppStatus.Failed,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
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
                    result = getAppResponsePayload('client-new-app-dev')
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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        deployer.deploy(request)

        // assert
        // our mock assertions should do the work here
    }

    def mockDeploymentAndXStatusChecks(HttpServerRequest request,
                                       AppStatus... appStatuses) {
        def uri = request.absoluteURI()
        if (mockAuthenticationOk(request)) {
            return true
        }
        if (mockEnvironments(request)) {
            return true
        }
        if (uri.endsWith('applications/client-new-app-dev') && request.method().name() == 'GET') {
            statusCheckCount++
            if (statusCheckCount <= appStatuses.length) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                              'application/json')
                    def result = null
                    def status = appStatuses[statusCheckCount - 1]
                    if (status == AppStatus.NotFound) {
                        statusCode = 404
                    } else {
                        // deployment service returns this
                        statusCode = 200
                        result = getAppResponsePayload('client-new-app-dev',
                                                       status)
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
        def deployAndStatusCount = 0
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                deployAndStatusCount++
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        deployer.deploy(request)

        // assert
        assertThat '1 deployment request, 2 status checks',
                   deployAndStatusCount,
                   is(equalTo(3))
    }

    @Test
    void perform_deployment_succeeds_after_2_tries() {
        // arrange
        def deployAndStatusCount = 0
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Deploying,
                                               AppStatus.Started)) {
                deployAndStatusCount++
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        deployer.deploy(request)

        // assert
        assertThat '1 deployment request, 3 status checks',
                   deployAndStatusCount,
                   is(equalTo(4))
    }

    @Test
    void perform_deployment_times_out() {
        // arrange
        def deployAndStatusCount = 0
        def statusesWeWillReturn = [AppStatus.NotFound] + (0..(maxTries + 1)).collect { AppStatus.Deploying }
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               statusesWeWillReturn.toArray(new AppStatus[0]) as AppStatus[])) {
                deployAndStatusCount++
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat '1 deployment request, 10 status checks',
                   deployAndStatusCount,
                   is(equalTo(11))
        assertThat exception.message,
                   is(equalTo('Deployment has not failed but app has not started after 10 tries!'))
    }

    @Test
    void perform_deployment_eventually_fails() {
        // arrange
        def deployAndStatusCount = 0
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockDeploymentAndXStatusChecks(request,
                                               AppStatus.NotFound,
                                               AppStatus.Deploying,
                                               AppStatus.Deploying,
                                               AppStatus.Deploying,
                                               AppStatus.Failed)) {
                deployAndStatusCount++
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                // deployment service returns this
                statusCode = 200
                def result = getAppResponsePayload('new-app')
                request.expectMultipart = true
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        def exception = shouldFail {
            deployer.deploy(request)
        }

        // assert
        assertThat '1 deployment request, 4 status checks',
                   deployAndStatusCount,
                   is(equalTo(5))
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
    void getAppStatus_undeployed() {
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
    void getAppStatus_undeploying() {
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
                        status: 'UNDEPLOYING'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Undeploying))
    }

    @Test
    void getAppStatus_deploying() {
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
                        status: 'DEPLOYING'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Deploying))
    }

    @Test
    void getAppStatus_failed() {
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
                   is(equalTo(AppStatus.NotFound))
    }

    @Test
    void getAppStatus_unknown() {
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
                        status: 'FOOBAR'
                ]))
            }
        }

        // act
        def exception = shouldFail {
            deployer.getAppStatus('DEV',
                                  'the-app')
        }

        // assert
        assertThat exception.message,
                   is(containsString('Unknown status value of FOOBAR detected from CloudHub!'))
    }

    @Test
    void getAppStatus_deleted() {
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
                        status: 'DELETED'
                ]))
            }
        }

        // act
        def status = deployer.getAppStatus('DEV',
                                           'the-app')

        // assert
        assertThat status,
                   is(equalTo(AppStatus.Deleted))
    }

    @Test
    void isMule4Request_no() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('4.2.2',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

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
