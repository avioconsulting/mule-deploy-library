package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import com.avioconsulting.mule.deployment.internal.AppBuilding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

// optimizing refs would prevent us from testing DSL resolution
class MuleDeployContextTest implements AppBuilding {
    private MuleDeployContext context
    private Map params

    @BeforeEach
    void setup() {
        this.params = [:]
        context = new MuleDeployContext(this.params)
    }

    @Test
    void cloudhub() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            cloudHubApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                workerSpecs {
                    muleVersion '4.2.2'
                }
                file tempRequest.file.path
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.deploymentRequest,
                   is(instanceOf(CloudhubDeploymentRequest))
        assertThat deploymentPackage.apiSpecifications.size(),
                   is(not(0))
        assertThat deploymentPackage.desiredPolicies.size(),
                   is(equalTo(1))
        assertThat deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.All
                   ]))
    }

    @Test
    void on_prem() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file tempRequest.file.path
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.deploymentRequest,
                   is(instanceOf(OnPremDeploymentRequest))
        assertThat deploymentPackage.apiSpecifications.size(),
                   is(not(0))
        assertThat deploymentPackage.desiredPolicies.size(),
                   is(equalTo(1))
        assertThat deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.All
                   ]))
    }

    @Test
    void cloudhubV2() {
        // arrange
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            cloudHubV2Application {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
                workerSpecs {
                    muleVersion '4.2.2'
                    target 'sharedTarget'
                }
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.deploymentRequest,
                is(instanceOf(CloudhubV2DeploymentRequest))
        assertThat deploymentPackage.apiSpecifications.size(),
                is(equalTo(1))
        assertThat deploymentPackage.enabledFeatures,
                is(equalTo([
                        Features.AppDeployment,
                        Features.ApiManagerDefinitions
                ]))
    }

    @Test
    void runtimeFabric() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            runtimeFabricApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
                workerSpecs {
                    muleVersion '4.2.2'
                    target 'sharedTarget'
                }
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.deploymentRequest,
                is(instanceOf(RuntimeFabricDeploymentRequest))
        assertThat deploymentPackage.apiSpecifications.size(),
                is(equalTo(1))
        assertThat deploymentPackage.enabledFeatures,
                is(equalTo([
                        Features.AppDeployment,
                        Features.ApiManagerDefinitions
                ]))
    }

    @Test
    void unsupported_version() {
        // arrange
        def closure = {
            version '0.5'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            onPremApplication {
                environment 'DEV'
                applicationName 'the-app'
                appVersion '1.2.3'
                file 'path/to/file.jar'
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        assertThat exception.message,
                   is(containsString('Only version 1.0 of the DSL is supported and you are using 0.5'))
    }

    @Test
    void attempt_to_do_ch_and_on_prem() {
        // arrange
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file 'path/to/file.jar'
                targetServerOrClusterName 'theServer'
            }

            cloudHubApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
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
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentPackage()
        }

        // assert
        assertThat exception.message,
                   is(containsString('You cannot deploy both a CloudHub and on-prem application!'))
    }

    @Test
    void attempt_to_do_ch2_and_on_prem() {
        // arrange
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file 'path/to/file.jar'
                targetServerOrClusterName 'theServer'
            }

            cloudHubV2Application {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                workerSpecs {
                    muleVersion '4.2.2'
                }
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentPackage()
        }

        // assert
        assertThat exception.message,
                is(containsString('You cannot deploy both a CloudHub and on-prem application!'))
    }

    @Test
    void attempt_to_do_rtf_and_on_prem() {
        // arrange
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file 'path/to/file.jar'
                targetServerOrClusterName 'theServer'
            }

            runtimeFabricApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                workerSpecs {
                    muleVersion '4.2.2'
                }
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentPackage()
        }

        // assert
        assertThat exception.message,
                is(containsString('You cannot deploy both a CloudHub and on-prem application!'))
    }

    @Test
    void missing_stuff() {
        // arrange
        def closure = {

        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createDeploymentPackage()
        }

        // assert
        assertThat exception.message,
                   is(equalTo("""Your file is not complete. The following errors exist:
- version missing
- Either onPremApplication, cloudHubApplication, cloudHubV2Application or runtimeFabricApplication should be supplied
""".trim()))
    }

    @Test
    void optional_stuff() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file tempRequest.file.path
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.apiSpecifications.size(),
                   is(equalTo(0))
        assertThat 'Lack of policy section should remove policy sync from features',
                   deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.AppDeployment,
                           Features.DesignCenterSync,
                           Features.ApiManagerDefinitions
                   ]))
    }

    @Test
    void specify_features() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            enabledFeatures {
                apiManagerDefinitions
                appDeployment
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file tempRequest.file.path
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.ApiManagerDefinitions,
                           Features.AppDeployment
                   ]))
    }

    @Test
    void specify_features_with_policy_section_omitted() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            enabledFeatures {
                apiManagerDefinitions
                appDeployment
                policySync
            }

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix false
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file tempRequest.file.path
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat 'No policies section so will not sync',
                   deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.ApiManagerDefinitions,
                           Features.AppDeployment
                   ]))
    }

    @Test
    void empty_policies() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {}

            onPremApplication {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                file tempRequest.file.path
                targetServerOrClusterName 'theServer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.desiredPolicies,
                   is(equalTo([]))
        assertThat deploymentPackage.enabledFeatures,
                   is(equalTo([
                           Features.All
                   ]))
    }

    @Test
    void cloudhubV2_and_policies() {
        // arrange
        def tempRequest = buildFullFileBasedApp()
        def closure = {
            version '1.0'

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            cloudHubV2Application {
                environment 'DEV'
                applicationName {
                    baseAppName 'the-app'
                    usePrefix true
                    useSuffix true
                    prefix 'AVI'
                    suffix 'dev'
                }
                appVersion '1.2.3'
                businessGroupId 'f2ea2cb4-c600-4bb5-88e8-e952ff5591ee'
                workerSpecs {
                    muleVersion '4.2.2'
                    target 'sharedTarget'
                }
                cryptoKey 'theKey'
                autoDiscovery {
                    clientId 'the_client_id'
                    clientSecret 'the_client_secret'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def deploymentPackage = context.createDeploymentPackage()

        // assert
        assertThat deploymentPackage.deploymentRequest,
                is(instanceOf(CloudhubV2DeploymentRequest))
        assertThat deploymentPackage.apiSpecifications.size(),
                is(not(0))
        assertThat deploymentPackage.desiredPolicies.size(),
                is(equalTo(1))
        assertThat deploymentPackage.enabledFeatures,
                is(equalTo([
                        Features.AppDeployment,
                        Features.ApiManagerDefinitions,
                        Features.PolicySync
                ]))
    }
}
