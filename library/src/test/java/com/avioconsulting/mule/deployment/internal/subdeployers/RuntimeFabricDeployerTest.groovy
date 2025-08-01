package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpServerRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings("groovy:access")
class RuntimeFabricDeployerTest extends BaseTest implements MavenInvoke {
    private RuntimeFabricDeployer deployer
    protected int statusCheckCount
    protected int maxTries

    protected final String ENV = 'DEV'
    protected final String ENV_ID = 'def456'
    protected final String VERSION = '4.2.2'
    protected final String TARGET_NAME = 'us-west-2'
    protected final String TARGET_ID = '123-456-789'
    protected final String APP_NAME = 'new-app'
    protected final String APP_ID = '987-654-321'
    protected final String APP_VERSION = '1.2.3'
    protected final String GROUP_ID = 'the-org-id'
    protected final String CRYPTO_KEY = 'crypto-key'
    protected final String CLIENT_ID = 'client-id'
    protected final String CLIENT_SECRET = 'client-secret'

    @BeforeEach
    void clean() {
        statusCheckCount = 0
        maxTries = 10
        setupDeployer(DryRunMode.Run)
        buildApp()
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new RuntimeFabricDeployer(this.clientWrapper,
                environmentLocator,
                500,
                maxTries,
                new TestConsoleLogger(),
                dryRunMode)
    }

    RuntimeFabricDeploymentRequest getStandardDeploymentRequest() {
       def request = new RuntimeFabricDeploymentRequest(ENV, null,
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        false,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        2,
                        false,
                        false,
                        30,
                        800,
                        null,
                        null,
                        'LTS',
                        '17',
                        false),
                CRYPTO_KEY, null,
                CLIENT_ID,
                CLIENT_SECRET,
                new ApplicationName(APP_NAME, null, 'dev'),
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop', '1234')
        return request
    }

    @Test
    void new_app_success_deployment() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        String rawBody = null
        MultiMap sentFormAttributes = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request)) {
                return
            }
            if (mockGetAppInfo(request,true, true)) {
                return
            }
            if (mockCreateApp(request)) {
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                return
            }
        }

        def request = getStandardDeploymentRequest()
        request.target = 'us-west-2'
        request.appName = new ApplicationName(APP_NAME, null, 'dev')
        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                is(equalTo('/amc/application-manager/api/v2/organizations/the-org-id/environments/def456/deployments'))
        assertThat method,
                is(equalTo('POST'))
        assertThat authToken,
                is(equalTo('Bearer the token'))
        assertThat envId,
                is(equalTo('def456'))
        assertThat orgId,
                is(equalTo('the-org-id'))
        def map = new JsonSlurper().parseText(rawBody)
        assertThat map.name,
                is(equalTo('new-app-dev'))
        assertThat map.application.ref.groupId,
                is(equalTo(GROUP_ID))
        assertThat map.application.ref.version,
                is(equalTo(APP_VERSION))
        assertThat map.target.targetId,
                is(equalTo(TARGET_ID))
    }

    @Test
    void new_app_deployment_max_attempt_reached() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request)) {
                return
            }
            if (mockGetAppInfo(request,true, false)) {
                return
            }
            if (mockCreateApp(request)) {
                return
            }
        }

        def request = getStandardDeploymentRequest()


        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString('Deployment has not failed but app has not started after 10 tries!'))

    }

    @Test
    void update_app_success_deployment() {
        // arrange
        String url = null
        String method = null
        String authToken = null
        String envId = null
        String orgId = null
        String rawBody = null
        MultiMap sentFormAttributes = null
        withHttpServer { HttpServerRequest request ->
            def uri = request.uri()
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request)) {
                return
            }
            if (mockGetAppInfo(request, false, true)) {
                return
            }
            if (mockUpdateApp(request)) {
                url = uri
                method = request.method()
                (authToken, orgId, envId) = capturedStandardHeaders(request)
                sentFormAttributes = request.formAttributes()
                request.bodyHandler { buffer ->
                    rawBody = buffer.toString()
                }
                return
            }
        }

