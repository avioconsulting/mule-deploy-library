package com.avioconsulting.mule.deployment.api.models.policies

import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class DLBIPWhiteListPolicyTest {
    @Test
    void standard() {
        // arrange

        // act
        def model = new DLBIPWhiteListPolicy(['192.168.1.1/24'])

        // assert
        assertThat model.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-2] }]",
                           ips         : [
                                   '192.168.1.1/24'
                           ]
                   ]))
    }

    @Test
    void custom_index() {
        // arrange

        // act
        def model = new DLBIPWhiteListPolicy(['192.168.1.1/24'],
                                             -1)

        // assert
        assertThat model.policyConfiguration,
                   is(equalTo([
                           ipExpression: "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[-1] }]",
                           ips         : [
                                   '192.168.1.1/24'
                           ]
                   ]))
    }
}
