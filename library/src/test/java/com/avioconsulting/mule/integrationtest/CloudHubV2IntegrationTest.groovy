package com.avioconsulting.mule.integrationtest

import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.credentials.ConnectedAppCredential
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubV2Deployer
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test

import static com.avioconsulting.mule.integrationtest.TestUtils.waitForAppDeletion
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class CloudHubV2IntegrationTest {

    private static final String ANYPOINT_CONNECTED_APP_ID = System.getProperty('anypoint.connected-app.id')
    private static final String ANYPOINT_CONNECTED_APP_SECRET = System.getProperty('anypoint.connected-app.secret')
    private static final String ANYPOINT_CLIENT_ID = System.getProperty('anypoint.client.id')
    private static final String ANYPOINT_CLIENT_SECRET = System.getProperty('anypoint.client.secret')
    private static final String AVIO_SANDBOX_BIZ_GROUP_NAME = 'AVIO Sandbox'

    private static CloudhubV2DeploymentRequest cloudhubV2DeploymentRequest
    private static Deployer overallDeployer
    private static HttpClientWrapper clientWrapper
    private static CloudHubV2Deployer cloudHubV2Deployer
    private static TestConsoleLogger logger
    private static EnvironmentLocator environmentLocator
    public static final String NORMALIZED_APP_NAME = 'cloudhubv2-int-test-dev'
    public static final String APP_STATUS = 'RUNNING'
    public static final String DEPLOYMENT_STATUS = 'APPLIED'
    public static final String ANYPOINT_URL = 'https://anypoint.mulesoft.com'
    public static final String APP_NAME = "cloudhubv2-int-test"
    public static final String APP_VERSION = "1.0.0"
    public static final String GROUP_ID = "f2ea2cb4-c600-4bb5-88e8-e952ff5591ee"
    public static final String CLOUDHUB_V2_REGION = "Cloudhub-US-West-2"
    public static final String MULE_VERSION = "4.4.0"
    public static final String ENV = "DEV"

    @BeforeClass
    static void setup() {
        checkCredentialsComeFromArgs()
        logger = new TestConsoleLogger()
        startClientWrapper()
        environmentLocator = new EnvironmentLocator(clientWrapper, logger)
        startDeployer()
        startOverallDeployer()
        startDeploymentRequest()
    }

    @After
    void afterEachTestInvocation() {
        try {
            deleteApp()
        } catch (Exception exc) {
            print(exc.message)
        }
    }

    @Test
    void test_cloudhubv2_sharedspace_deployment() {
        overallDeployer.deployApplication(cloudhubV2DeploymentRequest, null, null)
        def apiResult = getAppSpecsInfoFromDeployment()
        assertThat apiResult.getName(), is(equalTo(NORMALIZED_APP_NAME))
        assertThat apiResult.getAppStatus(), is(equalTo(APP_STATUS))
        assertThat apiResult.getDeploymentStatus(), is(equalTo(DEPLOYMENT_STATUS))
    }

    static DeploymentItem getAppSpecsInfoFromDeployment() {
        def groupId = cloudhubV2DeploymentRequest.groupId
        def envId = environmentLocator.getEnvironmentId(cloudhubV2DeploymentRequest.environment, groupId)
        DeploymentItem appInfo = cloudHubV2Deployer.getAppInfo(envId, groupId, cloudhubV2DeploymentRequest.normalizedAppName)
        return appInfo
    }

    def static deleteApp() {
        def appName = cloudhubV2DeploymentRequest.normalizedAppName
        def groupId= cloudhubV2DeploymentRequest.getGroupId()
        def envId = environmentLocator.getEnvironmentId(cloudhubV2DeploymentRequest.environment, groupId)
        println "Attempting to clean out existing app ${appName}"
        cloudHubV2Deployer.deleteApp(cloudhubV2DeploymentRequest,
                'integration test app cleanup')
        println 'Waiting for app deletion to finish'
        waitForAppDeletion(cloudHubV2Deployer,
                envId,
                appName,
                groupId)
    }

    static void startOverallDeployer() {
        overallDeployer = new Deployer(clientWrapper,
                DryRunMode.Run,
                logger,
                [ENV    ],
                environmentLocator)
    }

    static void startDeployer() {
        cloudHubV2Deployer = new CloudHubV2Deployer(this.clientWrapper,
                environmentLocator,
                10000,
                100,
                logger,
                DryRunMode.Run)
    }

    static void startClientWrapper() {
        clientWrapper = new HttpClientWrapper(ANYPOINT_URL,
                new ConnectedAppCredential(ANYPOINT_CONNECTED_APP_ID, ANYPOINT_CONNECTED_APP_SECRET),
                logger,
                AVIO_SANDBOX_BIZ_GROUP_NAME)
    }

    static void startDeploymentRequest() {
        cloudhubV2DeploymentRequest = new CloudhubV2DeploymentRequest(ENV,
                new WorkerSpecRequest(CLOUDHUB_V2_REGION, MULE_VERSION),
                null, ANYPOINT_CLIENT_ID, ANYPOINT_CLIENT_SECRET,
                null,
                APP_NAME,
                APP_VERSION,
                GROUP_ID,
                null,
                null
        )
    }

    static void checkCredentialsComeFromArgs() {
        assert ANYPOINT_CONNECTED_APP_ID: 'Did you forget -Danypoint.connected-app.id?'
        assert ANYPOINT_CONNECTED_APP_SECRET: 'Did you forget -Danypoint.connected-app.secret?'
        assert ANYPOINT_CLIENT_ID: 'Did you forget -Danypoint.client.id?'
        assert ANYPOINT_CLIENT_SECRET: 'Did you forget -Danypoint.client.secret?'
    }

}
