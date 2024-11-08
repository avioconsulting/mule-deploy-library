package com.avioconsulting.mule

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker

// TODO:  This currently only builds on Maven up to 3.8.8, we need to consider if the sample needs to move to mule maven plugin 4.x
trait MavenInvoke {
    static File projectDirectory
    static File builtFile

    static File getProjectDir(String proj) {
        def pomFileUrl = MavenInvoke.getResource("/${proj}/pom.xml")
        new File(pomFileUrl.toURI())
    }

    static String getFileName(String appName,
                              String appVersion,
                              String muleVersion) {
        return muleVersion.startsWith("3") ?
                String.format("%s-%s.zip",
                              appName,
                              appVersion) :
                String.format("%s-%s-mule-application.jar",
                              appName,
                              appVersion)
    }

    static def buildApp(String muleVersion = '4.3.0') {
        def pomFile = getProjectDir('mule4_project')
        def appVersion = '2.2.9'
        pomFile.text = pomFile.text.replaceAll(/<app.runtime>\S+<\/app.runtime>/,
                                               "<app.runtime>${muleVersion}</app.runtime>")
                .replaceAll(/<version>replaceme<\/version>/,
                            "<version>${appVersion}</version>")
        projectDirectory = pomFile.parentFile
        def targetDir = new File(projectDirectory,
                                 'target')
        builtFile = new File(targetDir,
                             getFileName('mule-deploy-lib-v4-test-app',
                                         appVersion,
                                         muleVersion))
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
}
