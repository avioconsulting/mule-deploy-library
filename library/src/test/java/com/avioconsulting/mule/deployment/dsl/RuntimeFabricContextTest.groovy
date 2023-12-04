package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import org.hamcrest.MatcherAssert
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(['GroovyAssignabilityCheck'])
class RuntimeFabricContextTest implements MavenInvoke {
    @BeforeClass
    static void setup() {
        buildApp()
    }

    @Test
    void required_only() {
        // arrange
        def context = new RuntimeFabricContext()
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
                usePrefix true
                useSuffix true
                prefix 'AVI'
                suffix 'dev'
            }
            workerSpecs {
                target 'target_name'
                muleVersion '4.3.0'
            }
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            assertThat environment,
                       is(equalTo('DEV'))
            assertThat applicationName.normalizedAppName,
                       is(equalTo('avi-the-app-dev'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat anypointClientId,
                       is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                       is(equalTo('the_client_secret'))
            workerSpecRequest.with {
                assertThat target,
                        is(equalTo('target_name'))
                assertThat muleVersion,
                        is(equalTo('4.3.0'))
                assertThat lastMileSecurity,
                        is(equalTo(false))
                assertThat persistentObjectStore,
                        is(equalTo(false))
                assertThat clustered,
                        is(equalTo(false))
                assertThat updateStrategy,
                        is(equalTo(UpdateStrategy.rolling))
                assertThat replicasAcrossNodes,
                        is(equalTo(false))
                assertThat publicURL,
                        is(equalTo(false))
                assertThat replicaSize,
                        is(equalTo(VCoresSize.vCore1GB))
                assertThat workerCount,
                        is(equalTo(1))
            }
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new RuntimeFabricContext()
        def closure = {
            appVersion '1.2.3'
        }
        closure.delegate = context

        // act
        closure.call()
        def exception = shouldFail {
            context.createDeploymentRequest()
        }

        MatcherAssert.assertThat exception.message,
                is(equalTo("""Your deployment request is not complete. The following errors exist:
- businessGroupId missing
- cryptoKey missing
- environment missing
- workerSpecs.muleVersion missing
- workerSpecs.target missing
- autoDiscovery.clientId missing
- autoDiscovery.clientSecret missing
""".trim()))
    }

    @Test
    void include_optional() {
        // arrange
        def context = new RuntimeFabricContext()
        def closure = {
            environment 'DEV'
            applicationName {
                baseAppName 'the-app'
                usePrefix false
                useSuffix false
                prefix 'AVI'
                suffix 'dev'
            }
            appVersion '2.2.9'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            businessGroupId '123-456-789'
            workerSpecs {
                target 'target_name'
                muleVersion '4.3.0'
                lastMileSecurity true
                persistentObjectStore true
                clustered true
                updateStrategy UpdateStrategy.recreate
                replicasAcrossNodes true
                publicURL true
                replicaSize VCoresSize.vCore15GB
                workerCount 13
                cpuReserved 30
                memoryReserved 800
            }
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            assertThat environment,
                    is(equalTo('DEV'))
            assertThat applicationName.normalizedAppName,
                    is(equalTo('the-app'))
            assertThat appVersion,
                    is(equalTo('2.2.9'))
            assertThat cryptoKey,
                    is(equalTo('theKey'))
            assertThat anypointClientId,
                    is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                    is(equalTo('the_client_secret'))
            workerSpecRequest.with {
                assertThat target,
                        is(equalTo('target_name'))
                assertThat muleVersion,
                        is(equalTo('4.3.0'))
                assertThat lastMileSecurity,
                        is(equalTo(true))
                assertThat persistentObjectStore,
                        is(equalTo(true))
                assertThat clustered,
                        is(equalTo(true))
                assertThat updateStrategy,
                        is(equalTo(UpdateStrategy.recreate))
                assertThat replicasAcrossNodes,
                        is(equalTo(true))
                assertThat publicURL,
                        is(equalTo(true))
                assertThat replicaSize,
                        is(equalTo(VCoresSize.vCore15GB))
                assertThat workerCount,
                        is(equalTo(13))
                assertThat cpuReserved,
                        is(equalTo("30m"))
                assertThat memoryReserved,
                        is(equalTo("800Mi"))
            }
        }
    }

    @Test
    void repeat_field() {
        // arrange
        def context = new RuntimeFabricContext()
        def closure = {
            environment 'DEV'
            environment 'DEV'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            cloudHubAppPrefix 'AVI'
            workerSpecs { target 'target_name'}
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                   is(equalTo("Field 'environment' has already been set!"))
    }

    @Test
    void repeat_closure() {
        // arrange
        def context = new RuntimeFabricContext()
        def closure = {
            environment 'DEV'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            workerSpecs { target 'target_name'}
            workerSpecs { target 'target_name2'}
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                   is(equalTo("Field 'workerSpecs' has already been set!"))
    }
}
