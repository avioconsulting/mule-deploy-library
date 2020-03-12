package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.internal.models.ApiManagerDefinition
import com.avioconsulting.mule.deployment.internal.models.ApiQueryResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
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
                                          environmentLocator,
                                          System.out)
    }

    @Test
    void createApiDefinition() {
        // arrange
        String url = null
        HttpMethod method = null
        Map sentPayload = null
        String envHeader, auth, org = null
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
            (auth, org, envHeader) = capturedStandardHeaders(request)
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
                                                     'https://some.endpoint',
                                                     'DEV',
                                                     '4.2.2')

        // act
        def id = deployer.createApiDefinition(apiDefinition)

        // assert
        assertThat id,
                   is(equalTo('123'))
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis'))
        assertThat method,
                   is(equalTo(HttpMethod.POST))
        assertThat envHeader,
                   is(equalTo('def456'))
        assertThat sentPayload,
                   is(equalTo([
                           spec         : [
                                   groupId: 'the-org-id',
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
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString())
            }
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
                                                     'https://some.endpoint',
                                                     'DEV',
                                                     '3.9.1')

        // act
        deployer.createApiDefinition(apiDefinition)

        // assert
        assertThat sentPayload,
                   is(equalTo([
                           spec         : [
                                   groupId: 'the-org-id',
                                   assetId: 'the-asset-id',
                                   version: '1.2.3'
                           ],
                           endpoint     : [
                                   uri                : 'https://some.endpoint',
                                   proxyUri           : null,
                                   muleVersion4OrAbove: false,
                                   isCloudHub         : null
                           ],
                           instanceLabel: 'DEV - Automated'
                   ]))
    }

    @Test
    void chooseApiDefinitionId_single() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter',
                                     '1.2.3')
        ]

        // act
        def result = deployer.chooseApiDefinitionId('the label',
                                                    responses)

        // assert
        assertThat result,
                   is(equalTo('1234'))
    }

    @Test
    void chooseApiDefinitionId_matching_label() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter',
                                     '1.2.3'),
                new ApiQueryResponse('4567',
                                     'the label',
                                     '1.2.3')
        ]

        // act
        def result = deployer.chooseApiDefinitionId('the label',
                                                    responses)

        // assert
        assertThat result,
                   is(equalTo('4567'))
    }

    @Test
    void chooseApiDefinitionId_no_labels() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter',
                                     '1.2.3'),
                new ApiQueryResponse('4567',
                                     'does not matter 2',
                                     '1.2.3')
        ]

        // act
        def result = deployer.chooseApiDefinitionId('the label',
                                                    responses)

        // assert
        assertThat result,
                   is(equalTo('1234'))
    }
}
