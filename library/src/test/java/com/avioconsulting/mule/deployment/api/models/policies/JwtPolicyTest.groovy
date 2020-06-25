package com.avioconsulting.mule.deployment.api.models.policies

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class JwtPolicyTest {
    @Test
    void standard() {
        // arrange

        // act
        def model = new JwtPolicy('https://foo',
                                  'https://the_audience',
                                  'https://theissuer')

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('jwt-validation'))
        assertThat model.version,
                   is(equalTo('1.1.2'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           jwtOrigin             : 'httpBearerAuthenticationHeader',
                           jwtExpression         : "#[attributes.headers['jwt']]",
                           signingMethod         : 'rsa',
                           textKey               : 'your-(256|384|512)-bit-secret',
                           signingKeyLength      : 256,
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://foo',
                           jwksServiceTimeToLive : 60,
                           skipClientIdValidation: false,
                           clientIdExpression    : '#[vars.claimSet.client_id]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true,
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://theissuer'
                                   ]
                           ]
                   ]))
        println "tostring is ${model.toString()}"
    }

    @Test
    void ignores_text_key_in_comparison() {
        // arrange
        def proposed = new JwtPolicy('https://foo',
                                     'https://the_audience',
                                     'https://theissuer')
        def proposedPolicies = [proposed]
        def existingConfigDoesNotShowTextKey = new HashMap<String, Object>(proposed.policyConfiguration)
        existingConfigDoesNotShowTextKey.remove('textKey')
        def existingPolicies = [new Policy(proposed.assetId,
                                           proposed.version,
                                           existingConfigDoesNotShowTextKey,
                                           proposed.groupId,
                                           proposed.policyPathApplications)]

        // act + assert
        assertThat existingPolicies == proposedPolicies,
                   is(true)
    }

    @Test
    void customClaimValidations() {
        // arrange

        // act
        def model = new JwtPolicy('https://foo',
                                  'https://the_audience',
                                  'https://theissuer',
                                  null,
                                  [
                                          roles: '#[vars.claimSet.roles contains \'Access.To.App\']'
                                  ])

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('jwt-validation'))
        assertThat model.version,
                   is(equalTo('1.1.2'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           jwtOrigin             : 'httpBearerAuthenticationHeader',
                           jwtExpression         : "#[attributes.headers['jwt']]",
                           signingMethod         : 'rsa',
                           textKey               : 'your-(256|384|512)-bit-secret',
                           signingKeyLength      : 256,
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://foo',
                           jwksServiceTimeToLive : 60,
                           skipClientIdValidation: false,
                           clientIdExpression    : '#[vars.claimSet.client_id]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://theissuer'
                                   ],
                                   [
                                           key  : 'roles',
                                           value: '#[vars.claimSet.roles contains \'Access.To.App\']'
                                   ]
                           ],
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true
                   ]))
        println "tostring is ${model.toString()}"
    }

    @Test
    void other_custom_attr() {
        // arrange

        // act
        def model = new JwtPolicy('https://foo',
                                  'https://the_audience',
                                  'https://theissuer',
                                  null,
                                  [:],
                                  'othercliid',
                                  true,
                                  90)

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('jwt-validation'))
        assertThat model.version,
                   is(equalTo('1.1.2'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           jwtOrigin             : 'httpBearerAuthenticationHeader',
                           jwtExpression         : "#[attributes.headers['jwt']]",
                           signingMethod         : 'rsa',
                           textKey               : 'your-(256|384|512)-bit-secret',
                           signingKeyLength      : 256,
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://foo',
                           jwksServiceTimeToLive : 90,
                           skipClientIdValidation: true,
                           clientIdExpression    : 'othercliid',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true,
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://theissuer'
                                   ]
                           ]
                   ]))
        println "tostring is ${model.toString()}"
    }
}
