package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubV2WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Test

@SuppressWarnings("GroovyAccessibility")
class CloudHubV2DeployerTest extends BaseTest {
    private CloudHubV2Deployer deployer
    private int statusCheckCount
    private int maxTries

    @Before
    void clean() {
        statusCheckCount = 0
        maxTries = 10
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new CloudHubV2Deployer(this.clientWrapper,
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
                    'client-sys-new-app-dev',
                    new AppStatusPackage(AppStatus.Applying,
                            null),
                    new AppStatusPackage(AppStatus.Applied,
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
                def result = getAppDeployResponsePayload('sys-new-app')
                end(JsonOutput.toJson(result))
            }
        }
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubV2DeploymentRequest('DEV',
                new CloudhubV2WorkerSpecRequest('target',
                        '4.2.2',
                        false,
                        false,
                        false,
                        UpdateStrategy.rolling,
                        false,
                        false,
                        VCoresSize.vCore1GB,
                        1),
                file,
                'theKey',
                'theClientId',
                'theSecret',
                'client',
                'sys-new-app',
                '1.2.3')
        request.setAutoDiscoveryId('the.auto.disc.prop',
                '1234')

        // act
        deployer.deploy(request)

    }

}