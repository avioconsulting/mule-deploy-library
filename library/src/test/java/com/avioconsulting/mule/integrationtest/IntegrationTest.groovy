package com.avioconsulting.mule.integrationtest

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.credentials.ConnectedAppCredential
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.MulesoftPolicy
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubDeployer
import com.avioconsulting.mule.deployment.internal.subdeployers.OnPremDeployer
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.junit.*

import static com.avioconsulting.mule.integrationtest.TestUtils.hitEndpointAndAssert
import static com.avioconsulting.mule.integrationtest.TestUtils.waitForAppDeletion
import static org.junit.Assume.assumeTrue

class IntegrationTest implements MavenInvoke {
    private static final String AVIO_SANDBOX_BIZ_GROUP_NAME = 'AVIO Sandbox'
    private static final String ANYPOINT_CONNECTED_APP_ID = System.getProperty('anypoint.connected-app.id')
    private static final String ANYPOINT_CONNECTED_APP_SECRET = System.getProperty('anypoint.connected-app.secret')
    private static final String ANYPOINT_CLIENT_ID = System.getProperty('anypoint.client.id')
    private static final String ANYPOINT_CLIENT_SECRET = System.getProperty('anypoint.client.secret')
    private static final String ON_PREM_SERVER_NAME = System.getProperty('mule4.onprem.server.name')
    private static final String CLOUDHUB_APP_PREFIX = 'avio'

    public static final String AVIO_ENVIRONMENT_DEV = 'DEV'
    private CloudHubDeployer cloudHubDeployer
    private OnPremDeployer onPremDeployer
    private Deployer overallDeployer
    private HttpClientWrapper clientWrapper
    private CloudhubDeploymentRequest cloudhubDeploymentRequest
    private OnPremDeploymentRequest onPremDeploymentRequest


    @BeforeClass
    static void setup() {
        Assume.assumeTrue('skip it',
                          System.getProperty('skip.integration.test') == null)
        buildApp("4.4.0")
        // cut down on the unit test noise here
        Configurator.setLevel('org.apache.http.wire',
                              Level.INFO)

        assert ANYPOINT_CONNECTED_APP_ID: 'Did you forget -Danypoint.connected-app.id?'
        assert ANYPOINT_CONNECTED_APP_SECRET: 'Did you forget -Danypoint.connected-app.secret?'
        assert ANYPOINT_CLIENT_ID: 'Did you forget -Danypoint.client.id?'
        assert ANYPOINT_CLIENT_SECRET: 'Did you forget -Danypoint.client.secret?'
    }

    def deleteCloudHubApp(CloudhubDeploymentRequest request) {
        def appName = request.appName.normalizedAppName
        println "Attempting to clean out existing app ${appName}"
        cloudHubDeployer.deleteApp(request.environment,
                                   request.appName.normalizedAppName,
                                   'integration test app cleanup')
        println 'Waiting for app deletion to finish'
        waitForAppDeletion(request.environment,
                           appName,null)
    }

    def deleteOnPremApp(OnPremDeploymentRequest request) {
        def existingAppId = onPremDeployer.locateApplication(request.environment,
                                                             request.appName)
        if (existingAppId) {
            onPremDeployer.deleteApp(request.environment,
                                     existingAppId)
        }
    }

