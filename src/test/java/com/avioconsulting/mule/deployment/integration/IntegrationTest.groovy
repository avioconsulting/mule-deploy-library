package com.avioconsulting.mule.deployment.integration

import com.avioconsulting.mule.deployment.MuleUtil
import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.CloudhubFileDeploymentRequest
import com.avioconsulting.mule.deployment.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.subdeployers.CloudHubDeployer
import com.avioconsulting.mule.deployment.subdeployers.OnPremDeployer
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assume.assumeTrue

class IntegrationTest {
    private static final String AVIO_ORG_ID = 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
    private static final String ANYPOINT_USERNAME = System.getProperty('anypoint.username')
    private static final String ANYPOINT_PASSWORD = System.getProperty('anypoint.password')
    private static final String ON_PREM_SERVER_NAME = System.getProperty('mule4.onprem.server.name')
    private static final String CLOUDHUB_APP_PREFIX = 'avio'
    private static final String CLOUDHUB_APP_NAME = 'mule-deploy-lib-v4-test-app-ch'
    private static final String ONPREM_APP_NAME = 'mule-deploy-lib-v4-test-app-onprem'
    private static File projectDirectory
    private static File builtFile
    public static final String AVIO_ENVIRONMENT_DEV = 'DEV'
    private CloudHubDeployer cloudHubDeployer
    private OnPremDeployer onPremDeployer
    private HttpClientWrapper clientWrapper
    private CloudhubFileDeploymentRequest cloudhubDeploymentRequest
    private OnPremDeploymentRequest onPremDeploymentRequest

    private static File getProjectDir(String proj) {
        def pomFileUrl = IntegrationTest.getResource("/${proj}/pom.xml")
        new File(pomFileUrl.toURI())
    }

    @BeforeClass
    static void setup() {
        assert ANYPOINT_USERNAME: 'Did you forget -Danypoint.username?'
        assert ANYPOINT_PASSWORD: 'Did you forget -Danypoint.password?'
        def pomFile = getProjectDir('mule4_project')
        projectDirectory = pomFile.parentFile
        def targetDir = new File(projectDirectory,
                                 'target')
        builtFile = new File(targetDir,
                             MuleUtil.getFileName('mule4testapp',
                                                  '1.0.0',
                                                  '4.2.2'))
        def mavenInvokeRequest = new DefaultInvocationRequest().with {
            setGoals(['clean', 'package'])
            setPomFile(pomFile)
            setShowErrors(true)
            it
        }
        def mavenInvoker = new DefaultInvoker()
        def mavenHome = System.getProperty('maven.home')
        assert mavenHome: 'Did you forget maven.home in surefire config?'
        mavenInvoker.setMavenHome(new File(mavenHome))
        def result = mavenInvoker.execute(mavenInvokeRequest)
        assert result.exitCode == 0
    }

    def deleteCloudHubApp(CloudhubFileDeploymentRequest request) {
        def appName = request.normalizedAppName
        println "Attempting to clean out existing app ${appName}"
        cloudHubDeployer.deleteApp(request.environment,
                                   request.normalizedAppName)
        println 'Waiting for app deletion to finish'
        cloudHubDeployer.waitForAppDeletion(request.environment,
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

    @Before
    void cleanup() {
        onPremDeploymentRequest = new OnPremDeploymentRequest(AVIO_ENVIRONMENT_DEV,
                                                              ONPREM_APP_NAME,
                                                              ON_PREM_SERVER_NAME,
                                                              builtFile.name,
                                                              builtFile.newInputStream(),
                                                              [env: AVIO_ENVIRONMENT_DEV])
        cloudhubDeploymentRequest = new CloudhubFileDeploymentRequest(builtFile.newInputStream(),
                                                                      AVIO_ENVIRONMENT_DEV,
                                                                      CLOUDHUB_APP_NAME,
                                                                      new CloudhubWorkerSpecRequest('4.2.2',
                                                                                                    false,
                                                                                                    1),
                                                                      builtFile.name,
                                                                      'abcdefg',
                                                                      'someid',
                                                                      'somesecret',
                                                                      CLOUDHUB_APP_PREFIX)
        clientWrapper = new HttpClientWrapper(ANYPOINT_USERNAME,
                                              ANYPOINT_PASSWORD,
                                              AVIO_ORG_ID,
                                              System.out)
        def environmentLocator = new EnvironmentLocator(this.clientWrapper,
                                                        System.out)
        cloudHubDeployer = new CloudHubDeployer(this.clientWrapper,
                                                environmentLocator,
                                                10000,
                                                // faster testing
                                                100,
                                                System.out)
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
                                            System.out)
    }

    @Test
    void cloudhub() {
        // arrange

        // act
        try {
            cloudHubDeployer.deploy(cloudhubDeploymentRequest)
            println 'test: app deployed OK, now trying to hit its HTTP listener'

            // assert
            def url = "http://${cloudhubDeploymentRequest.normalizedAppName}.us-w2.cloudhub.io/".toURL()
            println "Hitting app @ ${url}"
            assertThat url.text,
                       is(equalTo('hello there'))
            println 'test passed'
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            // don't be dirty!
            deleteCloudHubApp(cloudhubDeploymentRequest)
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

        // act
        onPremDeployer.deploy(onPremDeploymentRequest)
        println 'test: app deployed OK, now trying to hit its HTTP listener'

        // assert
        try {
            def url = "http://localhost:8081/".toURL()
            println "Hitting app @ ${url}"
            assertThat url.text,
                       is(equalTo('hello there'))
            println 'test passed'
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            deleteOnPremApp(onPremDeploymentRequest)
        }
    }
}
