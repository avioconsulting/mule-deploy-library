package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.internal.models.ApiQueryResponse
import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ResolvedApiSpec
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Assert
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
        def desiredApiDefinition = new ResolvedApiSpec('the-asset-id',
                                                       '1.2.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       true)

        // act
        def result = deployer.createApiDefinition(desiredApiDefinition)

        // assert
        def expected = new ExistingApiSpec('123',
                                           'the-asset-id',
                                           '1.2.3',
                                           'https://some.endpoint',
                                           'DEV',
                                           true)
        assertThat result,
                   is(equalTo(expected))
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
        def desiredApiDefinition = new ResolvedApiSpec('the-asset-id',
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
        def desiredApiDefinition = new ApiSpec('the-asset-id',
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
        def desiredApiDefinition = new ApiSpec('the-asset-id',
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
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               false)

        // act
        def response = deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat response,
                   is(equalTo(new ExistingApiSpec('1234',
                                                  'the-asset-id',
                                                  '1.2.3',
                                                  'https://some.endpoint',
                                                  'DEV',
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
        def desiredApiDefinition = new ExistingApiSpec('1234',
                                                       'the-asset-id',
                                                       '1.3.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       false)


        // act
        deployer.updateApiDefinition(desiredApiDefinition)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234'))
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

    @Test
    void resolveAssetVersion_queries_properly() {
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
        def desiredApiDefinition = new ResolvedApiSpec('the-asset-id',
                                                       '1.2.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       true)

        // act
        deployer.resolveAssetVersion(desiredApiDefinition)

        // assert
        assertThat url,
                   is(equalTo('/graph/api/v1/graphql'))
        assertThat method,
                   is(equalTo(HttpMethod.POST))
        assertThat auth,
                   is(equalTo('Bearer the token'))
        assertThat sentPayload,
                   is(equalTo([
                           query    : 'query GetAssets($asset_id: String!, $group_id: String!) { assets(asset: {groupId: $group_id, assetId: $asset_id}) { __typename assetId version } }',
                           variables: '{"asset_id":"the-asset-id","group_id":"the-org-id"}'
                   ]))
    }

    @Test
    void resolveAssetVersion_match_older_than_us() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void resolveAssetVersion_exact_match_our_app_version() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void resolveAssetVersion_newer_versions_than_us_exist() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void resolveAssetVersion_all_versions_are_newer() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
