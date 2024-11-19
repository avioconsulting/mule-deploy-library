package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(['GroovyAssignabilityCheck'])
class CloudhubV2ContextTest implements MavenInvoke {

    @Test
    void required_only() {
        // arrange
        double vCoreSizeExpected = 0.1
        def context = new CloudhubV2Context()
        def closure = {
            environment 'DEV'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            businessGroupId '123-456-789'
            appVersion '2.2.9'
            applicationName {
                baseAppName 'the-app'
                prefix 'mule'
                suffix 'dev'
            }
            workerSpecs {
                target 'us-west-2'
            }
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert basic requirements
        request.with {
            assertThat environment,
                       is(equalTo('DEV'))
            assertThat applicationName.normalizedAppName,
                       is(equalTo('mule-the-app-dev'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat groupId,
                    is(equalTo('123-456-789'))
            assertThat target,
                    is(equalTo('us-west-2'))
            // These next 3 assertions test the functionality we are over riding within CloudhubV2DeploymentRequest
            cloudhubAppInfo.target.deploymentSettings.with {
                assertThat enforceDeployingReplicasAcrossNodes,
                        is(true)
                assertThat persistentObjectStore,
                        is(false)
            }
            cloudhubAppInfo.application.with {
                assertThat vCores,
                        is(vCoreSizeExpected)
            }
        }

    }
}
