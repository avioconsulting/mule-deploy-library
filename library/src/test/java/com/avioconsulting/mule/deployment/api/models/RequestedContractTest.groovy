package com.avioconsulting.mule.deployment.api.models

import org.junit.Test

import static org.junit.Assert.assertTrue

class RequestedContractTest {

    @Test
    void buildJsonRequest_standard() {
        RequestedContract requestedContract = new RequestedContract("my-client", "test-api")
        requestedContract.version = "1.0.0"
        requestedContract.versionGroup = "v1"
        requestedContract.organizationId = "my-org"
        requestedContract.environmentId = "envId"
        requestedContract.apiManagerId = 123123
        assert requestedContract.buildJsonRequest(), is(
                [
                        "apiId": "123123",
                        "environmentId": "envId",
                        "instanceType": "api",
                        "acceptedTerms": true,
                        "organizationId": "my-org",
                        "groupId": "my-org",
                        "assetId": "test-api",
                        "version": "1.0.0",
                        "versionGroup": "v1"
                ]
        )
    }
}