//        def request = new RuntimeFabricDeploymentRequest(ENV,
//                new WorkerSpecRequest(TARGET_NAME,
//                        VERSION,
//                        true,
//                        true,
//                        true,
//                        UpdateStrategy.recreate,
//                        true,
//                        true,
//                        VCoresSize.vCore15GB,
//                        13,
//                        true,
//                        false),
//                'theKey',
//                'theClientId',
//                'theSecret',
//                new ApplicationName(APP_NAME, null, 'dev'),
//                APP_VERSION,
//                GROUP_ID)

        def request = getStandardDeploymentRequest()

        // act
        deployer.deploy(request)

        // assert
        assertThat url,
                is(equalTo('/amc/application-manager/api/v2/organizations/the-org-id/environments/def456/deployments/987-654-321'))
        assertThat method,
                is(equalTo('PATCH'))
        assertThat authToken,
                is(equalTo('Bearer the token'))
        assertThat envId,
                is(equalTo('def456'))
        assertThat orgId,
                is(equalTo('the-org-id'))
        def map = new JsonSlurper().parseText(rawBody)
        assertThat map.name,
                is(equalTo('new-app-dev'))
        assertThat map.application.ref.groupId,
                is(equalTo(GROUP_ID))
        assertThat map.application.ref.version,
                is(equalTo(APP_VERSION))
        assertThat map.target.targetId,
                is(equalTo(TARGET_ID))

    }

    @Test
    void update_app_deployment_max_attempt_reached() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request)) {
                return
            }
            if (mockGetAppInfo(request, false, false)) {
                return
            }
            if (mockUpdateApp(request)) {
                return
            }
        }

