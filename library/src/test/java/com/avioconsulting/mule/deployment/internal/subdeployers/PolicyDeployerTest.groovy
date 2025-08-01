package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpServerRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings('groovy:access')
class PolicyDeployerTest extends BaseTest {
    private PolicyDeployer policyDeployer

    @BeforeEach
    void clean() {
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        policyDeployer = new PolicyDeployer(this.clientWrapper,
                                            this.environmentLocator,
                                            new TestConsoleLogger(),
                                            dryRunMode)
    }

    @Test
    void getExistingPolicies_standard() {
        // arrange
        String url = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = [
                        policies: [
                                [
                                        policyTemplateId: '269608',
                                        order           : 2,
                                        pointcutData    : [
                                                [
                                                        methodRegex     : 'POST',
                                                        uriTemplateRegex: '.*foo'
                                                ],
                                                [
                                                        methodRegex     : 'GET',
                                                        uriTemplateRegex: '.*bar'
                                                ]
                                        ],
                                        policyId        : 654159,
                                        configuration   : [
                                                exposeHeaders: false
                                        ],
                                        template        : [
                                                groupId     : '68ef9520-24e9-4cf2-b2f5-620025690913',
                                                assetId     : 'openidconnect-access-token-enforcement',
                                                assetVersion: '1.2.0'
                                        ]
                                ],
                                [
                                        policyTemplateId: '269608',
                                        order           : 1,
                                        pointcutData    : [
                                                [
                                                        methodRegex     : 'POST',
                                                        uriTemplateRegex: '.*foo'
                                                ]
                                        ],
                                        policyId        : 654160,
                                        configuration   : [
                                                exposeHeaders: false
                                        ],
                                        template        : [
                                                groupId     : '68ef9520-24e9-4cf2-b2f5-620025690913',
                                                assetId     : 'openidconnect-access-token-enforcement',
                                                assetVersion: '1.3.0'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy('openidconnect-access-token-enforcement',
                                              '1.3.0',
                                              [exposeHeaders: false],
                                              Policy.mulesoftGroupId,
                                              [
                                                      new PolicyPathApplication([HttpMethod.POST],
                                                                                '.*foo')
                                              ],
                                              '654160'),
                           new ExistingPolicy('openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
                                              Policy.mulesoftGroupId,
                                              [
                                                      new PolicyPathApplication([HttpMethod.POST],
                                                                                '.*foo'),
                                                      new PolicyPathApplication([HttpMethod.GET],
                                                                                '.*bar')
                                              ],
                                              '654159')
                   ]))
    }

    @Test
    void getExistingPolicies_no_paths() {
        // arrange
        String url = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = [
                        policies: [
                                [
                                        policyTemplateId: '269608',
                                        order           : 2,
                                        pointcutData    : null,
                                        policyId        : 654159,
                                        configuration   : [
                                                exposeHeaders: false
                                        ],
                                        template        : [
                                                groupId     : '68ef9520-24e9-4cf2-b2f5-620025690913',
                                                assetId     : 'openidconnect-access-token-enforcement',
                                                assetVersion: '1.2.0'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy('openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
                                              Policy.mulesoftGroupId,
                                              null,
                                              '654159')
                   ]))
    }

    @Test
    void getExistingPolicies_multiple_methods() {
        // arrange
        String url = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = [
                        policies: [
                                [
                                        policyTemplateId: '269608',
                                        order           : 2,
                                        pointcutData    : [
                                                [
                                                        methodRegex     : 'GET|POST',
                                                        uriTemplateRegex: '.*foo'
                                                ]
                                        ],
                                        policyId        : 654159,
                                        configuration   : [
                                                exposeHeaders: false
                                        ],
                                        template        : [
                                                groupId     : '68ef9520-24e9-4cf2-b2f5-620025690913',
                                                assetId     : 'openidconnect-access-token-enforcement',
                                                assetVersion: '1.2.0'
                                        ]
                                ]
                        ]
                ]
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy('openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
                                              Policy.mulesoftGroupId,
                                              [
                                                      new PolicyPathApplication([
                                                                                        HttpMethod.GET,
                                                                                        HttpMethod.POST
                                                                                ],
                                                                                '.*foo')
                                              ],
                                              '654159')
                   ]))
    }

    @Test
    void createPolicy() {
        // arrange
        String url = null
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policy = new Policy('openidconnect-access-token-enforcement',
                                '1.2.0',
                                [exposeHeaders: false],
                                Policy.mulesoftGroupId,
                                [
                                        new PolicyPathApplication([
                                                                          HttpMethod.GET,
                                                                          HttpMethod.POST
                                                                  ],
                                                                  '.*foo')
                                ])

        // act
        policyDeployer.createPolicy(apiSpec,
                                    policy,
                                    22)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat sentPayload,
                   is(equalTo([
                           configurationData: [
                                   exposeHeaders: false
                           ],
                           pointcutData     : [
                                   [
                                           methodRegex     : 'GET|POST',
                                           uriTemplateRegex: '.*foo'
                                   ]
                           ],
                           order            : 22,
                           groupId          : Policy.mulesoftGroupId,
                           assetId          : 'openidconnect-access-token-enforcement',
                           assetVersion     : '1.2.0'
                   ]))
    }

    @Test
    void createPolicy_no_paths() {
        // arrange
        String url = null
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policy = new Policy('openidconnect-access-token-enforcement',
                                '1.2.0',
                                [exposeHeaders: false],
                                Policy.mulesoftGroupId)

        // act
        policyDeployer.createPolicy(apiSpec,
                                    policy,
                                    22)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat sentPayload,
                   is(equalTo([
                           configurationData: [
                                   exposeHeaders: false
                           ],
                           pointcutData     : null,
                           order            : 22,
                           groupId          : Policy.mulesoftGroupId,
                           assetId          : 'openidconnect-access-token-enforcement',
                           assetVersion     : '1.2.0'
                   ]))
    }

    @Test
    void deletePolicy() {
        // arrange
        String url = null
        String method = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            url = request.uri()
            method = request.method().name()
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policy = new ExistingPolicy('openidconnect-access-token-enforcement',
                                        '1.2.0',
                                        [exposeHeaders: false],
                                        Policy.mulesoftGroupId,
                                        [],
                                        '654159')
        // act
        policyDeployer.deletePolicy(apiSpec,
                                    policy)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies/654159'))
        assertThat method,
                   is(equalTo('DELETE'))
    }

    static def mockPolicyGet(HttpServerRequest request,
                             List<ExistingPolicy> policies) {
        if (request.uri() != '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies' || request.method() != io.vertx.core.http.HttpMethod.GET) {
            return false
        }
        request.response().with {
            statusCode = 200
            putHeader('Content-Type',
                      'application/json')
            def result = [
                    policies: policies.withIndex().collect { ExistingPolicy policy, index ->
                        [
                                policyTemplateId: '269608',
                                order           : index,
                                pointcutData    : policy.policyPathApplications.collect { ppa ->
                                    [
                                            methodRegex     : ppa.httpMethods.collect { r -> r.toString() }.join('|'),
                                            uriTemplateRegex: ppa.regex
                                    ]
                                }.with {
                                    it.any() ? it : null
                                },
                                policyId        : policy.id,
                                configuration   : policy.policyConfiguration,
                                template        : [
                                        groupId     : policy.groupId,
                                        assetId     : policy.assetId,
                                        assetVersion: policy.version
                                ]
                        ]
                    }
            ]
            end(JsonOutput.toJson(result))
        }
        return true
    }

    @Test
    void synchronizePolicies_no_existing() {
        // arrange
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (mockPolicyGet(request,
                              [])) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([
                           'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies',
                           'POST /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'
                   ]))
    }

    @Test
    void synchronizePolicies_existing_already_match() {
        // arrange
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (mockPolicyGet(request,
                              [
                                      new ExistingPolicy('openidconnect-access-token-enforcement',
                                                         '1.2.0',
                                                         [exposeHeaders: false],
                                                         Policy.mulesoftGroupId,
                                                         null,
                                                         '1234')
                              ])) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([
                           'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies',
                   ]))
    }

    @Test
    void synchronizePolicies_existing_add_new_ones() {
        // arrange
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (mockPolicyGet(request,
                              [
                                      new ExistingPolicy('openidconnect-access-token-enforcement',
                                                         '1.2.0',
                                                         [exposeHeaders: false],
                                                         Policy.mulesoftGroupId,
                                                         null,
                                                         '987')
                              ])) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId),
                new Policy('some-other-policy',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([
                           'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies',
                           'DELETE /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies/987',
                           'POST /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies',
                           'POST /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'
                   ]))
    }

    @Test
    void synchronizePolicies_existing_wrong_replace_same_number() {
        // arrange
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (mockPolicyGet(request,
                              [
                                      new ExistingPolicy('openidconnect-access-token-enforcement',
                                                         '1.1.0',
                                                         [exposeHeaders: false],
                                                         Policy.mulesoftGroupId,
                                                         null,
                                                         '987')
                              ])) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([
                           'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies',
                           'DELETE /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies/987',
                           'POST /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'
                   ]))
    }

    @Test
    void synchronizePolicies_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (mockPolicyGet(request,
                              [
                                      new ExistingPolicy('openidconnect-access-token-enforcement',
                                                         '1.2.0',
                                                         [exposeHeaders: false],
                                                         Policy.mulesoftGroupId,
                                                         null,
                                                         '987')
                              ])) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec('1234',
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId),
                new Policy('some-other-policy',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([
                           'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'
                   ]))
    }

    @Test
    void synchronizePolicies_online_validate_api_definition_not_created_yet() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            if (request.uri() == "/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/${ApiManagerDeployer.DRY_RUN_API_ID}/policies") {
                request.response().with {
                    statusCode = 404
                    end()
                }
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def result = 'did it'
                end(JsonOutput.toJson(result))
            }
        }
        def apiSpec = new ExistingApiSpec(ApiManagerDeployer.DRY_RUN_API_ID,
                                          'the-asset-id',
                                          '1.2.3',
                                          'https://foo',
                                          'DEV',
                                          'v1',
                                          true)
        def policies = [
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId),
                new Policy('some-other-policy',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId)
        ]

        // act
        policyDeployer.synchronizePolicies(apiSpec,
                                           policies)

        // assert
        assertThat requests,
                   is(equalTo([]))
    }

    @Test
    void normalizePolicies() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
        }
        def input = [
                // should stay the same
                new Policy('openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false],
                           Policy.mulesoftGroupId),
                new Policy('some-other-policy',
                           '1.2.0',
                           [exposeHeaders: false])
        ]

        // act
        def result = policyDeployer.normalizePolicies(input)

        // assert
        assertThat result,
                   is(equalTo([
                           // should stay the same
                           new Policy('openidconnect-access-token-enforcement',
                                      '1.2.0',
                                      [exposeHeaders: false],
                                      Policy.mulesoftGroupId),
                           new Policy('some-other-policy',
                                      '1.2.0',
                                      [exposeHeaders: false],
                                      'the-org-id')
                   ]))
    }
}
