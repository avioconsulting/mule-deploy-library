package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.*
import org.junit.jupiter.api.Test

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
            expectedIssuer 'https://issuer'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(instanceOf(JwtPolicy))
        assertThat request.version,
                   is(equalTo('1.3.1'))
    }

    @Test
    void jwt_policy_custom_stuff() {
        // arrange
        def context = new JwtPolicyBasicContext()
        def closure = {
            jwksUrl 'https://stuff'
            expectedAudience 'https://aud'
            expectedIssuer 'https://issuer'
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
                           jwtOrigin             : 'httpBearerAuthenticationHeader',
                           jwtExpression         : "#[attributes.headers['jwt']]",
                           signingMethod         : 'rsa',
                           textKey               : 'your-(256|384|512)-bit-secret',
                           signingKeyLength      : 256,
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://stuff',
                           jwksServiceTimeToLive : 90,
                           skipClientIdValidation: true,
                           clientIdExpression    : 'othercliid',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://aud',
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://issuer'
                                   ],
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
                           mandatoryNbfClaim     : true
                   ]))
    }

    @Test
    void azureAdJwtPolicy() {
        // arrange
        def context = new AzureAdJwtPolicyBasicContext()
        def closure = {
            azureAdTenantId 'abcd'
            expectedAudience 'https://aud'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(instanceOf(AzureAdJwtPolicy))
    }

    @Test
    void azureAdJwtPolicy_custom_stuff() {
        // arrange
        def context = new AzureAdJwtPolicyBasicContext()
        def closure = {
            azureAdTenantId 'abcd'
            expectedAudience 'https://aud'
            requireRole 'role1'
            requireRole 'role2'
            validateClaim 'foo',
                          'bar'
            skipClientIdEnforcement()
            jwksCachingTtlInMinutes 90
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                   is(instanceOf(AzureAdJwtPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           jwtOrigin             : 'httpBearerAuthenticationHeader',
                           jwtExpression         : "#[attributes.headers['jwt']]",
                           signingMethod         : 'rsa',
                           textKey               : 'your-(256|384|512)-bit-secret',
                           signingKeyLength      : 256,
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://login.microsoftonline.com/abcd/discovery/v2.0/keys',
                           jwksServiceTimeToLive : 90,
                           skipClientIdValidation: true,
                           clientIdExpression    : '#[vars.claimSet.appid]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://aud',
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://sts.windows.net/abcd/'
                                   ],
                                   [
                                           key  : 'roles',
                                           value: "#[(vars.claimSet.roles contains 'role1') or (vars.claimSet.roles contains 'role2')]".toString()
                                   ],
                                   [
                                           key  : 'foo',
                                           value: 'bar'
                                   ]
                           ],
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true
                   ]))
    }

    @Test
    void dlb_ip_white_list() {
        // arrange
        def context = new DLBIPWhiteListPolicyContext()
        def closure = {
            ipsToAllow(['192.168.1.1'])
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assert request,
                is(instanceOf(DLBIPWhiteListPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-2] }]",
                           ips         : [
                                   '192.168.1.1'
                           ]
                   ]))
    }

    @Test
    void dlb_ip_white_list_paths() {
        // arrange
        def context = new DLBIPWhiteListPolicyContext()
        def closure = {
            version '1.1.1'
            ipsToAllow(['192.168.1.1'])
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
        assert request,
                is(instanceOf(DLBIPWhiteListPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-2] }]",
                           ips         : [
                                   '192.168.1.1'
                           ]
                   ]))
        assertThat request.version,
                   is(equalTo('1.1.1'))
        assertThat request.policyPathApplications,
                   is(equalTo([new PolicyPathApplication([HttpMethod.PUT],
                                                         '.*bar')]))
    }

    @Test
    void dlb_ip_white_list_single_ip() {
        // arrange
        def context = new DLBIPWhiteListPolicyContext()
        def closure = {
            ipsToAllow '192.168.1.1'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assert request,
                is(instanceOf(DLBIPWhiteListPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-2] }]",
                           ips         : [
                                   '192.168.1.1'
                           ]
                   ]))
    }

    @Test
    void dlb_ip_white_list_repeat_ips() {
        // arrange
        def context = new DLBIPWhiteListPolicyContext()
        def closure = {
            ipToAllow '192.168.1.1'
            ipToAllow '192.168.2.1'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assert request,
                is(instanceOf(DLBIPWhiteListPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-2] }]",
                           ips         : [
                                   '192.168.1.1',
                                   '192.168.2.1'
                           ]
                   ]))
    }

    @Test
    void dlb_ip_white_custom_index() {
        // arrange
        def context = new DLBIPWhiteListPolicyContext()
        def closure = {
            ipsToAllow(['192.168.1.1'])
            dwListIndexToUse(-1)
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assert request,
                is(instanceOf(DLBIPWhiteListPolicy))
        assertThat request.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-1] }]",
                           ips         : [
                                   '192.168.1.1'
                           ]
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
    void client_enforcement_policy_custom_minimum() {
        // arrange
        def context = new ClientEnforcementPolicyCustomContext()
        def closure = {
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request,
                is(equalTo(new ClientEnforcementPolicyCustomAuth()))
    }

    @Test
    void client_enforcement_policy_custom_full(){
        def context = new ClientEnforcementPolicyCustomContext()
        def closure = {
            version '1.4.1'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createPolicyModel()

        // assert
        assertThat request.version,
            is(equalTo("1.4.1"))
        assertThat request.policyConfiguration,
                is(equalTo([
                    credentialsOriginHasHttpBasicAuthenticationHeader : "customExpression",
                    clientIdExpression : "#[attributes.headers['client_id']]",
                    clientSecretExpression : "#[attributes.headers['client_secret']]"
                ]))
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