    @Before
    void cleanup_and_instantiate() {
        onPremDeploymentRequest = new OnPremDeploymentRequest(AVIO_ENVIRONMENT_DEV,
                                                              ON_PREM_SERVER_NAME,
                                                              builtFile,
                                                              null,
                                                              null,
                                                              [env: AVIO_ENVIRONMENT_DEV])
        cloudhubDeploymentRequest = new CloudhubDeploymentRequest(AVIO_ENVIRONMENT_DEV,
                                                                  new CloudhubWorkerSpecRequest(),
                                                                  builtFile,
                                                                  'abcdefg',
                                                                  ANYPOINT_CLIENT_ID,
                                                                  ANYPOINT_CLIENT_SECRET,
                                                                  new ApplicationName(null, false, false, '', ''),
                                                                  null,
                                                                  [:])
        def logger = new TestConsoleLogger()
        clientWrapper = new HttpClientWrapper('https://anypoint.mulesoft.com',
                new ConnectedAppCredential(ANYPOINT_CONNECTED_APP_ID,ANYPOINT_CONNECTED_APP_SECRET),
                                              logger,
                                              AVIO_SANDBOX_BIZ_GROUP_NAME)
        def environmentLocator = new EnvironmentLocator(this.clientWrapper,
                                                        logger)
        cloudHubDeployer = new CloudHubDeployer(this.clientWrapper,
                                                environmentLocator,
                                                10000,
                                                // faster testing
                                                100,
                                                logger,
                                                DryRunMode.Run)
        try {
            deleteCloudHubApp(cloudhubDeploymentRequest)
        } catch (e) {
            if (e.message.contains('HTTP 404')) {
                println "Got ${e} while trying to delete app, no problem, it's not there"
            } else {
                throw e
            }
        }
        onPremDeployer = new OnPremDeployer(this.clientWrapper,
                                            environmentLocator,
                                            logger,
                                            DryRunMode.Run)
        overallDeployer = new Deployer(clientWrapper,
                                       DryRunMode.Run,
                                       logger,
                                       ['DEV'],
                                       environmentLocator)
    }

    @Test
    void cloudhub() {
        // arrange
        def apiSpec1 = new ApiSpecification('Mule Deploy Design Center Test Project',
                                           cloudhubDeploymentRequest.getRamlFilesFromApp('/api',
                                                                                         true),
                                            'mule-deploy-design-center-test-project-v2.raml',
                                            null,
                                            null,
                                            'auto.disc.1',
                                            null,
                                            '/api')

        def apiSpec2 = new ApiSpecification('Mule Deploy Design Center Test Project',
                                            cloudhubDeploymentRequest.getRamlFilesFromApp('/api_v1',
                                                                                          true),
                                            'mule-deploy-design-center-test-project.raml',
                                            null,
                                            null,
                                            'auto.disc.prop.1',
                                            'v1',
                                            '/api_v1')

        // act
        try {
            overallDeployer.deployApplication(cloudhubDeploymentRequest,
                                              new ApiSpecificationList([apiSpec1, apiSpec2]),
                                              [
                                                      new MulesoftPolicy('http-basic-authentication',
                                                                         '1.2.1',
                                                                         [
                                                                                 username: 'john',
                                                                                 password: 'doe'
                                                                         ])
                                              ])
            println 'test: app deployed OK, now trying to hit its HTTP listener'

            // assert
            hitEndpointAndAssert('john',
                        'doe',
                        "http://${cloudhubDeploymentRequest.appName.normalizedAppName}.us-w2.cloudhub.io/",
                        'hello there')
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            // don't be dirty!
            try {
                deleteCloudHubApp(cloudhubDeploymentRequest)
            } catch (e) {
                println "Unable to cleanup ${e}"
            }
        }
    }

    @Ignore
    void on_prem() {
        // arrange
        assumeTrue('Need a configured AND RUNNING -Dmule4.onprem.server.name to run this test',
                   ON_PREM_SERVER_NAME != '' && ON_PREM_SERVER_NAME != null)
        println 'Cleaning up existing app'
        def deleteResult = deleteOnPremApp(onPremDeploymentRequest)
        if (deleteResult == 404) {
            println 'Existing app does not exist, no problem'
        }
        def apiSpec = new ApiSpecification('Mule Deploy Design Center Test Project',
                                           onPremDeploymentRequest.getRamlFilesFromApp('/api', true))

        // act
        overallDeployer.deployApplication(onPremDeploymentRequest,
                                          new ApiSpecificationList([apiSpec]),
                                          [
                                                  new MulesoftPolicy('http-basic-authentication',
                                                                     '1.2.1',
                                                                     [
                                                                             username: 'john',
                                                                             password: 'doe'
                                                                     ])
                                          ])
        println 'test: app deployed OK, now trying to hit its HTTP listener'

        // assert
        try {
            hitEndpointAndAssert('john',
                        'doe',
                        'http://localhost:8081/',
                        'hello there')
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            deleteOnPremApp(onPremDeploymentRequest)
        }
    }
}
