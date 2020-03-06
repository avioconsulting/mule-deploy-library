package com.avioconsulting.jenkins.mule.impl

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
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

    def getFullAppName(String appName,
                       String prefix,
                       String environment) {
        cloudHubDeployer.normalizeAppName(appName,
                                          prefix,
                                          environment)
    }

    def deleteCloudHubApp(String appName,
                          String prefix,
                          String environment) {
        appName = getFullAppName(appName,
                                 prefix,
                                 environment)
        def environmentId = cloudHubDeployer.locateEnvironment(environment)
        println "Attempting to clean out existing app ${appName}"
        cloudHubDeployer.deleteApp(environmentId,
                                   appName)
        println 'Waiting for app deletion to finish'
        cloudHubDeployer.waitForAppDeletion(environmentId,
                                            appName)
    }

    def deleteOnPremApp(String appName,
                        String environment) {
        def environmentId = onPremDeployer.locateEnvironment(environment)
        def existingAppId = onPremDeployer.locateApplication(environmentId,
                                                             appName)
        if (existingAppId) {
            onPremDeployer.deleteApp(environmentId,
                                     existingAppId)
        }
    }

    @Before
    void cleanup() {
        cloudHubDeployer = new CloudHubDeployer(AVIO_ORG_ID,
                                                ANYPOINT_USERNAME,
                                                ANYPOINT_PASSWORD,
                                                System.out)
        cloudHubDeployer.authenticate()
        try {
            deleteCloudHubApp(CLOUDHUB_APP_NAME,
                              CLOUDHUB_APP_PREFIX,
                              AVIO_ENVIRONMENT_DEV)
        } catch (e) {
            if (e.message.contains('HTTP 404')) {
                println "Got ${e} while trying to delete app, no problem, it's not there"
            } else {
                throw e
            }
        }
        onPremDeployer = new OnPremDeployer(AVIO_ORG_ID,
                                            ANYPOINT_USERNAME,
                                            ANYPOINT_PASSWORD,
                                            System.out)
        onPremDeployer.authenticate()
    }

    @Test
    void cloudhub() {
        // arrange
        def zipFile = builtFile.newInputStream()

        // act
        try {
            cloudHubDeployer.deployFromFile(AVIO_ENVIRONMENT_DEV,
                                            CLOUDHUB_APP_NAME,
                                            CLOUDHUB_APP_PREFIX,
                                            zipFile,
                                            builtFile.name,
                                            'abcdefg',
                                            '4.2.2',
                                            false,
                                            WorkerTypes.Micro,
                                            1,
                                            'someid',
                                            'somesecret',
                                            AwsRegions.UsWest2)
            println 'test: app deployed OK, now trying to hit its HTTP listener'

            // assert
            def actualAppName = getFullAppName(CLOUDHUB_APP_NAME,
                                               CLOUDHUB_APP_PREFIX,
                                               AVIO_ENVIRONMENT_DEV)
            def url = "http://${actualAppName}.us-w2.cloudhub.io/".toURL()
            println "Hitting app @ ${url}"
            assertThat url.text,
                       is(equalTo('hello there'))
            println 'test passed'
        }
        finally {
            println 'test has finished one way or the other, now cleaning up our mess'
            // don't be dirty!
            deleteCloudHubApp(CLOUDHUB_APP_NAME,
                              CLOUDHUB_APP_PREFIX,
                              AVIO_ENVIRONMENT_DEV)
        }
    }

    @Test
    void on_prem() {
        // arrange
        assumeTrue('Need a configured AND RUNNING -Dmule4.onprem.server.name to run this test',
                   ON_PREM_SERVER_NAME != '' && ON_PREM_SERVER_NAME != null)
        println 'Cleaning up existing app'
        def deleteResult = deleteOnPremApp(ONPREM_APP_NAME,
                                           AVIO_ENVIRONMENT_DEV)
        if (deleteResult == 404) {
            println 'Existing app does not exist, no problem'
        }
        def zipFile = builtFile.newInputStream()

        // act
        onPremDeployer.deploy(AVIO_ENVIRONMENT_DEV,
                              ONPREM_APP_NAME,
                              zipFile,
                              builtFile.name,
                              ON_PREM_SERVER_NAME,
                              [env: AVIO_ENVIRONMENT_DEV])
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
            deleteOnPremApp(ONPREM_APP_NAME,
                            AVIO_ENVIRONMENT_DEV)
        }
    }
}
