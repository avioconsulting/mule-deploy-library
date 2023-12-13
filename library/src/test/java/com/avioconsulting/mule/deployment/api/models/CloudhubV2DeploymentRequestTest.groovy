package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
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

    /**
     * This test case validates that after creating a CloudhubV2DeploymentRequest object this
     * is valid according to it's contructor.
     */
    @Test
    void test_deploymentRequest_creation_ok() {

        def request = new CloudhubV2DeploymentRequest('DEV',
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

    /**
     * This case validates that the application to deploy doesn't contain spaces in it's name
     * example: appName="some app name" is not a valid name. appName="my-app" is a valid one.
     */
    @Test
    void spaces_in_nametest_deploymentRequest_appName_should_not_contain_spaces() {

        def exception = shouldFail {
            new CloudhubV2DeploymentRequest('DEV',
                    new WorkerSpecRequest('us-west-2'),
                    'theKey',
                    'theClientId',
                    'theSecret',
                    new ApplicationName('some app name', true, false, 'client', null),
                    '4.2.2',
                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')
        }

        MatcherAssert.assertThat('fail', exception.message.contains("you should specify an non-empty baseAppName. It shouldn't contain spaces as well"))
    }

    /**
     * This case validates that a new CloudhubV2DeploymentRequest contains all the attributes correctly set by using
     * getCloudhubAppInfo() method.
     */
    @Test
    void test_getCloudhubAppInfo_only_required() {

        def request = new CloudhubV2DeploymentRequest('DEV',
                                                    new WorkerSpecRequest('us-west-2', '4.3.0'),
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app',false,false,null,null),
                                                    '2.2.9',
                                                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')

        def appInfo = request.getCloudhubAppInfo()

        assertThat appInfo,
                   is(equalTo([
                           name: 'new-app',
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
                                   objectStoreV2Enabled: false,
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
    void test_deploymentRequest_getCloudhubAppInfo_ok() {

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
                new ApplicationName('new-app',true,false,'prefix',null),
                '1.2.3',
                'new-group-id')

        request.setAutoDiscoveryId("apiId", "123")

        def appInfo = request.getCloudhubAppInfo()

        assertThat appInfo,
                   is(equalTo([
                           name: 'prefix-new-app',
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
                                   objectStoreV2Enabled:true,
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

    /**
     * This case validates that the name of application to deploy is not larger that the maximum required of 42 characters.
     * however, this validation happens after build the final appName (normalizedAppName) like this: ${cloudhubAppprefix}-${appName}-${env} length should not larger than 42 characters.
     * example: cloudhubPrefix=someprefix , appName=myVeryVeryVeryLargeApplicationName, env=prod -> someprefix-myVeryVeryLargeApplicationName-prod is larger than 42 characters, it's not a valid name
     * ${cloudhubAppprefix} could be null so: myVeryVeryVeryLargeApplicationName-prod is a valid name
     */
    @Test
    void test_deploymentRequest_appName_should_not_larger_than_required() {

        def appName = 'app-myVeryVeryVeryLargeApplicationName'
        def cloudHubPrefix = 'client'
        def environment = 'DEV'
        def exception = shouldFail {
            new CloudhubV2DeploymentRequest(environment,
                    new WorkerSpecRequest('us-west-2'),
                    'theKey',
                    'theClientId',
                    'theSecret',
                    new ApplicationName(appName,true,false,'cloudHubPrefix',null),
                    '4.2.2',
                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')
        }

        MatcherAssert.assertThat exception.message,
                is(equalTo("Maximum size of application name is 42 and the provided name has 53 characters"))
    }

    @Test
    void test_deploymentRequest_appName_empty() {

        def appName = null
        def cloudHubPrefix = 'client'
        def environment = 'DEV'
        def exception = shouldFail {
            new CloudhubV2DeploymentRequest(environment,
                    new WorkerSpecRequest('us-west-2'),
                    'theKey',
                    'theClientId',
                    'theSecret',
                    new ApplicationName(appName, false, false, null, null),
                    '4.2.2',
                    'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee')
        }

        MatcherAssert.assertThat exception.message,
                is(equalTo("Property applicationName.baseAppName is required for CloudHub 2.0 and RTF applications"))
    }

}
