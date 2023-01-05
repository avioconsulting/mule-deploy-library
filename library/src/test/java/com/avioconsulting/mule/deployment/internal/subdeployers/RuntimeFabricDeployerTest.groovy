package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.RuntimeFabricDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings("GroovyAccessibility")
class RuntimeFabricDeployerTest extends BaseTest implements MavenInvoke {
    private RuntimeFabricDeployer deployer
    private int statusCheckCount
    private int maxTries

    private final String ENV = 'DEV'
    private final String ENV_ID = 'def456'
    private final String VERSION = '4.2.2'
    private final String TARGET_NAME = 'us-west-2'
    private final String TARGET_ID = '123-456-789'
    private final String APP_NAME = 'new-app'
    private final String APP_ID = '987-654-321'
    private final String APP_VERSION = '1.2.3'
    private final String GROUP_ID = 'the-org-id'

    @Before
    void clean() {
        statusCheckCount = 0
        maxTries = 10
        setupDeployer(DryRunMode.Run)
        buildApp()
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new CloudHubV2Deployer(this.clientWrapper,
                environmentLocator,
                500,
                maxTries,
                new TestConsoleLogger(),
                dryRunMode)
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

        def request = new RuntimeFabricDeploymentRequest(ENV,
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                'new-app',
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

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
                is(equalTo('new-app'))
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

        def request = new RuntimeFabricDeploymentRequest(ENV,
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                'new-app',
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

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

        def request = new RuntimeFabricDeploymentRequest(ENV,
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                APP_NAME,
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

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
                is(equalTo(APP_NAME))
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

        def request = new RuntimeFabricDeploymentRequest(ENV,
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                APP_NAME,
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

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

        def request = new RuntimeFabricDeploymentRequest('INVALID_ENV',
                new WorkerSpecRequest(TARGET_NAME,
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                APP_NAME,
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find environment 'INVALID_ENV'. Valid environments are [Design, DEV]"))

    }

    @Test
    void invalid_target_failure() {
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            if (mockGetTargetId(request, false)) {
                return
            }
        }

        def request = new RuntimeFabricDeploymentRequest(ENV,
                new WorkerSpecRequest('INVALID_TARGET_NAME',
                        VERSION,
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                APP_NAME,
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find target 'INVALID_TARGET_NAME'. Valid targets are [Not_Found_Target_Name]"))

    }

    def mockGetTargetId(HttpServerRequest request,
                        boolean isSuccess = true,
                        String groupId = this.GROUP_ID) {
        def uri = request.absoluteURI()
        if (uri.endsWith("/runtimefabric/api/organizations/${groupId}/targets") && request.method().name() == 'GET') {
            println "mock status invocation /runtimefabric/api/organizations/${groupId}/targets"
            if(isSuccess) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [id: TARGET_ID, name: TARGET_NAME]
                    ]))
                }
            } else {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [id: 'Not_Found_Target_Id', name: 'Not_Found_Target_Name']
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
            println "mock status invocation /amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments"
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
                                    targetId: 'us-east-2'
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
                                name: APP_NAME,
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
                                name: APP_NAME,
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
                                name: APP_NAME,
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
            println "mock status invocation /amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments"

            request.response().with {
                statusCode = 202
                putHeader('Content-Type',
                        'application/json')
                end(JsonOutput.toJson([
                    [
                        id: APP_ID,
                        name: APP_NAME,
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
            println "mock status invocation /runtimefabric/api/organizations/${groupId}/targets"
            if(isSuccess) {
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                            'application/json')
                    end(JsonOutput.toJson([
                        [
                            id: APP_ID,
                            name: APP_NAME,
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