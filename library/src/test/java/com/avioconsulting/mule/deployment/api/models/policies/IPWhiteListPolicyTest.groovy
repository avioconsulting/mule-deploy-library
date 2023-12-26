package com.avioconsulting.mule.deployment.api.models.policies


import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class IPWhiteListPolicyTest {
    @Test
    void standard() {
        // arrange

        // act
        def model = new IPWhiteListPolicy(['192.168.1.1/24'])

        // assert
        assertThat model.groupId,
                   is(equalTo('68ef9520-24e9-4cf2-b2f5-620025690913'))
        assertThat model.assetId,
                   is(equalTo('ip-whitelist'))
        assertThat model.version,
                   is(equalTo('1.2.2'))
        assertThat model.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[attributes.headers['x-forwarded-for']]",
                           ips         : [
                                   '192.168.1.1/24'
                           ]
                   ]))
    }

    @Test
    void custom_expression() {
        // arrange

        // act
        def model = new IPWhiteListPolicy(['192.168.1.1/24'],
                                          'foo')

        // assert
        assertThat model.policyConfiguration,
                   is(equalTo([
                           ipExpression: 'foo',
                           ips         : [
                                   '192.168.1.1/24'
                           ]
                   ]))
    }
}
