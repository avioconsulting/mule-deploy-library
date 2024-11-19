package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.*
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class PolicyListContextTest {
    @Test
    void list() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            policy {
                assetId 'the-asset-id'
                version '1.2.1'
                config hello: 'there'
            }
            clientEnforcementPolicyBasic()
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(2))
        assertThat result[0],
                   is(instanceOf(Policy))
        assertThat result[1],
                   is(instanceOf(ClientEnforcementPolicyBasicAuth))
    }

    @Test
    void mulesoft_policy() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            mulesoftPolicy {
                assetId 'the-asset-id'
                version '1.2.1'
                config hello: 'there'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(1))
        def policy = result[0]
        assertThat policy,
                   is(instanceOf(Policy))
        assertThat policy.groupId,
                   is(equalTo(Policy.getMulesoftGroupId()))
    }

    @Test
    void clientEnforcementPolicyBasic_with_details() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            policy {
                assetId 'the-asset-id'
                version '1.2.1'
                config hello: 'there'
            }
            clientEnforcementPolicyBasic { version '1.4.1' }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(2))
        assertThat result[0],
                   is(instanceOf(Policy))
        assertThat result[1],
                   is(instanceOf(ClientEnforcementPolicyBasicAuth))
    }

    @Test
    void clientEnforcementPolicyCustom_with_details() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            policy {
                assetId 'the-asset-id'
                version '1.2.1'
                config hello: 'there'
            }
            clientEnforcementPolicyCustom { version '1.4.1' }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                is(equalTo(2))
        assertThat result[0],
                is(instanceOf(Policy))
        assertThat result[1],
                is(instanceOf(ClientEnforcementPolicyCustomAuth))
    }

    @Test
    void no_policies_on_purpose() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result,
                   is(equalTo([]))
    }

    @Test
    void jwtPolicy() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            jwtPolicy {
                jwksUrl 'https://stuff'
                expectedAudience 'https://aud'
                expectedIssuer 'https://issuer'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(1))
        assertThat result[0],
                   is(instanceOf(JwtPolicy))
    }

    @Test
    void DLBIPWhiteListPolicy() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            DLBIPWhiteListPolicy {
                ipsToAllow ' 192.168.1.1'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(1))
        assertThat result[0],
                   is(instanceOf(DLBIPWhiteListPolicy))
    }

    @Test
    void azureAdJwtPolicy() {
        // arrange
        def context = new PolicyListContext()
        def closure = {
            azureAdJwtPolicy {
                azureAdTenantId 'abcd'
                expectedAudience 'https://aud'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createPolicyList()

        // assert
        assertThat result.size(),
                   is(equalTo(1))
        assertThat result[0],
                   is(instanceOf(AzureAdJwtPolicy))
    }
}
