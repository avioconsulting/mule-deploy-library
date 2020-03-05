package com.avioconsulting.jenkins.mule.workflow

import hudson.EnvVars
import hudson.FilePath
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.steps.StepContext

class CommonStepCode {
    static CommonConfig getCommonConfig(EnvVars environment) {
        def orgId = environment.get('anypoint_org_id')
        assert orgId: 'Configure your Anypoint Organization ID in the anypoint_org_id environment variable in your Jenkinsfile!'
        def username = environment.get('anypoint_credentials_usr')
        assert username: 'Configure your Anypoint username in the anypoint_credentials_usr environment variable in your Jenkinsfile!'
        def password = environment.get('anypoint_credentials_psw')
        assert password: 'Configure your Anypoint password in the anypoint_credentials_psw environment variable in your Jenkinsfile!'
        def anypointEnvironment = environment.get('anypoint_environment')
        assert anypointEnvironment: 'Configure your Anypoint environment in the anypoint_environment environment variable in your Jenkinsfile!'
        new CommonConfig(orgId,
                         username,
                         password,
                         anypointEnvironment)
    }

    static FilePath getZipFilePath(StepContext context,
                                   String zipFilePath) {
        def filePath = context.get(FilePath.class)
        def zipFile = filePath.child(zipFilePath)
        assert zipFile.exists(): "ZIP file you are trying to deploy, ${zipFile} does not exist! Are you using the correct filename from a Maven build? Did you stash/unstash properly if this deployment is happening in a different stage?"
        zipFile
    }

    static def setVersion(String version,
                          String environmentLabel,
                          EnvVars environment,
                          WorkflowRun run) {
        if (version == null) {
            version = environment.get('BUILD_NUMBER')
        }
        run.setDescription("${version} in ${environmentLabel} since ${java.time.LocalDate.now().toString()}")
    }
}
