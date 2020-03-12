package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.internal.models.ApiManagerDefinition
import com.avioconsulting.mule.deployment.internal.models.ApiQueryResponse
import com.avioconsulting.mule.deployment.internal.models.ExistingApiManagerDefinition
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings("GroovyAccessibility")
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
                        id           : 123,
                        endpoint     : [
                                uri                : 'https://some.endpoint',
                                muleVersion4OrAbove: true
                        ],
                        assetId      : 'the-asset-id',
                        assetVersion : '1.2.3',
                        instanceLabel: 'DEV - Automated'
                ]))
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.2.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               true)

        // act
        def result = deployer.createApiDefinition(desiredApiDefinition)

        // assert
        assertThat result.details,
                   is(equalTo(desiredApiDefinition))
        assertThat result.id,
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
                        id           : 123,
                        endpoint     : [
                                uri                : 'https://some.endpoint',
                                muleVersion4OrAbove: false
                        ],
                        assetId      : 'the-asset-id',
                        assetVersion : '1.2.3',
                        instanceLabel: 'DEV - Automated'
                ]))
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.2.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               false)

        // act
        deployer.createApiDefinition(desiredApiDefinition)

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
    void chooseApiDefinition_single() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  responses)

        // assert
        assertThat result,
                   is(equalTo(responses[0]))
    }

    @Test
    void chooseApiDefinition_matching_label() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter'),
                new ApiQueryResponse('4567',
                                     'the label')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  responses)

        // assert
        assertThat result,
                   is(equalTo(responses[1]))
    }

    @Test
    void chooseApiDefinition_no_labels() {
        // arrange
        def responses = [
                new ApiQueryResponse('1234',
                                     'does not matter'),
                new ApiQueryResponse('4567',
                                     'does not matter 2')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  responses)

        // assert
        assertThat result,
                   is(equalTo(responses[0]))
    }

    @Test
    void getExistingApiDefinition_correct_request() {
        // arrange
        String url = null
        HttpMethod method = null
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
            (auth, org, envHeader) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        total : 0,
                        assets: []
                ]))
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.2.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               false)

        // act
        deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id'))
        assertThat method,
                   is(equalTo(HttpMethod.GET))
        assertThat envHeader,
                   is(equalTo('def456'))
    }

    @Test
    void getExistingApiDefinition_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        total : 0,
                        assets: []
                ]))
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.2.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               false)

        // act
        def response = deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat response,
                   is(nullValue())
    }

    @Test
    void getExistingApiDefinition_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                Map responseMap = null
                if (request.uri() == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.GET) {
                    responseMap = [
                            id           : 1234,
                            endpoint     : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId      : 'the-asset-id',
                            assetVersion : '1.2.3',
                            instanceLabel: 'DEV - Automated'
                    ]
                } else {
                    responseMap = [
                            total : 1,
                            assets: [
                                    [
                                            apis: [
                                                    new ApiQueryResponse('1234',
                                                                         'does not matter')
                                            ]
                                    ]
                            ]
                    ]
                }
                end(new ObjectMapper().writeValueAsString(responseMap))
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.3.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               false)

        // act
        def response = deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat response.id,
                   is(equalTo('1234'))
        assertThat response.details,
                   is(equalTo(new ApiManagerDefinition('the-asset-id',
                                                       '1.2.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       'DEV - Automated',
                                                       false)))
    }

    @Test
    void updateApiDefinition() {
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
                end()
            }
        }
        def desiredApiDefinition = ApiManagerDefinition.createWithDefaultLabel('the-asset-id',
                                                                               '1.3.3',
                                                                               'https://some.endpoint',
                                                                               'DEV',
                                                                               false)


        // act
        deployer.updateApiDefinition(new ExistingApiManagerDefinition('1234',
                                                                      desiredApiDefinition))

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis'))
        assertThat method,
                   is(equalTo(HttpMethod.PATCH))
        assertThat envHeader,
                   is(equalTo('def456'))
        assertThat sentPayload,
                   is(equalTo([
                           assetVersion : '1.3.3',
                           instanceLabel: 'DEV - Automated',
                           endpoint     : [
                                   uri                : 'https://some.endpoint',
                                   proxyUri           : null,
                                   muleVersion4OrAbove: false,
                                   isCloudHub         : null
                           ]
                   ]))
    }
}
