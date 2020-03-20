package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.ClientEnforcementPolicyBasicAuth
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import org.junit.Test

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
}
