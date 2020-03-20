package com.avioconsulting.mule.deployment.dsl

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

// optimizing refs would prevent us from testing DSL resolution
@SuppressWarnings(["UnnecessaryQualifiedReference", "GroovyAssignabilityCheck"])
class CloudhubContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            applicationName 'the-app'
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
            cloudHubAppPrefix 'AVI'
        }
        closure.delegate = context

        // act
        closure.call()
        def request = context.createDeploymentRequest()

        // assert
        request.with {
            assertThat environment,
                       is(equalTo('DEV'))
            assertThat request.appName,
                       is(equalTo('the-app'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            workerSpecRequest.with {
                assertThat muleVersion,
                           is(equalTo('4.2.2'))
                assertThat usePersistentQueues,
                           is(equalTo(false))
                assertThat awsRegion,
                           is(nullValue())
                assertThat workerCount,
                           is(equalTo(1))
                assertThat workerType,
                           is(equalTo(com.avioconsulting.mule.deployment.api.models.WorkerTypes.Micro))
            }
            assertThat file,
                       is(equalTo(new File('path/to/file.jar')))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat anypointClientId,
                       is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                       is(equalTo('the_client_secret'))
            assertThat cloudHubAppPrefix,
                       is(equalTo('AVI'))
        }
    }

    @Test
    void include_optional() {
        // arrange
        def context = new CloudhubContext()
        def closure = {
            environment 'DEV'
            applicationName 'the-app'
            appVersion '1.2.3'
            workerSpecs {
                // only muleVersion is required
                muleVersion '4.2.2'
                usePersistentQueues true
                workerType WorkerTypes.xLarge
                workerCount 22
                // DO NOT Optimize!!!!
                awsRegion com.avioconsulting.mule.deployment.api.models.AwsRegions.UsEast1
            }
            file 'path/to/file.jar'
            cryptoKey 'theKey'
            autoDiscovery {
                clientId 'the_client_id'
                clientSecret 'the_client_secret'
            }
            cloudHubAppPrefix 'AVI'
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
            assertThat appName,
                       is(equalTo('the-app'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            workerSpecRequest.with {
                assertThat muleVersion,
                           is(equalTo('4.2.2'))
                assertThat usePersistentQueues,
                           is(equalTo(true))
                assertThat workerType,
                           is(equalTo(com.avioconsulting.mule.deployment.api.models.WorkerTypes.xLarge))
                assertThat workerCount,
                           is(equalTo(22))
                assertThat awsRegion,
                           is(equalTo(com.avioconsulting.mule.deployment.api.models.AwsRegions.UsEast1))
            }
            assertThat file,
                       is(equalTo(new File('path/to/file.jar')))
            assertThat cryptoKey,
                       is(equalTo('theKey'))
            assertThat anypointClientId,
                       is(equalTo('the_client_id'))
            assertThat anypointClientSecret,
                       is(equalTo('the_client_secret'))
            assertThat cloudHubAppPrefix,
                       is(equalTo('AVI'))
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

        // assert
        assertThat exception.message,
                   is(equalTo("""Your deployment request is not complete. The following errors exist:
- appVersion missing
- applicationName missing
- cloudHubAppPrefix missing
- cryptoKey missing
- environment missing
- file missing
- workerSpecs.muleVersion missing
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
            applicationName 'the-app'
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
            cloudHubAppPrefix 'AVI'
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
            applicationName 'the-app'
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
            cloudHubAppPrefix 'AVI'
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
