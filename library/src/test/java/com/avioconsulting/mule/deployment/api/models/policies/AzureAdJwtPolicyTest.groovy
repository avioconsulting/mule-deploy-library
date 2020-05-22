package com.avioconsulting.mule.deployment.api.models.policies

import org.junit.Assert
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class AzureAdJwtPolicyTest {
    @Test
    void standard() {
        // arrange + act
        def model = new AzureAdJwtPolicy('1234',
                                         'https://the_audience')

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
                           jwksUrl               : 'https://login.microsoftonline.com/1234/discovery/v2.0/keys',
                           jwksServiceTimeToLive : 60,
                           skipClientIdValidation: false,
                           clientIdExpression    : '#[vars.claimSet.appid]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true,
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://sts.windows.net/1234/'
                                   ]
                           ]
                   ]))
        println "tostring is ${model.toString()}"
    }

    @Test
    void with_role_single() {
        // arrange + act
        def model = new AzureAdJwtPolicy('1234',
                                         'https://the_audience',
                                         ['role1'])

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
                           jwksUrl               : 'https://login.microsoftonline.com/1234/discovery/v2.0/keys',
                           jwksServiceTimeToLive : 60,
                           skipClientIdValidation: false,
                           clientIdExpression    : '#[vars.claimSet.appid]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true,
                           validateCustomClaim   : true,
                           mandatoryCustomClaims : [
                                   [
                                           key  : 'iss',
                                           value: 'https://sts.windows.net/1234/'
                                   ],
                                   [
                                           key  : 'roles',
                                           value: "#[(vars.claimSet.roles contains 'role1')]"
                                   ]
                           ]
                   ]))
        println "tostring is ${model.toString()}"
    }

    @Test
    void with_role_multiple() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void custom_claims_no_roles() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void custom_claims_with_roles() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
