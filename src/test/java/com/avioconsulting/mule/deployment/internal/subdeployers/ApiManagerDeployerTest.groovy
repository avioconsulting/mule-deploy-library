package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.internal.models.ApiManagerDefinition
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class ApiManagerDeployerTest extends BaseTest {
    private ApiManagerDeployer deployer

    @Before
    void setupDeployer() {
        deployer = new ApiManagerDeployer(clientWrapper,
                                          System.out)
    }

    @Test
    void createApiDefinition() {
        // arrange
        String url = null
        HttpMethod method = null
        Map sentPayload = null
        String env, auth, org = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            method = request.method()
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString())
            }
            (auth, org, env) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        id: 123
                ]))
            }
        }
        def apiDefinition = new ApiManagerDefinition('the-asset-id',
                                                     '1.2.3',
                                                     'someguid',
                                                     'https://some.endpoint',
                                                     'DEV',
                                                     '4.2.2')

        // act
        def id = deployer.createApiDefinition(apiDefinition)

        // assert
        assertThat id,
                   is(equalTo('123'))
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/someguid/environments/def456/apis'))
        assertThat method,
                   is(equalTo(HttpMethod.POST))
        assertThat env,
                   is(equalTo('def456'))
        assertThat sentPayload,
                   is(equalTo([
                           spec         : [
                                   groupId: 'someguid',
                                   assetId: 'the-asset-id',
                                   version: '1.2.3'
                           ],
                           endpoint     : [
                                   uri                : 'https://some.endpoint',
                                   proxyUri           : null,
                                   muleVersion4OrAbove: true,
                                   isCloudHub         : null
                           ],
                           instanceLabel: 'DEV - Automated'
                   ]))
    }

    @Test
    void createApiDefinition_mule3() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
