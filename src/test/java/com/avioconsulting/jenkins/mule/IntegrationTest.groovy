package com.avioconsulting.jenkins.mule

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.BuildWatcher
import org.jvnet.hudson.test.JenkinsRule

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.junit.Assume.assumeTrue

class IntegrationTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule()
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher()

    private String getProjectDir(String proj) {
        def pomFileUrl = getClass().getResource("/${proj}/pom.xml")
        def pomFile = new File(pomFileUrl.toURI())
        pomFile.parentFile.absolutePath
    }

    @BeforeClass
    static void checkForCreds() {
        assumeTrue("Need Anypoint credentials to run tests",
                   System.getenv('anypoint_credentials_psw') != null)
    }

    private WorkflowJob getProject(String jenkinsFileName) {
        jenkinsRule.createProject(WorkflowJob).with {
            def jenkinsFileText = new File('src/test/resources/jenkinsfiles',
                                           jenkinsFileName).text
            def mule3ProjectDir = getProjectDir 'mule_project'
            println "Resolved MULE_PROJECT_PATH path as ${mule3ProjectDir}, placing in Jenkinsfile"
            def mule4ProjectDir = getProjectDir 'mule4_project'
            println "Resolved MULE4_PROJECT_PATH path as ${mule4ProjectDir}, placing in Jenkinsfile"
            jenkinsFileText = jenkinsFileText.replace('MULE_PROJECT_PATH',
                                                      mule3ProjectDir)
                    .replace('MULE4_PROJECT_PATH',
                             mule4ProjectDir)
            definition = new CpsFlowDefinition(jenkinsFileText)
            it
        }
    }

    @Test
    void onPrem() {
        // arrange
        def project = getProject('onprem/Jenkinsfile')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('1 in DEV since'))
    }

    @Test
    void onPrem_updates_correctly() {
        // arrange
        def project = getProject('onprem/Jenkinsfile')
        println 'doing our first deployment'
        jenkinsRule.buildAndAssertSuccess(project)
        println 'rebuilding with maven with a new version'

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('2 in DEV since'))
    }

    @Test
    void onPrem_specify_version() {
        // arrange
        def project = getProject('onprem/Jenkinsfile_version')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('1.0.1 in DEV since'))
    }

    @Test
    void cloudhub() {
        // arrange
        def project = getProject('cloudhub/Jenkinsfile')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('1 in DEV since'))
    }

    @Test
    void onprem_mule4_update() {
        // arrange
        def project = getProject('onprem/Jenkinsfile_mule4')
        println 'doing our first deployment'
        jenkinsRule.buildAndAssertSuccess(project)
        println 'rebuilding with maven with a new version'

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('2 in DEV since'))
        fail 'write the test'
    }

    @Test
    void cloudhub_mule4_update() {
        // arrange
        def project = getProject('cloudhub/Jenkinsfile_mule4')
        println 'doing our first deployment'
        jenkinsRule.buildAndAssertSuccess(project)
        println 'rebuilding with maven with a new version'

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('2 in DEV since'))
    }

    @Test
    void cloudhub_mule4_gav() {
        // arrange
        def project = getProject('cloudhub/Jenkinsfile_mule4_gav')

        // act
        println 'doing our first deployment'
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('1 in DEV since'))
    }

    @Test
    void cloudhub_specify_version() {
        // arrange
        def project = getProject('cloudhub/Jenkinsfile_version')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        assertThat run.description,
                   is(containsString('1.0.1 in DEV since'))
    }

    @Test
    void muleFileName_mule3() {
        // arrange
        def project = getProject('muleFileName_mule3/Jenkinsfile')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        jenkinsRule.assertLogContains('the filename is target/theapp-1.0.1.zip',
                                      run)
        jenkinsRule.assertLogContains('the base filename is theapp-1.0.1.zip',
                                      run)
    }

    @Test
    void muleFileName_mule4() {
        // arrange
        def project = getProject('muleFileName_mule4/Jenkinsfile')

        // act
        def run = jenkinsRule.buildAndAssertSuccess(project)

        // assert
        jenkinsRule.assertLogContains('the filename is target/theapp-1.0.1-mule-application.jar',
                                      run)
    }
}