//        def request = new RuntimeFabricDeploymentRequest(ENV,
//                new WorkerSpecRequest(TARGET_NAME,
//                        VERSION,
//                        true,
//                        true,
//                        true,
//                        UpdateStrategy.recreate,
//                        true,
//                        true,
//                        VCoresSize.vCore15GB,
//                        13,
//                        true,
//                        false),
//                'theKey',
//                'theClientId',
//                'theSecret',
//                new ApplicationName(APP_NAME, null, 'dev'),
//                APP_VERSION,
//                GROUP_ID)

        def request = getStandardDeploymentRequest()

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString('Deployment has not failed but app has not started after 10 tries!'))

    }

    @Test
    void invalid_environment_failure() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
        }

        def request = getStandardDeploymentRequest()
        request.environment = 'INVALID_ENV'

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find environment 'INVALID_ENV'. Valid environments are [Design, DEV]"))

    }

    @Test
    void invalid_targetType_failure() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request, false, true)) {
                return
            }
        }

        def request = getStandardDeploymentRequest()
        request.target = 'INVALID_TARGET_NAME'

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find a valid runtime fabric target 'INVALID_TARGET_NAME'. Valid targets are []"))

    }

    @Test
    void invalid_targetId_failure() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request, false, false)) {
                return
            }
        }

        def request = getStandardDeploymentRequest()
        request.target = 'INVALID_TARGET_NAME'

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find a valid runtime fabric target 'INVALID_TARGET_NAME'. Valid targets are [Not_Found_Target_Name]"))

    }

    def mockGetTargetId(HttpServerRequest request,
                        boolean isSuccess = true,
                        boolean isErrorGetTargetType = false,
                        String groupId = this.GROUP_ID) {
        def uri = request.absoluteURI()
        if (uri.endsWith("/runtimefabric/api/organizations/${groupId}/targets") && request.method().name() == 'GET') {
            println "mock get targetId /runtimefabric/api/organizations/${groupId}/targets"
            if(isSuccess) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [id: TARGET_ID, name: TARGET_NAME, type: deployer.RUNTIME_FABRIC_TARGET_TYPE],
                        [id: TARGET_ID, name: TARGET_NAME, type: deployer.SHARED_SPACE_TARGET_TYPE]
                    ]))
                }
            } else if (isErrorGetTargetType) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                            [id: TARGET_ID, name: TARGET_NAME, type: 'Invalid_Type']
                    ]))
                }
            } else {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [id: 'Not_Found_Target_Id', name: 'Not_Found_Target_Name', type: deployer.RUNTIME_FABRIC_TARGET_TYPE],
                        [id: 'Not_Found_Target_Id', name: 'Not_Found_Target_Name', type: deployer.SHARED_SPACE_TARGET_TYPE]
                    ]))
                }
            }
            return true
        }
        return false
    }

    def mockGetAppInfo(HttpServerRequest request,
                       boolean newApp = true,
                       boolean isSuccess = true,
                       String groupId = this.GROUP_ID,
                       String envId = this.ENV_ID) {
        def uri = request.absoluteURI()
        if (uri.endsWith("/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments") && request.method().name() == 'GET') {
            println "mock get appInfo /amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments"
            if (newApp && isSuccess && statusCheckCount <= 1) {
                statusCheckCount++
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        items: [
                            [
                                id: APP_ID,
                                name: 'app-not-found',
                                status: 'APPLYING',
                                application: [
                                    status: 'RUNNING'
                                ],
                                target: [
                                    provider: 'MC',
                                    targetId: 'us-west-2'
                                ]
                            ]
                        ]
                    ]))
                }
            } else if (newApp && isSuccess && statusCheckCount > 1) {
                statusCheckCount++
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        items: [
                            [
                                id: APP_ID,
                                name: "${APP_NAME}-${ENV}".toLowerCase(),
                                status: 'APPLIED',
                                application: [
                                    status: 'RUNNING'
                                ],
                                target: [
                                    provider: 'MC',
                                    targetId: 'us-west-2'
                                ]
                            ]
                        ]
                    ]))
                }
            } else if (!newApp && isSuccess && (statusCheckCount == 0 || statusCheckCount > 1)) {
                statusCheckCount++
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        items: [
                            [
                                id: APP_ID,
                                name: "${APP_NAME}-${ENV}".toLowerCase(),
                                status: 'APPLIED',
                                application: [
                                    status: 'RUNNING'
                                ],
                                target: [
                                    provider: 'MC',
                                    targetId: 'us-east-2'
                                ]
                            ]
                        ]
                    ]))
                }
            } else if ((!newApp && isSuccess && statusCheckCount == 1) || (!newApp && !isSuccess)) {
                statusCheckCount++
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        items: [
                            [
                                id: APP_ID,
                                name: "${APP_NAME}-${ENV}".toLowerCase(),
                                status: 'APPLYING',
                                application: [
                                    status: 'RUNNING'
                                ],
                                target: [
                                    provider: 'MC',
                                    targetId: 'us-east-2'
                                ]
                            ]
                        ]
                    ]))
                }
            } else {
                request.response().with {
                    statusCheckCount++
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        items: [
                            [
                                id: APP_ID,
                                name: 'app-not-found',
                                status: 'STOPPED',
                                application: [
                                        status: 'NOT_RUNNING'
                                ],
                                target: [
                                        provider: 'MC',
                                        targetId: 'us-east-2'
                                ]
                            ]
                        ]
                    ]))
                }
            }
            return true
        }
        return false
    }

    def mockCreateApp(HttpServerRequest request,
                        String groupId = this.GROUP_ID,
                        String envId = this.ENV_ID) {
        def uri = request.absoluteURI()
        if (uri.endsWith("/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments") && request.method().name() == 'POST') {
            println "mock creat app invocation /amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments"

            request.response().with {
                statusCode = 202
                putHeader('Content-Type',
                        'application/json')
                end(JsonOutput.toJson([
                    [
                        id: APP_ID,
                        name: "${APP_NAME}-${ENV}".toLowerCase(),
                        creationDate: 1671455155002,
                        lastModifiedDate: 1671455155002
                    ]
                ]))
            }
            return true
        }
        return false
    }

    def mockUpdateApp(HttpServerRequest request,
                      boolean isSuccess = true,
                      String groupId = this.GROUP_ID,
                      String envId = this.ENV_ID,
                      String appId = this.APP_ID) {
        def uri = request.absoluteURI()
        if (uri.endsWith("/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments/${appId}") && request.method().name() == 'PATCH') {
            println "mock updte app invocation /runtimefabric/api/organizations/${groupId}/targets"
            if(isSuccess) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [
                            id: APP_ID,
                            name: "${APP_NAME}-${ENV}".toLowerCase(),
                            creationDate: 1671455155002,
                            lastModifiedDate: 1671455155002
                        ]
                    ]))
                }
            } else {
                request.response().with {
                    statusCode = 500
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [
                            timestamp: 1671456239010,
                            status: 500,
                            error: 'Internal Server Error'
                        ]
                    ]))
                }
            }
            return true
        }
        return false
    }
}