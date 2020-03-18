package com.avioconsulting.mule.deployment.api.models.policies


import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class ClientEnforcementPolicyBasicAuthTest {
    @Test
    void test() {
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
    }
}
