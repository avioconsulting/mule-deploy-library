package com.avioconsulting.jenkins.mule.impl

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class IntegrationTest {
    private static final String AVIO_ORG_ID = 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
    private static final String ANYPOINT_USERNAME = System.getProperty('anypoint.username')
    private static final String ANYPOINT_PASSWORD = System.getProperty('anypoint.password')
    private static File projectDirectory
    private static File builtFile

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

    @Test
    void on_prem() {
        // arrange
        def deployer = new OnPremDeployer(AVIO_ORG_ID,
                                          ANYPOINT_USERNAME,
                                          ANYPOINT_PASSWORD,
                                          System.out)
        def zipFile = builtFile.newInputStream()

        // act
        deployer.deploy('DEV',
                        'mule-deploy-lib-test-app-on-prem',
                        zipFile,
                        builtFile.name,
                        'mule-deploy-test-server')

        // assert
        Assert.fail("write it")
    }
}
