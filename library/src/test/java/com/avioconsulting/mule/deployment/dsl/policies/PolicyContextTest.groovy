package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.ClientEnforcementPolicyBasicAuth
import com.avioconsulting.mule.deployment.api.models.policies.JwtPolicy
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class PolicyContextTest {
    @Test
    void default_policy_minimum() {
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
                   is(equalTo(new Policy('the-asset-id',
                                         '1.2.1',
                                         [hello: 'there'])))
    }

    @Test
    void default_policy_our_group_id() {
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
                   is(equalTo(new Policy('the-asset-id',
                                         '1.2.1',
                                         [hello: 'there'])))
    }

    @Test
    void default_policy_full() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            groupId 'the-group-id'
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
            paths {
                path {
                    method HttpMethod().get
                    method HttpMethod().put
                    regex '.*foo'
                }
                path {
                    method HttpMethod().put
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
                   is(equalTo(new Policy('the-asset-id',
                                         '1.2.1',
                                         [
                                                 hello: 'there'
                                         ],
                                         'the-group-id',
                                         [
                                                 new PolicyPathApplication([HttpMethod.GET,
                                                                            HttpMethod.PUT],
                                                                           '.*foo'),
                                                 new PolicyPathApplication([HttpMethod.PUT],
                                                                           '.*bar')
                                         ])))
    }

    @Test
    void empty_paths() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
            paths {}
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createPolicyModel()
        }

        // assert
        assertThat exception.message,
                   is(containsString("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one."))
    }

    @Test
    void path_without_methods() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
            paths {
                path {
                    //method PUT
                    regex '.*bar'
                }
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        assertThat exception.message,
                   is(containsString("'path' is missing a 'method' declaration"))
    }

    @Test
    void path_without_regex() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            assetId 'the-asset-id'
            version '1.2.1'
            config hello: 'there'
            paths {
                path {
                    method HttpMethod().put
                    //regex '.*bar'
                }
            }
        }
        closure.delegate = context

        // act
        def exception = shouldFail {
            closure.call()
        }

        // assert
        assertThat exception.message,
                   is(containsString("'path' is missing a 'regex' declaration"))
    }

    @Test
    void default_policy_not_enough() {
        // arrange
        def context = new PolicyContext()
        def closure = {
            //assetId 'the-asset-id'
            //version '1.2.1'
            //config hello: 'there'
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createPolicyModel()
        }

        // assert
        assertThat exception.message,
                   is(containsString("""Your policy spec is not complete. The following errors exist:
- policies.policy.assetId missing
- policies.policy.config missing
- policies.policy.version missing"""))
    }

    @Test
    void jwt_policy() {
        // arrange
        def context = new JwtPolicyBasicContext()
        def closure = {
            jwksUrl 'https://stuff'
            expectedAudience 'https://aud'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(instanceOf(JwtPolicy))
        assertThat request.version,
                   is(equalTo('1.1.2'))
    }

    @Test
    void jwt_policy_custom_stuff() {
        // arrange
        def context = new JwtPolicyBasicContext()
        def closure = {
            jwksUrl 'https://stuff'
            expectedAudience 'https://aud'
            validateClaim 'roles',
                          'howdy'
            validateClaim 'foo',
                          'bar'
            clientIdExpression 'othercliid'
            skipClientIdEnforcement()
            jwksCachingTtlInMinutes 90
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(instanceOf(JwtPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://stuff',
                           skipClientIdValidation: true,
                           clientIdExpression    : 'othercliid',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://aud',
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'roles',
                                           value: 'howdy'
                                   ],
                                   [
                                           key  : 'foo',
                                           value: 'bar'
                                   ]
                           ],
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true,
                           jwksServiceTimeToLive : 90
                   ]))
    }

    @Test
    void client_enforcement_policy_minimum() {
        // arrange
        def context = new ClientEnforcementPolicyBasicContext()
        def closure = {
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(equalTo(new ClientEnforcementPolicyBasicAuth()))
    }

    @Test
    void client_enforcement_policy_full() {
        // arrange
        def context = new ClientEnforcementPolicyBasicContext()
        def closure = {
            version '1.4.1'
            paths {
                path {
                    method HttpMethod().put
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
                   is(equalTo(new ClientEnforcementPolicyBasicAuth([
                                                                           new PolicyPathApplication([HttpMethod.PUT],
                                                                                                     '.*bar')
                                                                   ],
                                                                   '1.4.1')))
    }
}
