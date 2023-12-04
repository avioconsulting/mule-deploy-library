package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
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
class CloudHubV2DeployerTest extends RuntimeFabricDeployerTest implements MavenInvoke {
    private CloudHubV2Deployer deployer

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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName("new-app",false,true,null,ENV),
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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName("new-app",false,false,null,null),
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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName(APP_NAME,false,true,null,ENV),
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
                is(equalTo("new-app-dev"))
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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName(APP_NAME,false,true,null,ENV),
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

        def request = new CloudhubV2DeploymentRequest('INVALID_ENV',
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName(APP_NAME,false,true,null,ENV),
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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName(APP_NAME,false,true,null,ENV),
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find valid cloudhub v2 target 'INVALID_TARGET_NAME'. Valid targets are []"))

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

        def request = new CloudhubV2DeploymentRequest(ENV,
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
                'theKey',
                'theClientId',
                'theSecret',
                new ApplicationName(APP_NAME,false,true,null,ENV),
                APP_VERSION,
                GROUP_ID)

        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

        def exception = shouldFail {
            deployer.deploy(request)
        }

        assertThat exception.message,
                is(containsString("Unable to find valid cloudhub v2 target 'INVALID_TARGET_NAME'. Valid targets are [Not_Found_Target_Name]"))
    }
}