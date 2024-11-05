package com.avioconsulting.mule.deployment.api.models.policies

import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class ClientEnforcementPolicyTest {

    @Test
    void basic_auth() {
        // arrange

        // act
        def model = new ClientEnforcementPolicyBasicAuth()

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('client-id-enforcement'))
        assertThat model.version,
                   is(equalTo('1.2.1'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           credentialsOriginHasHttpBasicAuthenticationHeader: 'httpBasicAuthenticationHeader',
                           clientIdExpression                               : "#[attributes.headers['client_id']]"
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
    void custom() {
        // arrange

        // act
        def model = new ClientEnforcementPolicyCustomAuth()

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('client-id-enforcement'))
        assertThat model.version,
                   is(equalTo('1.2.1'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           credentialsOriginHasHttpBasicAuthenticationHeader: 'customExpression',
                           clientIdExpression                               : "#[attributes.headers['client_id']]",
                           clientSecretExpression                           : "#[attributes.headers['client_secret']]"
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
}
