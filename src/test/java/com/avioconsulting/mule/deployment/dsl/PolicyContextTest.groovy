package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import org.junit.Assert
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class PolicyContextTest {
    @Test
    void default_mule_policy_minimum() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(equalTo(new Policy(Policy.mulesoftGroupId,
                                         'the-asset-id',
                                         '1.2.1',
                                         [hello: 'there'])))
    }

    @Test
    void default_mule_policy_full() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            groupId 'the-group-id'
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
            paths {
                path {
                    method GET
                    method PUT
                    regex '.*foo'
                }
                path {
                    method PUT
                    regex '.*bar'
                }
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(equalTo(new Policy('the-group-id',
                                         'the-asset-id',
                                         '1.2.1',
                                         [
                                                 hello: 'there'
                                         ],
                                         [
                                                 new PolicyPathApplication([HttpMethod.GET,
                                                                            HttpMethod.PUT],
                                                                           '.*foo'),
                                                 new PolicyPathApplication([HttpMethod.PUT],
                                                                           '.*bar')
                                         ])))
    }

    @Test
    void no_paths() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void path_without_methods() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void path_without_regex() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void default_policy_not_enough() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void our_own_policy() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void client_enforcement_policy_without_version() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void client_enforcement_policy_with_version() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
