package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.UpdateStrategy
import com.avioconsulting.mule.deployment.api.models.VCoresSize
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(['GroovyAssignabilityCheck'])
class RuntimeFabricContextTest{

    // Succesful Test Cases
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
                baseAppName 'new-app'
            }
            workerSpecs {
                target 'us-west-2'
                muleVersion '4.2.2'
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
                    is(equalTo('new-app'))
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
                        is(equalTo('us-west-2'))
                assertThat lastMileSecurity,
                        is(equalTo(false))
                assertThat objectStoreV2,
                        is(equalTo(true))
                assertThat clustered,
                        is(equalTo(true))
                assertThat updateStrategy,
                        is(equalTo(UpdateStrategy.rolling))
                assertThat replicasAcrossNodes,
                        is(equalTo(true))
                assertThat publicUrl,
                        is(equalTo(null))
                assertThat replicaSize,
                        is(equalTo(VCoresSize.vCore1GB))
                assertThat workerCount,
                        is(equalTo(1))
            }
        }
        def appInfo = request.getCloudhubAppInfo()

        appInfo.with {
            assertThat name,
                    is(equalTo('new-app'))
            application.with {
                ref.with {
                    assertThat groupId,
                            is(equalTo('123-456-789'))
                    assertThat artifactId,
                            is(equalTo('new-app'))
                    assertThat version,
                            is(equalTo('2.2.9'))
                    assertThat packaging,
                            is(equalTo('jar'))
                }
                assertThat desiredState,
                        is(equalTo('STARTED'))
                configuration.with {
                    it.with {
                        assertThat it."mule.agent.application.properties.service".applicationName,
                                is(equalTo('new-app'))
                        assertThat it."mule.agent.application.properties.service".properties,
                                is(equalTo(['anypoint.platform.client_id'       : 'the_client_id', 'env': 'dev',
                                            'anypoint.platform.visualizer.layer': null]))
                        assertThat it."mule.agent.application.properties.service".secureProperties,
                                is(equalTo(['anypoint.platform.client_secret': 'the_client_secret', 'crypto.key': 'theKey']))
                    }
                }
                integrations.with {
                    services.with {
                        objectStoreV2.with {
                            assertThat enabled,
                                    is(equalTo(true))
                        }
                    }
                }
                assertThat vCores,
                        is(equalTo(VCoresSize.vCore1GB))
            }
            target.with {
                assertThat targetId,
                        is(equalTo(null))
                assertThat provider,
                        is(equalTo('MC'))
                deploymentSettings.with {
                    assertThat clustered,
                            is(equalTo(true))
                    assertThat updateStrategy,
                            is(equalTo(UpdateStrategy.rolling))
                    assertThat enforceDeployingReplicasAcrossNodes,
                            is(equalTo(true))
                    assertThat disableAmLogForwarding,
                            is(equalTo(false))
                    assertThat generateDefaultPublicUrl,
                            is(equalTo(true))
                    http.with {
                        inbound.with {
                            assertThat publicUrl,
                                    is(equalTo(null))
                            assertThat pathRewrite,
                                    is(equalTo(null))
                            assertThat lastMileSecurity,
                                    is(equalTo(false))
                            assertThat forwardSslSession,
                                    is(equalTo(false))
                        }
                    }
                    assertThat jvm,
                            is(equalTo([:]))
                    assertThat outbound,
                            is(equalTo([:]))
                    runtime.with {
                        assertThat version,
                                is(equalTo('4.2.2'))
                        assertThat releaseChannel,
                                is(equalTo('LTS'))
                        assertThat java,
                                is(equalTo('8'))
                    }
                    assertThat tracingEnabled,
                            is(equalTo(false))
                    resources.with {
                        cpu.with {
                            assertThat reserved,
                                    is(equalTo('20m'))
                        }
                        memory.with {
                            assertThat reserved,
                                    is(equalTo('700Mi'))
                        }
                    }
                }
                assertThat replicas,
                    is(equalTo(1))
            }
        }
    }

    @Test
    void include_optional() {
        // arrange
        def context = new RuntimeFabricContext()
        def closure = {
            environment 'DEV'
            applicationName {
                baseAppName 'new-app'
                prefix 'prefix'
                suffix 'suffix'
            }
            appVersion '2.2.9'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            businessGroupId '123-456-789'
            workerSpecs {
                muleVersion '4.6.9'
                releaseChannel 'LTS'
                javaVersion '17'

                target 'us-west-2'
                workerCount 2
                replicaSize VCoresSize.vCore2GB
                cpuReserved 30
                memoryReserved 800
                replicasAcrossNodes true
                clustered false
                updateStrategy UpdateStrategy.recreate

                publicUrl 'https://api.mycompany.com/my-api'
                generateDefaultPublicUrl false
                pathRewrite 'newpath'
                lastMileSecurity true
                forwardSslSession true

                objectStoreV2 false
                disableAmLogForwarding true
                tracingEnabled true
            }
            appProperties ([
                prop1: 'value1',
                prop2: 'value2'
            ])
            appSecureProperties ([
                secureProp1: 'secureValue1',
                secureProp2: 'secureValue2'
            ])
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()
        request.setAutoDiscoveryId("apiId", "123")
        // assert
        request.with {
            assertThat environment,
                    is(equalTo('DEV'))
            assertThat applicationName.normalizedAppName,
                    is(equalTo('prefix-new-app-suffix'))
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
                        is(equalTo('us-west-2'))
                assertThat muleVersion,
                        is(equalTo('4.6.9'))
                assertThat lastMileSecurity,
                        is(equalTo(true))
                assertThat objectStoreV2,
                        is(equalTo(false))
                assertThat clustered,
                        is(equalTo(false))
                assertThat updateStrategy,
                        is(equalTo(UpdateStrategy.recreate))
                assertThat replicasAcrossNodes,
                        is(equalTo(true))
                assertThat publicUrl,
                        is(equalTo('https://api.mycompany.com/my-api'))
                assertThat replicaSize,
                        is(equalTo(VCoresSize.vCore2GB))
                assertThat workerCount,
                        is(equalTo(2))
                assertThat cpuReserved,
                        is(equalTo("30m"))
                assertThat memoryReserved,
                        is(equalTo("800Mi"))
            }
        }

        def appInfo = request.getCloudhubAppInfo()

        appInfo.with {
            assertThat name,
                    is(equalTo('prefix-new-app-suffix'))
            application.with {
                ref.with {
                    assertThat groupId,
                            is(equalTo('123-456-789'))
                    assertThat artifactId,
                            is(equalTo('new-app'))
                    assertThat version,
                            is(equalTo('2.2.9'))
                    assertThat packaging,
                            is(equalTo('jar'))
                }
                assertThat desiredState,
                        is(equalTo('STARTED'))
                configuration.with {
                    it.with {
                        assertThat it."mule.agent.application.properties.service".applicationName,
                                is(equalTo('new-app'))
                        assertThat it."mule.agent.application.properties.service".properties,
                                is(equalTo(['apiId': '123', 'anypoint.platform.client_id'       : 'the_client_id', 'env': 'dev',
                                            'anypoint.platform.visualizer.layer': null, 'prop1': 'value1', 'prop2': 'value2']))
                        assertThat it."mule.agent.application.properties.service".secureProperties,
                                is(equalTo(['anypoint.platform.client_secret': 'the_client_secret', 'crypto.key': 'theKey',
                                            'secureProp1': 'secureValue1', 'secureProp2': 'secureValue2']))
                    }
                }
                integrations.with {
                    services.with {
                        objectStoreV2.with {
                            assertThat enabled,
                                    is(equalTo(false))
                        }
                    }
                }
                assertThat vCores,
                        is(equalTo(VCoresSize.vCore2GB))
            }
            target.with {
                assertThat targetId,
                        is(equalTo(null))
                assertThat provider,
                        is(equalTo('MC'))
                deploymentSettings.with {
                    assertThat clustered,
                            is(equalTo(false))
                    assertThat updateStrategy,
                            is(equalTo(UpdateStrategy.recreate))
                    assertThat enforceDeployingReplicasAcrossNodes,
                            is(equalTo(true))
                    assertThat disableAmLogForwarding,
                            is(equalTo(true))
                    assertThat generateDefaultPublicUrl,
                            is(equalTo(false))
                    http.with {
                        inbound.with {
                            assertThat publicUrl,
                                    is(equalTo('https://api.mycompany.com/my-api'))
                            assertThat pathRewrite,
                                    is(equalTo('newpath'))
                            assertThat lastMileSecurity,
                                    is(equalTo(true))
                            assertThat forwardSslSession,
                                    is(equalTo(true))
                        }
                    }
                    assertThat jvm,
                            is(equalTo([:]))
                    assertThat outbound,
                            is(equalTo([:]))
                    runtime.with {
                        assertThat version,
                                is(equalTo('4.6.9'))
                        assertThat releaseChannel,
                                is(equalTo('LTS'))
                        assertThat java,
                                is(equalTo('17'))
                    }
                    assertThat tracingEnabled,
                            is(equalTo(true))
                    resources.with {
                        cpu.with {
                            assertThat reserved,
                                    is(equalTo('30m'))
                        }
                        memory.with {
                            assertThat reserved,
                                    is(equalTo('800Mi'))
                        }
                    }
                }
                assertThat replicas,
                        is(equalTo(2))
            }
        }
    }

    // Failure Test Cases
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
- workerSpecs.target missing
- autoDiscovery.clientId missing
- autoDiscovery.clientSecret missing
""".trim()))
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
    @Test
    void test_deploymentRequest_appName_should_not_have_spaces_in_name() {

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
                baseAppName 'some app name'
            }
            workerSpecs {
                target 'target_name'
            }
        }
        closure.delegate = context
        closure.call()
        // act
        def exception = shouldFail {
            def request = context.createDeploymentRequest()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                is(equalTo("Name must be alphanumeric with dashes allowed within. Expression: valid. Values: valid = false"))

    }

    /**
     * This case validates that the name of application to deploy is not larger that the maximum required of 42 characters.
     * however, this validation happens after build the final appName (normalizedAppName) like this: ${cloudhubAppprefix}-${appName}-${env} length should not larger than 42 characters.
     * example: ApplicationName{ baseName=myVeryVeryVeryLargeApplicationName, prefix=someprefix, suffix=prod -> someprefix-myVeryVeryLargeApplicationName-prod is larger than 42 characters, it's not a valid name
     */
    @Test
    void test_deploymentRequest_appName_should_not_larger_than_required() {

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
                baseAppName 'app-myVeryVeryVeryLargeApplicationName'
                prefix 'client'
                suffix 'DEV'
            }
            workerSpecs {
                target 'target_name'
            }
        }
        closure.delegate = context
        closure.call()
        // act
        def exception = shouldFail {
            def request = context.createDeploymentRequest()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                is(equalTo("Maximum size of application name is 42 and the provided name has 49 characters"))

    }

    @Test
    void test_deploymentRequest_appName_empty() {

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

            }
            workerSpecs {
                target 'target_name'
            }
        }
        closure.delegate = context
        closure.call()
        // act
        def exception = shouldFail {
            def request = context.createDeploymentRequest()
        }

        // assert
        MatcherAssert.assertThat exception.message,
                is(equalTo("Property applicationName.baseAppName is required for CloudHub 2.0 and RTF applications"))

    }

}
