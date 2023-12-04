package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import org.hamcrest.MatcherAssert
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class RuntimeFabricDeploymentRequestTest implements MavenInvoke {
    @BeforeClass
    static void setup() {
        buildApp()
    }

    @Test
    void explicit() {

        def request = new RuntimeFabricDeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2',
                                                                           '4.2.2'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app',true,false,'prefix',null),
                                                    '1.2.3',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

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
    }

    @Test
    void derived_app_version_and_name_normal() {

        def request = new RuntimeFabricDeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2', '4.2.2'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app',true,false,'prefix',null),
                                                    '2.2.9',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        request.with {
            assertThat appName.baseAppName,
                       is(equalTo('new-app'))
            assertThat normalizedAppName,
                       is(equalTo('prefix-new-app'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            assertThat groupId,
                    is(equalTo('f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'))
            assertThat target,
                    is(equalTo('us-west-2'))
            assertThat 'artifactId in the POM',
                       appName.baseAppName,
                       is(equalTo('new-app'))
            assertThat 'app.runtime in the POM',
                    workerSpecRequest.muleVersion,
                    is(equalTo('4.2.2'))
        }
    }

    @Test
    void spaces_in_name() {

        def exception = shouldFail {
            new RuntimeFabricDeploymentRequest('DEV',
                                          new WorkerSpecRequest('us-west-2', '4.2.2'),
                                            'theKey',
                                            'theClientId',
                                            'theSecret',
                                            new ApplicationName('some app name',true,false,'prefix','DEV'),
                                            '1.2.3',
                                            'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')
        }
        MatcherAssert.assertThat('fail', exception.message.contains("you should specify an non-empty baseAppName. It shouldn't contain spaces as well"))
    }

    @Test
    void getCloudhubAppInfo_only_required() {

        def request = new RuntimeFabricDeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2', '4.2.2'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app',false,true,null,'DEV'),
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
                                                   applicationName: 'new-app',
                                                   properties: [:]
                                           ]
                                   ],
                                   integrations:[
                                           services : [
                                               objectStoreV2: [
                                                       enabled: false
                                               ]
                                           ]
                                   ],
                                   objectStoreV2Enabled:false
                           ],
                           target: [
                                   targetId: null,
                                   provider: "MC",
                                   deploymentSettings: [
                                           runtimeVersion: '4.2.2',
                                           lastMileSecurity: false,
                                           persistentObjectStore: false,
                                           clustered: false,
                                           updateStrategy: UpdateStrategy.rolling,
                                           enforceDeployingReplicasAcrossNodes: false,
                                           forwardSslSession: false,
                                           disableAmLogForwarding: true,
                                           generateDefaultPublicUrl: false,
                                           resources: [
                                                   cpu: [
                                                           reserved: "20m"
                                                   ],
                                                   memory: [
                                                           reserved: "700Mi"
                                                   ]
                                           ]
                                   ],
                                   replicas: 1
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_all_properties() {

        def request = new RuntimeFabricDeploymentRequest('DEV',
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
                        false,
                        456,
                        789),
                        'theKey',
                        'theClientId',
                        'theSecret',
                        new ApplicationName('new-app',true,true,'prefix','DEV'),
                        '1.2.3',
                        'new-group-id')

        request.setAutoDiscoveryId("apiId", "123")

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
                                                   properties     : [
                                                           apiId : '123'
                                                   ]
                                           ]
                                   ],
                                   integrations:[
                                           services : [
                                                   objectStoreV2: [
                                                           enabled: true
                                                   ]
                                           ]
                                   ],
                                   objectStoreV2Enabled:true
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
                                           generateDefaultPublicUrl: true,
                                           resources: [
                                                   cpu: [
                                                           reserved: "456m"
                                                   ],
                                                   memory: [
                                                           reserved: "789Mi"
                                                   ]
                                           ]
                                   ],
                                   replicas: 13
                           ]
                   ]))
    }

}
