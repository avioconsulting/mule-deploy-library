package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class CloudhubV2DeploymentRequestTest {


    WorkerSpecRequest getWorkerSpecBase() {
        return new WorkerSpecRequest('us-west-2',
                '4.2.2', null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * This test case validates that after creating a CloudhubV2DeploymentRequest object this
     * is valid according to it's contructor.
     */
    @Test
    void test_deploymentRequest_creation_ok() {

        double vCoreSizeExpected = 0.1

        def request = new CloudhubV2DeploymentRequest('DEV', null,
                getWorkerSpecBase(),
                'theKey', null,
                'theClientId',
                'theSecret',
                new ApplicationName('new-app', 'prefix', null),
                '1.2.3',
                'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        def appInfo = request.getCloudhubAppInfo()

        request.with {
            assertThat appName.baseAppName,
                    is(equalTo('new-app'))
            assertThat normalizedAppName,
                    is(equalTo('prefix-new-app'))
            assertThat appVersion,
                    is(equalTo('1.2.3'))
            assertThat groupId,
                    is(equalTo('f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'))
            assertThat target,
                    is(equalTo('us-west-2'))
        }
        assertThat  appInfo.target.deploymentSettings.enforceDeployingReplicasAcrossNodes, is(equalTo(true))
        assertThat  appInfo.target.deploymentSettings.persistentObjectStore, is(equalTo(false))
        assertThat  appInfo.application.vCores, is(equalTo(vCoreSizeExpected))
    }
}