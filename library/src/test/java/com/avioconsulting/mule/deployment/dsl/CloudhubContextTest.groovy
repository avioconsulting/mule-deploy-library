package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.AwsRegions
import com.avioconsulting.mule.deployment.api.models.WorkerTypes
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(['GroovyAssignabilityCheck'])
class CloudhubContextTest implements MavenInvoke {

    @BeforeAll
    static void setup() {
        buildApp()
    }

    @Test
    void required_only() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            workerSpecs {}
            file builtFile.absolutePath
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
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
            assertThat request.appName.normalizedAppName,
                       is(equalTo('mule-deploy-lib-v4-test-app'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            workerSpecRequest.with {
                assertThat muleVersion,
                           is(equalTo('4.3.0'))
                assertThat usePersistentQueues,
                           is(equalTo(false))
                assertThat awsRegion,
                           is(nullValue())
                assertThat workerCount,
                           is(equalTo(1))
                assertThat workerType,
                           is(equalTo(WorkerTypes.Micro))
                assertThat objectStoreV2Enabled,
                           is(equalTo(true))
            }
            assertThat analyticsAgentEnabled,
                       is(equalTo(true))
            assertThat file,
                       is(equalTo(builtFile))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat anypointClientId,
                       is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                       is(equalTo('the_client_secret'))
        }
    }

    @Test
    void no_worker_specs_needed() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            file builtFile.absolutePath
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            applicationName {}
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            workerSpecRequest.with {
                assertThat muleVersion,
                           is(equalTo('4.3.0'))
                assertThat usePersistentQueues,
                           is(equalTo(false))
                assertThat updateId,
                           is(nullValue())
                assertThat customLog4j2Enabled,
                           is(equalTo(false))
                assertThat staticIpEnabled,
                           is(equalTo(false))
                assertThat objectStoreV2Enabled,
                           is(equalTo(true))
                assertThat awsRegion,
                           is(nullValue())
                assertThat workerCount,
                           is(equalTo(1))
                assertThat workerType,
                           is(equalTo(WorkerTypes.Micro))
            }
        }
    }

    @Test
    void include_optional() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            appVersion '1.2.3'
            workerSpecs {
                muleVersion '4.2.2'
                usePersistentQueues true
                workerType WorkerTypes().xLarge
                workerCount 22
                awsRegion AwsRegions().useast1
                updateId 'abc'
                customLog4j2Enabled true
                staticIpEnabled true
                objectStoreV2Enabled false
            }
            applicationName {
                baseAppName 'the-app'
                prefix 'AVI'
            }
            analyticsAgentEnabled false
            file builtFile.absolutePath
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            // optional from here on out
            appProperties([
                    someProp: 'someValue'
            ])
            otherCloudHubProperties([
                    some_ch_value_we_havent_covered_yet: true
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
            assertThat appName.normalizedAppName,
                       is(equalTo('avi-the-app'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            workerSpecRequest.with {
                assertThat muleVersion,
                           is(equalTo('4.2.2'))
                assertThat usePersistentQueues,
                           is(equalTo(true))
                assertThat workerType,
                           is(equalTo(WorkerTypes.xLarge))
                assertThat workerCount,
                           is(equalTo(22))
                assertThat awsRegion,
                           is(equalTo(AwsRegions.UsEast1))
                assertThat updateId,
                           is(equalTo('abc'))
                assertThat customLog4j2Enabled,
                           is(equalTo(true))
                assertThat staticIpEnabled,
                           is(equalTo(true))
                assertThat objectStoreV2Enabled,
                           is(equalTo(false))
            }
            assertThat analyticsAgentEnabled,
                       is(equalTo(false))
            assertThat file.name,
                       is(equalTo('mule-deploy-lib-v4-test-app-2.2.9-mule-application.jar'))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat anypointClientId,
                       is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                       is(equalTo('the_client_secret'))
            //TODO validate app name
            assertThat appProperties,
                       is(equalTo([
                               someProp: 'someValue'
                       ]))
            assertThat otherCloudHubProperties,
                       is(equalTo([
                               some_ch_value_we_havent_covered_yet: true
                       ]))
        }
    }

    @Test
    void dsl_support() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            appVersion '1.2.3'
            workerSpecs {
                muleVersion '4.2.2'
                usePersistentQueues true
                workerType WorkerTypes().xlarge
                workerCount 22
                awsRegion AwsRegions().useast1
            }
            file builtFile.absolutePath
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            workerSpecRequest.with {
                assertThat workerType,
                           is(equalTo(WorkerTypes.xLarge))
                assertThat awsRegion,
                           is(equalTo(AwsRegions.UsEast1))
            }
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            workerSpecs {
            }
            autoDiscovery {
                clientId 'the_client_id'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentRequest()
        }

        //TODO fix here
        // assert
        assertThat exception.message,
                   is(equalTo("""Your deployment request is not complete. The following errors exist:
- cryptoKey missing
- environment missing
- file missing
- autoDiscovery.clientSecret missing
""".trim()))
    }

    @Test
    void repeat_field() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            environment 'DEV'
            appVersion '1.2.3'
            workerSpecs {
                muleVersion '4.2.2'
            }
            file 'path/to/file.jar'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Field 'environment' has already been set!"))
    }

    @Test
    void repeat_closure() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            applicationName {
                baseAppName 'the-app'
                prefix 'AVI'
            }
            appVersion '1.2.3'
            workerSpecs {
                muleVersion '4.2.2'
            }
            workerSpecs {
                muleVersion '3.9.1'
            }
            file 'path/to/file.jar'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Field 'workerSpecs' has already been set!"))
    }
}
