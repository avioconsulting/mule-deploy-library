package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import org.hamcrest.MatcherAssert
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class CloudhubV2DeploymentRequestTest implements MavenInvoke {
    @BeforeClass
    static void setup() {
        buildApp()
    }

    @Test
    void explicit() {

        def request = new CloudhubV2DeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2',
                                                                           '4.2.2'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'prefix',
                                                    'new-app',
                                                    '1.2.3',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        request.with {
            assertThat appName,
                       is(equalTo('new-app'))
            assertThat normalizedAppName,
                       is(equalTo('prefix-new-app-dev'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            assertThat groupId,
                    is(equalTo('f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'))
            assertThat target,
                    is(equalTo('us-west-2'))
        }
    }

    @Test
    void derived_app_version_and_name_normal() {

        def request = new CloudhubV2DeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2', '4.3.0'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'prefix',
                                                    'new-app',
                                                    '2.2.9',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        request.with {
            assertThat appName,
                       is(equalTo('new-app'))
            assertThat normalizedAppName,
                       is(equalTo('prefix-new-app-dev'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            assertThat groupId,
                    is(equalTo('f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'))
            assertThat target,
                    is(equalTo('us-west-2'))
            assertThat 'artifactId in the POM',
                       appName,
                       is(equalTo('new-app'))
            assertThat 'app.runtime in the POM',
                    workerSpecRequest.muleVersion,
                    is(equalTo('4.3.0'))
        }
    }

    @Test
    void spaces_in_name() {

        def exception = shouldFail {
            new CloudhubV2DeploymentRequest('DEV',
                                          new WorkerSpecRequest('us-west-2'),
                                          'theKey',
                                          'theClientId',
                                          'theSecret',
                                          'client',
                                          'some app name',
                                          '4.2.2',
                                          'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')
        }

        MatcherAssert.assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }

    @Test
    void getCloudhubAppInfo_only_required() {

        def request = new CloudhubV2DeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2', '4.3.0'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    null,
                                                    'new-app',
                                                    '2.2.9',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        def appInfo = request.getCloudhubAppInfo()

        assertThat appInfo,
                   is(equalTo([
                           name: 'new-app-dev',
                           application: [
                                   ref: [
                                           groupId: 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee',
                                           artifactId : 'new-app',
                                           version: '2.2.9',
                                           packaging: "jar"
                                   ],
                                   desiredState: "STARTED",
                                   configuration: [
                                           "mule.agent.application.properties.service": [
                                                   applicationName: 'new-app'
                                           ]
                                   ],
                                   "vCores": VCoresSize.vCore1GB.vCoresSize
                           ],
                           target: [
                                   targetId: null,
                                   provider: "MC",
                                   deploymentSettings: [
                                           runtimeVersion: '4.3.0',
                                           lastMileSecurity: false,
                                           persistentObjectStore: false,
                                           clustered: false,
                                           updateStrategy: UpdateStrategy.rolling,
                                           enforceDeployingReplicasAcrossNodes: false,
                                           forwardSslSession: false,
                                           disableAmLogForwarding: true,
                                           generateDefaultPublicUrl: false
                                   ],
                                   replicas: 1
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_all_properties() {

        def request = new CloudhubV2DeploymentRequest('DEV',
                new WorkerSpecRequest('us-west-2',
                        '4.2.2',
                        true,
                        true,
                        true,
                        UpdateStrategy.recreate,
                        true,
                        true,
                        VCoresSize.vCore15GB,
                        13,
                        true,
                        false),
                'theKey',
                'theClientId',
                'theSecret',
                'prefix',
                'new-app',
                '1.2.3',
                'new-group-id')

        def appInfo = request.getCloudhubAppInfo()

        assertThat appInfo,
                   is(equalTo([
                           name: 'prefix-new-app-dev',
                           application: [
                                   ref: [
                                           groupId: 'new-group-id',
                                           artifactId : 'new-app',
                                           version: '1.2.3',
                                           packaging: "jar"
                                   ],
                                   desiredState: "STARTED",
                                   configuration: [
                                           "mule.agent.application.properties.service": [
                                                   applicationName: 'new-app',
                                           ]
                                   ],
                                   "vCores": VCoresSize.vCore15GB.vCoresSize
                           ],
                           target: [
                                   targetId: null,
                                   provider: "MC",
                                   deploymentSettings: [
                                           runtimeVersion: '4.2.2',
                                           lastMileSecurity: true,
                                           persistentObjectStore: true,
                                           clustered: true,
                                           updateStrategy: UpdateStrategy.recreate,
                                           enforceDeployingReplicasAcrossNodes: true,
                                           forwardSslSession: true,
                                           disableAmLogForwarding: false,
                                           generateDefaultPublicUrl: true
                                   ],
                                   replicas: 13
                           ]
                   ]))
    }

}
