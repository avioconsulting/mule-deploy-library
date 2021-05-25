package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
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

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings('GroovyAccessibility')
class ApiManagerDeployerTest extends BaseTest {
    private ApiManagerDeployer deployer

    @Before
    void clean() {
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new ApiManagerDeployer(clientWrapper,
                                          environmentLocator,
                                          new TestConsoleLogger(),
                                          dryRunMode)
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
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            (auth, org, envHeader) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        id            : 123,
                        endpoint      : [
                                uri                : 'https://some.endpoint',
                                muleVersion4OrAbove: true
                        ],
                        assetId       : 'the-asset-id',
                        assetVersion  : '1.2.3',
                        productVersion: 'v1',
                        instanceLabel : 'DEV - Automated'
                ]))
            }
        }
        def desiredApiDefinition = new ResolvedApiSpec('the-asset-id',
                                                       '1.2.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       'v1',
                                                       true)

        // act
        def result = deployer.createApiDefinition(desiredApiDefinition)

        // assert
        def expected = new ExistingApiSpec('123',
                                           'the-asset-id',
                                           '1.2.3',
                                           'https://some.endpoint',
                                           'DEV',
                                           'v1',
                                           true)
        assertThat result,
                   is(equalTo(expected))
        assertThat result.id,
                   is(equalTo('123'))
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis'))
        assertThat method,
                   is(equalTo(HttpMethod.POST))
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
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        id            : 123,
                        endpoint      : [
                                uri                : 'https://some.endpoint',
                                muleVersion4OrAbove: false
                        ],
                        assetId       : 'the-asset-id',
                        assetVersion  : '1.2.3',
                        productVersion: 'v1',
                        instanceLabel : 'DEV - Automated'
                ]))
            }
        }
        def desiredApiDefinition = new ResolvedApiSpec('the-asset-id',
                                                       '1.2.3',
                                                       'https://some.endpoint',
                                                       'DEV',
                                                       'v1',
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
                                     'does not matter',
                                     'v1')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  'v1',
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
                                     'does not matter',
                                     'v1'),
                new ApiQueryResponse('4567',
                                     'the label',
                                     'v1')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  'v1',
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
                                     'does not matter',
                                     'v1'),
                new ApiQueryResponse('4567',
                                     'does not matter 2',
                                     'v1')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  'v1',
                                                  responses)

        // assert
        assertThat result,
                   is(equalTo(responses[0]))
    }

    @Test
    void chooseApiDefinition_multiple_versions_found() {
        // arrange
        def responses = [
                new ApiQueryResponse('8989',
                                     'the label',
                                     'v2'),
                new ApiQueryResponse('4567',
                                     'the label',
                                     'v1')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  'v1',
                                                  responses)

        // assert
        assertThat result,
                   is(equalTo(responses[1]))
    }

    @Test
    void chooseApiDefinition_multiple_versions_not_found() {
        // arrange
        def responses = [
                new ApiQueryResponse('8989',
                                     'the label',
                                     'v2'),
                new ApiQueryResponse('4567',
                                     'the label',
                                     'v2')
        ]

        // act
        def result = deployer.chooseApiDefinition('the label',
                                                  'v1',
                                                  responses)

        // assert
        assertThat result,
                   is(nullValue())
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
                                               'v1',
                                               false)

        // act
        deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id'))
        assertThat method,
                   is(equalTo(HttpMethod.GET))
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
                                               'v1',
                                               false)

        // act
        def response = deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat response,
                   is(nullValue())
    }

    @Test
    void getExistingApiDefinition_found_only_1_version_available() {
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
                Map responseMap
                if (request.uri() == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.GET) {
                    responseMap = [
                            id            : 1234,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false,
                                    unmapped_field_here: 'hello'
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.2.3',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated',
                            // should not disrupt anything
                            unmapped_field: 'hi'
                    ]
                } else {
                    // should not disrupt anything
                    def queryResponse = new ObjectMapper().convertValue(new ApiQueryResponse('1234',
                                                                                             'does not matter',
                                                                                             'v1'),
                                                                        Map)
                    queryResponse['unmapped_field_2'] = 'hi'
                    responseMap = [
                            total           : 1,
                            unmapped_field_3: 'hi',
                            assets          : [
                                    [
                                            apis            : [
                                                    queryResponse
                                            ],
                                            unmapped_field_4: 'hi'
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
                                               'v1',
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
                                                  'v1',
                                                  false)))
    }

    @Test
    void getExistingApiDefinition_only_different_versions_exist() {
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
                def queryResponse = new ObjectMapper().convertValue(new ApiQueryResponse('1234',
                                                                                         'does not matter',
                                                                                         'v1'),
                                                                    Map)
                queryResponse['unmapped_field_2'] = 'hi'
                def responseMap = [
                        total           : 1,
                        unmapped_field_3: 'hi',
                        assets          : [
                                [
                                        apis            : [
                                                queryResponse
                                        ],
                                        unmapped_field_4: 'hi'
                                ]
                        ]
                ]
                end(new ObjectMapper().writeValueAsString(responseMap))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v2',
                                               false)

        // act
        def response = deployer.getExistingApiDefinition(desiredApiDefinition)

        // assert
        assertThat response,
                   is(nullValue())
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
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
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
                                                       'v1',
                                                       false)


        // act
        deployer.updateApiDefinition(desiredApiDefinition)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234'))
        assertThat method,
                   is(equalTo(HttpMethod.PATCH))
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
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            (auth, org, envHeader) = capturedStandardHeaders(request)
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'

                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910213',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        deployer.resolveAssetVersion(desiredApiDefinition,
                                     '1.0.201910213')

        // assert
        assertThat url,
                   is(equalTo('/graph/api/v1/graphql'))
        assertThat method,
                   is(equalTo(HttpMethod.POST))
        assertThat auth,
                   is(equalTo('Bearer the token'))
        assertThat sentPayload,
                   is(equalTo([
                           query    : 'query GetAssets($asset_id: String!, $group_id: String!) { assets(asset: {groupId: $group_id, assetId: $asset_id}) { __typename assetId version versionGroup } }',
                           variables: '{"asset_id":"the-asset-id","group_id":"the-org-id"}'
                   ]))
    }

    @Test
    void resolveAssetVersion_not_found() {
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
                def response = [
                        data: null
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def exception = shouldFail {
            deployer.resolveAssetVersion(desiredApiDefinition,
                                         '1.0.202010213')
        }

        // assert
        assertThat exception.message,
                   is(containsString('Expected to find a v1 asset version <= our app version of 1.0.202010213 but did not! Asset versions found in Exchange were []'))
    }

    @Test
    void resolveAssetVersion_v1_there_but_not_v2() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'

                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910213',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v2',
                                               true)

        // act
        def exception = shouldFail {
            deployer.resolveAssetVersion(desiredApiDefinition,
                                         '2.0.202010213')
        }

        // assert
        assertThat exception.message,
                   is(containsString('Expected to find a v2 asset version <= our app version of 2.0.202010213 but did not! Asset versions found in Exchange were [1.0.201910193, 1.0.201910213]'))
    }

    @Test
    void resolveAssetVersion_both_versions_exist_v1_being_deployed_with_2_0_app_version() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'

                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202010213',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202110213',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '2.0.201911213',
                                                versionGroup: 'v2'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.resolveAssetVersion(desiredApiDefinition,
                                                  '2.0.202010213')

        // assert
        assertThat '1.0.202010213 is the latest v1 version <= to our app version',
                   result,
                   is(equalTo(new ResolvedApiSpec('the-asset-id',
                                                  '1.0.202010213',
                                                  'https://some.endpoint',
                                                  'DEV',
                                                  'v1',
                                                  true)))
    }

    @Test
    void resolveAssetVersion_not_found_dry_run() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
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
                def response = [
                        data: null
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.resolveAssetVersion(desiredApiDefinition,
                                                  '1.0.202010213')

        // assert
        assertThat result,
                   is(equalTo(new ResolvedApiSpec('the-asset-id',
                                                  ApiManagerDeployer.DRY_RUN_API_ID,
                                                  'https://some.endpoint',
                                                  'DEV',
                                                  'v1',
                                                  true)))
    }

    @Test
    void resolveAssetVersion_match_older_than_us() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '2.0.201910313',
                                                versionGroup: 'v2'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910213',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.resolveAssetVersion(desiredApiDefinition,
                                                  '1.0.202010213')

        // assert
        assertThat result,
                   is(equalTo(new ResolvedApiSpec('the-asset-id',
                                                  '1.0.201910213',
                                                  'https://some.endpoint',
                                                  'DEV',
                                                  'v1',
                                                  true)))
    }

    @Test
    void resolveAssetVersion_exact_match_our_app_version() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202010213',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.resolveAssetVersion(desiredApiDefinition,
                                                  '1.0.202010213')

        // assert
        assertThat result,
                   is(equalTo(new ResolvedApiSpec('the-asset-id',
                                                  '1.0.202010213',
                                                  'https://some.endpoint',
                                                  'DEV',
                                                  'v1',
                                                  true)))
    }

    @Test
    void resolveAssetVersion_newer_versions_than_us_exist() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.201910193',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202010203',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202011213',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.resolveAssetVersion(desiredApiDefinition,
                                                  '1.0.202010213')

        // assert
        assertThat result,
                   is(equalTo(new ResolvedApiSpec('the-asset-id',
                                                  '1.0.202010203',
                                                  'https://some.endpoint',
                                                  'DEV',
                                                  'v1',
                                                  true)))
    }

    @Test
    void resolveAssetVersion_all_versions_are_newer() {
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
                def response = [
                        data: [
                                assets: [
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202510193',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202510203',
                                                versionGroup: 'v1'
                                        ],
                                        [
                                                '__typename': 'Asset',
                                                assetId     : 'foo',
                                                version     : '1.0.202511213',
                                                versionGroup: 'v1'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def exception = shouldFail {
            deployer.resolveAssetVersion(desiredApiDefinition,
                                         '1.0.202010213')
        }

        // assert
        assertThat exception.message,
                   is(containsString('Expected to find a v1 asset version <= our app version of 1.0.202010213 but did not! Asset versions found in Exchange were [1.0.202510193, 1.0.202510203, 1.0.202511213]'))
    }

    @Test
    void synchronizeApiDefinition_does_not_exist() {
        // arrange
        def created = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                Map response
                def uri = request.uri()
                if (uri.contains('graphql')) {
                    statusCode = 200
                    response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ],
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.202010213',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id') {
                    statusCode = 200
                    response = [
                            total : 0,
                            assets: []
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis' && request.method() == HttpMethod.POST) {
                    created = true
                    statusCode = 200
                    response = [
                            id            : 123,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: true
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.2.3',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated'
                    ]
                } else {
                    statusCode = 500
                    response = 'Unknown request!'
                }
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.synchronizeApiDefinition(desiredApiDefinition,
                                                       '1.0.202010213')

        // assert
        assertThat result.id,
                   is(equalTo('123'))
        assertThat created,
                   is(equalTo(true))
    }

    @Test
    void synchronizeApiDefinition_already_exists_no_changes() {
        // arrange
        def created = false
        def updated = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                Map response
                def uri = request.uri()
                if (uri.contains('graphql')) {
                    statusCode = 200
                    response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ],
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.202010213',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.GET) {
                    statusCode = 200
                    response = [
                            id            : 1234,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.0.202010213',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id') {
                    statusCode = 200
                    response = [
                            total : 1,
                            assets: [
                                    [
                                            apis: [
                                                    new ApiQueryResponse('1234',
                                                                         'does not matter',
                                                                         'v1')
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis' && request.method() == HttpMethod.POST) {
                    statusCode = 200
                    created = true
                    response = [
                            id           : 1234,
                            endpoint     : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId      : 'the-asset-id',
                            assetVersion : '1.0.202010213',
                            instanceLabel: 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.PATCH) {
                    statusCode = 200
                    updated = true
                    response = [:]
                } else {
                    statusCode = 500
                    response = 'Unexpected request'
                }
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               false)

        // act
        def result = deployer.synchronizeApiDefinition(desiredApiDefinition,
                                                       '1.0.202010213')

        // assert
        assertThat result.id,
                   is(equalTo('1234'))
        assertThat 'Already exists',
                   created,
                   is(equalTo(false))
        assertThat 'Already correct',
                   updated,
                   is(equalTo(false))
    }

    @Test
    void synchronizeApiDefinition_already_exists_version_wrong() {
        // arrange
        def created = false
        def updated = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def response
                def uri = request.uri()
                if (uri.contains('graphql')) {
                    statusCode = 200
                    response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ],
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.202010213',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.GET) {
                    statusCode = 200
                    response = [
                            id            : 1234,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.0.202010213',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id') {
                    statusCode = 200
                    response = [
                            total : 1,
                            assets: [
                                    [
                                            apis: [
                                                    new ApiQueryResponse('1234',
                                                                         'does not matter',
                                                                         'v1')
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis' && request.method() == HttpMethod.POST) {
                    statusCode = 200
                    created = true
                    response = [
                            id           : 1234,
                            endpoint     : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId      : 'the-asset-id',
                            assetVersion : '1.0.202010213',
                            instanceLabel: 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.PATCH) {
                    statusCode = 200
                    updated = true
                    response = [:]
                } else {
                    statusCode = 500
                    response = 'Unexpected request'
                }
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               false)

        // act
        def result = deployer.synchronizeApiDefinition(desiredApiDefinition,
                                                       '1.0.202010113')

        // assert
        assertThat result.id,
                   is(equalTo('1234'))
        assertThat 'Already exists',
                   created,
                   is(equalTo(false))
        assertThat 'Version was wrong',
                   updated,
                   is(equalTo(true))
    }

    @Test
    void synchronizeApiDefinition_already_ismule4_wrong() {
        // arrange
        def created = false
        def updated = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                def response
                def uri = request.uri()
                if (uri.contains('graphql')) {
                    statusCode = 200
                    response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ],
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.202010213',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.GET) {
                    statusCode = 200
                    response = [
                            id            : 1234,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.0.202010213',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id') {
                    statusCode = 200
                    response = [
                            total : 1,
                            assets: [
                                    [
                                            apis: [
                                                    new ApiQueryResponse('1234',
                                                                         'does not matter',
                                                                         'v1')
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis' && request.method() == HttpMethod.POST) {
                    statusCode = 200
                    created = true
                    response = [
                            id           : 1234,
                            endpoint     : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: false
                            ],
                            assetId      : 'the-asset-id',
                            assetVersion : '1.0.202010213',
                            instanceLabel: 'DEV - Automated'
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234' && request.method() == HttpMethod.PATCH) {
                    statusCode = 200
                    updated = true
                    response = [:]
                } else {
                    statusCode = 500
                    response = 'Unexpected request'
                }
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.synchronizeApiDefinition(desiredApiDefinition,
                                                       '1.0.202010213')

        // assert
        assertThat result.id,
                   is(equalTo('1234'))
        assertThat 'Already exists',
                   created,
                   is(equalTo(false))
        assertThat 'Existing version is mule 4 false',
                   updated,
                   is(equalTo(true))
    }

    @Test
    void synchronizeApiDefinition_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def created = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            request.response().with {
                putHeader('Content-Type',
                          'application/json')
                Map response
                def uri = request.uri()
                if (uri.contains('graphql')) {
                    statusCode = 200
                    response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ],
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.202010213',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis?assetId=the-asset-id') {
                    statusCode = 200
                    response = [
                            total : 0,
                            assets: []
                    ]
                } else if (uri == '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis' && request.method() == HttpMethod.POST) {
                    created = true
                    statusCode = 200
                    response = [
                            id            : 123,
                            endpoint      : [
                                    uri                : 'https://some.endpoint',
                                    muleVersion4OrAbove: true
                            ],
                            assetId       : 'the-asset-id',
                            assetVersion  : '1.2.3',
                            productVersion: 'v1',
                            instanceLabel : 'DEV - Automated'
                    ]
                } else {
                    statusCode = 500
                    response = 'Unknown request!'
                }
                end(JsonOutput.toJson(response))
            }
        }
        def desiredApiDefinition = new ApiSpec('the-asset-id',
                                               'https://some.endpoint',
                                               'DEV',
                                               'v1',
                                               true)

        // act
        def result = deployer.synchronizeApiDefinition(desiredApiDefinition,
                                                       '1.0.202010213')

        // assert
        assertThat created,
                   is(equalTo(false))
        assertThat result.id,
                   is(equalTo('******'))
    }
}
