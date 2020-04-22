package com.avioconsulting.mule.deployment.api.models.policies

import org.junit.Assert
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
                           jwtKeyOrigin          : 'jwks',
                           jwksUrl               : 'https://foo',
                           skipClientIdValidation: false,
                           clientIdExpression    : '#[vars.claimSet.client_id]',
                           validateAudClaim      : true,
                           mandatoryAudClaim     : true,
                           supportedAudiences    : 'https://the_audience',
                           mandatoryExpClaim     : true,
                           mandatoryNbfClaim     : true
                   ]))
        println "tostring is ${model.toString()}"
        def compare = new Policy(model.assetId,
                                 model.version,
                                 model.policyConfiguration,
                                 model.groupId,
                                 model.policyPathApplications)
        assertThat model,
                   is(equalTo(compare))
    }

    @Test
    void customClaimValidations() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void other_custom_attr() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
