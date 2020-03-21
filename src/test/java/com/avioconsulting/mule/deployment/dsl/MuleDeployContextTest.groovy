package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.IDeployer
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

// optimizing refs would prevent us from testing DSL resolution
class MuleDeployContextTest {
    private MuleDeployContext context
    private String username
    private CloudhubDeploymentRequest chDeployment
    private OnPremDeploymentRequest onPremDeployment
    private ApiSpecification apiSpec
    private List<Policy> desiredPolicies
    private List<Features> enabledFeatures

    @Before
    void setup() {
        chDeployment = null
        onPremDeployment = null
        apiSpec = null
        desiredPolicies = []
        enabledFeatures = []
        username = null
        def mockDeployer = [
                deployApplication: { appDeploymentRequest,
                                     ApiSpecification apiSpecification,
                                     List<Policy> policies,
                                     List<Features> features ->
                    if (appDeploymentRequest instanceof CloudhubDeploymentRequest) {
                        chDeployment = appDeploymentRequest
                    } else {
                        onPremDeployment = appDeploymentRequest
                    }
                    apiSpec = apiSpecification
                    desiredPolicies = policies
                    enabledFeatures = features
                }
        ] as IDeployer
        def mockDeployerFactory = [
                create: { String user,
                          String password,
                          PrintStream logger,
                          String anypointOrganizationName ->
                    username = user
                    return mockDeployer
                }
        ] as IDeployerFactory
        context = new MuleDeployContext(mockDeployerFactory)
    }

    @Test
    void cloudhub() {
        // arrange
        def closure = {
            version '1.0'

            settings {
                username 'the_username'
                password 'the_password'
            }

            apiSpecification {
                name 'Design Center Project Name'
            }

            policies {
                clientEnforcementPolicyBasic()
            }

            cloudHubApplication {
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
        }
        closure.delegate = context
        closure.call()

        // act
        context.performDeployment()

        // assert
        assertThat 'At least ensure the settings context ran',
                   username,
                   is(equalTo('the_username'))
        assertThat chDeployment,
                   is(notNullValue())
        assertThat onPremDeployment,
                   is(nullValue())
        assertThat apiSpec,
                   is(notNullValue())
        assertThat desiredPolicies.size(),
                   is(equalTo(1))
        assertThat enabledFeatures,
                   is(equalTo([
                           Features.All
                   ]))
    }

    @Test
    void on_prem() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void unsupported_version() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void attempt_to_do_ch_and_on_prem() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void omit_features_if_section_gone() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    // TODO: Perhaps these tests should call Deployer's deploy methods? Should we add a dry run all the way in there so when we do a "syntax check" with the DSL, they can actually see what steps would run?
}
