package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(['GroovyAssignabilityCheck'])
class CloudhubV2ContextTest implements MavenInvoke {

    @BeforeAll
    static void setup() {
        buildApp()
    }

    @Test
    void required_only() {
        // arrange
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
        def context = new CloudhubV2Context()
        def closure = {
            appVersion '1.2.3'
        }
        closure.delegate = context

        // act
        closure.call()
        def exception = shouldFail {
            context.createDeploymentRequest()
        }

        assertThat exception.message,
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
        def context = new CloudhubV2Context()
        def closure = {
            environment 'DEV'
            applicationName {
                baseAppName 'the-app'
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
            }
            appProperties ([
                someProp: 'someValue',
            ])
            appSecureProperties ([
                secureProp1: "123",
                secureProp2: "456"
            ])
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            assertThat environment,
                    is(equalTo('DEV'))
            assertThat appVersion,
                    is(equalTo('2.2.9'))
            assertThat cryptoKey,
                    is(equalTo('theKey'))
            assertThat anypointClientId,
                    is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                    is(equalTo('the_client_secret'))
            assertThat applicationName.normalizedAppName,
                    is(equalTo('the-app-dev'))
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
            }
            assertThat appProperties.someProp,
                    is(equalTo('someValue'))
            assertThat appSecureProperties.secureProp1,
                    is(equalTo('123'))
            assertThat appSecureProperties.secureProp2,
                    is(equalTo('456'))
        }
    }

    @Test
    void repeat_field() {
        // arrange
        def context = new CloudhubV2Context()
        def closure = {
            environment 'DEV'
            environment 'DEV'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
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
        def context = new CloudhubV2Context()
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

    @Test
    void repeat_child_closure() {
        // arrange
        def context = new CloudhubV2Context()
        def closure = {
            environment 'DEV'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            workerSpecs {
                target 'target_name'
                target 'target_name_2'
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                is(equalTo("Field 'target' has already been set!"))
    }
}
