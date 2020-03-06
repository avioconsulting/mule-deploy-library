package com.avioconsulting.jenkins.mule.impl

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class IntegrationTest {
    private static final String AVIO_ORG_ID = 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
    private static final String ANYPOINT_USERNAME = System.getProperty('anypoint.username')
    private static final String ANYPOINT_PASSWORD = System.getProperty('anypoint.password')
    private static final String CLOUDHUB_APP_PREFIX = 'avio'
    private static final String CLOUDHUB_APP_NAME = 'mule-deploy-lib-v4-test-app-ch'
    private static File projectDirectory
    private static File builtFile
    public static final String AVIO_ENVIRONMENT_DEV = 'DEV'
    private CloudHubDeployer cloudHubDeployer

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
        }
        finally {
            // don't be dirty!
            deleteCloudHubApp(CLOUDHUB_APP_NAME,
                              CLOUDHUB_APP_PREFIX,
                              AVIO_ENVIRONMENT_DEV)
        }
    }
}
