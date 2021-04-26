package com.avioconsulting.mule.integrationtest

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.api.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.MulesoftPolicy
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubDeployer
import com.avioconsulting.mule.deployment.internal.subdeployers.OnPremDeployer
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assume.assumeTrue

@Ignore
class IntegrationTest implements MavenInvoke {
    private static final String AVIO_SANDBOX_BIZ_GROUP_NAME = 'AVIO Sandbox'
    private static final String ANYPOINT_USERNAME = System.getProperty('anypoint.username')
    private static final String ANYPOINT_PASSWORD = System.getProperty('anypoint.password')
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
        buildApp()
        // cut down on the unit test noise here
        Configurator.setLevel('org.apache.http.wire',
                              Level.INFO)
        assert ANYPOINT_USERNAME: 'Did you forget -Danypoint.username?'
        assert ANYPOINT_PASSWORD: 'Did you forget -Danypoint.password?'
        assert ANYPOINT_CLIENT_ID: 'Did you forget -Danypoint.client.id?'
        assert ANYPOINT_CLIENT_SECRET: 'Did you forget -Danypoint.client.secret?'
    }

    def deleteCloudHubApp(CloudhubDeploymentRequest request) {
        def appName = request.normalizedAppName
        println "Attempting to clean out existing app ${appName}"
        cloudHubDeployer.deleteApp(request.environment,
                                   request.normalizedAppName,
                                   'integration test app cleanup')
        println 'Waiting for app deletion to finish'
        waitForAppDeletion(request.environment,
                           appName)
    }

    def deleteOnPremApp(OnPremDeploymentRequest request) {
        def existingAppId = onPremDeployer.locateApplication(request.environment,
                                                             request.appName)
        if (existingAppId) {
            onPremDeployer.deleteApp(request.environment,
                                     existingAppId)
        }
    }

    def waitForAppDeletion(String environment,
                           String appName) {
        def tries = 0
        def deleted = false
        def failed = false
        println 'Now checking to see if app has been deleted'
        while (!deleted && tries < 10) {
            tries++
            println "*** Try ${tries} ***"
            AppStatusPackage status = cloudHubDeployer.getAppStatus(environment,
                                                                    appName)
            println "Received status of ${status}"
            if (status.appStatus == AppStatus.NotFound) {
                println 'App removed successfully!'
                deleted = true
                break
            }
            def retryIntervalInMs = 10000
            println "Sleeping for ${retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(retryIntervalInMs)
        }
        if (!deleted && failed) {
            throw new Exception('Deletion failed on 1 or more nodes. Please see logs and messages as to why app did not start')
        }
        if (!deleted) {
            throw new Exception("Deletion has not completed after ${tries} tries!")
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
                                                                  CLOUDHUB_APP_PREFIX)
        def logger = new TestConsoleLogger()
        clientWrapper = new HttpClientWrapper('https://anypoint.mulesoft.com',
                                              ANYPOINT_USERNAME,
                                              ANYPOINT_PASSWORD,
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
        def apiSpec = new ApiSpecification('Mule Deploy Design Center Test Project')

        // act
        try {
            overallDeployer.deployApplication(cloudhubDeploymentRequest,
                                              apiSpec,
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
            hitEndpoint('john',
                        'doe',
                        "http://${cloudhubDeploymentRequest.normalizedAppName}.us-w2.cloudhub.io/")
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

    static def hitEndpoint(String username,
                           String password,
                           String url) {
        def credsProvider = new BasicCredentialsProvider().with {
            setCredentials(AuthScope.ANY,
                           new UsernamePasswordCredentials(username,
                                                           password))
            it
        }
        HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credsProvider)
                .build().withCloseable { client ->
            Throwable exception = null
            10.times {
                try {
                    def request = new HttpGet(url)
                    println "Hitting app @ ${request}"
                    client.execute(request).withCloseable { response ->
                        assertThat response.statusLine.statusCode,
                                   is(equalTo(200))
                        assertThat response.entity.content.text,
                                   is(equalTo('hello there'))
                        exception = null
                    }

                }
                catch (AssertionError e) {
                    println 'Test failed, waiting 500 ms and trying again'
                    exception = e
                    Thread.sleep(500)
                }
            }
            if (exception) {
                throw exception
            } else {
                println 'test passed'
            }
        }
    }

    @Test
    void on_prem() {
        // arrange
        assumeTrue('Need a configured AND RUNNING -Dmule4.onprem.server.name to run this test',
                   ON_PREM_SERVER_NAME != '' && ON_PREM_SERVER_NAME != null)
        println 'Cleaning up existing app'
        def deleteResult = deleteOnPremApp(onPremDeploymentRequest)
        if (deleteResult == 404) {
            println 'Existing app does not exist, no problem'
        }
        def apiSpec = new ApiSpecification('Mule Deploy Design Center Test Project')

        // act
        overallDeployer.deployApplication(onPremDeploymentRequest,
                                          apiSpec,
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
            hitEndpoint('john',
                        'doe',
                        'http://localhost:8081/')
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            deleteOnPremApp(onPremDeploymentRequest)
        }
    }
}
