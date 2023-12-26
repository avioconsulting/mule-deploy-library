package com.avioconsulting.mule.integrationtest

import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.credentials.ConnectedAppCredential
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubV2Deployer
import org.apache.groovy.json.internal.LazyMap
import org.apache.http.client.methods.HttpGet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

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

    private static Deployer overallDeployer
    private static HttpClientWrapper clientWrapper
    private static CloudHubV2Deployer cloudHubV2Deployer
    private static TestConsoleLogger logger = new TestConsoleLogger()
    private static EnvironmentLocator environmentLocator
    public static final String NORMALIZED_APP_NAME = 'cloudhubv2-int-test-dev'
    public static final String APP_STATUS = 'APPLIED'
    public static final String DEPLOYMENT_STATUS = 'RUNNING'
    public static final String ANYPOINT_URL = 'https://anypoint.mulesoft.com'
    public static final String APP_NAME = "cloudhubv2-int-test"
    public static final String APP_VERSION = "1.0.0"
    public static final String GROUP_ID = "f2ea2cb4-c600-4bb5-88e8-e952ff5591ee"
    public static final String CLOUDHUB_V2_REGION = "Cloudhub-US-West-2"
    public static final String MULE_VERSION = "4.4.0"
    public static final String ENV = "DEV"

    @BeforeAll
    static void setup() {

        checkCredentialsComeFromArgs()
        startClientWrapper()
        environmentLocator = new EnvironmentLocator(clientWrapper, logger)
        startDeployer()
        startOverallDeployer()
        deleteApp(GROUP_ID,environmentLocator.getEnvironmentId(ENV,GROUP_ID),NORMALIZED_APP_NAME)

    }

    @AfterEach
    void afterEachTestInvocation() {
        deleteApp(GROUP_ID,environmentLocator.getEnvironmentId(ENV,GROUP_ID),NORMALIZED_APP_NAME)
    }

    @Test
    void test_chv2_sharedspace_deployment_minimum() {
        //arrange
        CloudhubV2DeploymentRequest deploymentRequest = initMinimumDeploymentRequest()
        //act
        overallDeployer.deployApplication(deploymentRequest, null, null)
        //assert
        def appInfo = getApp(GROUP_ID,environmentLocator.getEnvironmentId(ENV, GROUP_ID),deploymentRequest.normalizedAppName)
        assertThat appInfo["application"]["status"] , is(equalTo(DEPLOYMENT_STATUS))
        assertThat appInfo["status"] , is(equalTo(APP_STATUS))
        assertThat appInfo["target"]["replicas"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getWorkerCount()))
        assertThat appInfo["application"]["vCores"] , is(BigDecimal.valueOf(deploymentRequest.getWorkerSpecRequest().getReplicaSize().vCoresSize))
        assertThat appInfo["target"]["deploymentSettings"]["updateStrategy"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getUpdateStrategy().name()))
    }

    @Test
    void test_chv2_sharedspace_deployment_full() {
        //arrange
        CloudhubV2DeploymentRequest deploymentRequest = initFullDeploymentRequest()
        //act
        overallDeployer.deployApplication(deploymentRequest, null, null)
        //assert
        def appInfo = getApp(GROUP_ID,environmentLocator.getEnvironmentId(ENV, GROUP_ID),deploymentRequest.normalizedAppName)
        assertThat appInfo["application"]["status"] , is(equalTo(DEPLOYMENT_STATUS))
        assertThat appInfo["status"] , is(equalTo(APP_STATUS))
        assertThat appInfo["target"]["replicas"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getWorkerCount()))
        assertThat appInfo["application"]["vCores"] , is(BigDecimal.valueOf(deploymentRequest.getWorkerSpecRequest().getReplicaSize().vCoresSize))
        assertThat appInfo["target"]["deploymentSettings"]["clustered"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getClustered()))
        assertThat appInfo["application"]["integrations"]["services"]["objectStoreV2"]["enabled"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getPersistentObjectStore()))
        assertThat appInfo["target"]["deploymentSettings"]["updateStrategy"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getUpdateStrategy().name()))
        assertThat appInfo["target"]["deploymentSettings"]["lastMileSecurity"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getLastMileSecurity()))
    }

    @Test
    void test_chv2_sharedspace_deployment_autodiscovery() {
        //arrange
        CloudhubV2DeploymentRequest deploymentRequest = initApiDiscoveryDeploymentRequest()
        //act
        overallDeployer.deployApplication(deploymentRequest, null, null)
        //assert
        def appInfo = getApp(GROUP_ID,environmentLocator.getEnvironmentId(ENV, GROUP_ID),deploymentRequest.normalizedAppName)
        assertThat appInfo["application"]["status"] , is(equalTo(DEPLOYMENT_STATUS))
        assertThat appInfo["status"] , is(equalTo(APP_STATUS))
        assertThat appInfo["target"]["replicas"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getWorkerCount()))
        assertThat appInfo["application"]["vCores"] , is(equalTo(BigDecimal.valueOf(deploymentRequest.getWorkerSpecRequest().getReplicaSize().vCoresSize)))
        assertThat appInfo["target"]["deploymentSettings"]["updateStrategy"] , is(equalTo(deploymentRequest.getWorkerSpecRequest().getUpdateStrategy().name()))
        compareProperties(appInfo, deploymentRequest.getCloudhubAppInfo())
    }

    void compareProperties(LazyMap appInfo, LinkedHashMap deploymentInfo){
        def retrievedFromApi = appInfo["application"]["configuration"]["mule.agent.application.properties.service"]["properties"]["the.auto.disc.prop"]
        def definedInRequest =  deploymentInfo["application"]["configuration"]["mule.agent.application.properties.service"]["properties"]["the.auto.disc.prop"]
        assertThat retrievedFromApi, is(equalTo(definedInRequest))
    }


    static deleteApp(String groupId, String envId, String appName) {
        try {
            println "Attempting to clean out existing app ${appName}"
            cloudHubV2Deployer.deleteApp(groupId, envId, appName, 'integration test app cleanup')
            println 'Waiting for app deletion to finish'
            waitForAppDeletion(cloudHubV2Deployer,
                    envId,
                    appName,
                    groupId)
        } catch (Exception e) {
            logger.println(e.message)
        }
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

     CloudhubV2DeploymentRequest initMinimumDeploymentRequest() {
        return new CloudhubV2DeploymentRequest(ENV,
                new WorkerSpecRequest(CLOUDHUB_V2_REGION,
                        MULE_VERSION,
                        false,
                        false,
                        false,
                        UpdateStrategy.rolling,
                        false,
                        false,
                        VCoresSize.vCore1GB,
                        3,
                        false,
                        false),
                null,
                ANYPOINT_CLIENT_ID,
                ANYPOINT_CLIENT_SECRET,
                new ApplicationName(APP_NAME, false, true, 'AVI', 'dev'),
                APP_VERSION,
                GROUP_ID,
                null,
                null
        )
    }
    CloudhubV2DeploymentRequest initApiDiscoveryDeploymentRequest() {
        CloudhubV2DeploymentRequest deploymentRequest = new CloudhubV2DeploymentRequest(ENV,
                new WorkerSpecRequest(CLOUDHUB_V2_REGION,
                        MULE_VERSION,
                        false,
                        false,
                        false,
                        UpdateStrategy.rolling,
                        false,
                        false,
                        VCoresSize.vCore1GB,
                        3,
                        false,
                        false),
                null,
                ANYPOINT_CLIENT_ID,
                ANYPOINT_CLIENT_SECRET,
                new ApplicationName(APP_NAME, false, true, 'AVI', 'dev'),
                APP_VERSION,
                GROUP_ID,
                null,
                null
        )
        deploymentRequest.setAutoDiscoveryId('the.auto.disc.prop', "1234")
        return deploymentRequest

    }


    CloudhubV2DeploymentRequest initFullDeploymentRequest() {
          return new CloudhubV2DeploymentRequest(ENV,
                  new WorkerSpecRequest(CLOUDHUB_V2_REGION,
                          MULE_VERSION,
                          true,
                          true,
                          true,
                          UpdateStrategy.recreate,
                          true,
                          true,
                          VCoresSize.vCore1GB,
                          2,
                          true,
                          false),
                  null ,
                  ANYPOINT_CLIENT_ID,
                  ANYPOINT_CLIENT_SECRET,
                  new ApplicationName(APP_NAME, false, true, 'AVI', 'dev'),
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

    static LazyMap getApp(String groupId, String envId, String normalizedAppName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments/")
        def response = clientWrapper.execute(request)
        def parsedResult = HttpClientWrapper.assertSuccessfulResponseAndReturnJson(response,"cannot get deployments information")
        def appId = parsedResult.items.findAll {it.name == normalizedAppName}.id[0]

        request = new HttpGet("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments/${appId}")
        response = clientWrapper.execute(request)
        parsedResult = HttpClientWrapper.assertSuccessfulResponseAndReturnJson(response,"cannot get deployment information")
        return parsedResult
    }

}
