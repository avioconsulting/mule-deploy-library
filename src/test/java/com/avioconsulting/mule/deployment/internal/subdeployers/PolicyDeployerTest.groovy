package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingPolicy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpServerRequest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings("GroovyAccessibility")
class PolicyDeployerTest extends BaseTest {
    private PolicyDeployer policyDeployer

    @Before
    void setupDeployer() {
        policyDeployer = new PolicyDeployer(this.clientWrapper,
                                            this.environmentLocator,
                                            System.out)
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
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy(Policy.mulesoftGroupId,
                                              'openidconnect-access-token-enforcement',
                                              '1.3.0',
                                              [exposeHeaders: false],
                                              [
                                                      new PolicyPathApplication([HttpMethod.POST],
                                                                                '.*foo')
                                              ],
                                              '654160'),
                           new ExistingPolicy(Policy.mulesoftGroupId,
                                              'openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
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
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy(Policy.mulesoftGroupId,
                                              'openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
                                              [],
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
                                          true)

        // act
        def results = policyDeployer.getExistingPolicies(apiSpec)

        // assert
        assertThat url,
                   is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy(Policy.mulesoftGroupId,
                                              'openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
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
                                          true)
        def policy = new Policy(Policy.mulesoftGroupId,
                                'openidconnect-access-token-enforcement',
                                '1.2.0',
                                [exposeHeaders: false],
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
                                          true)
        def policy = new Policy(Policy.mulesoftGroupId,
                                'openidconnect-access-token-enforcement',
                                '1.2.0',
                                [exposeHeaders: false])

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
                                          true)
        def policy = new ExistingPolicy(Policy.mulesoftGroupId,
                                        'openidconnect-access-token-enforcement',
                                        '1.2.0',
                                        [exposeHeaders: false],
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

    def mockPolicyGet(HttpServerRequest request,
                      List<ExistingPolicy> policies) {
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
                                          true)
        def policies = [
                new Policy(Policy.mulesoftGroupId,
                           'openidconnect-access-token-enforcement',
                           '1.2.0',
                           [exposeHeaders: false])
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

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void synchronizePolicies_existing_add_new_ones() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void synchronizePolicies_existing_wrong_replace_same_number() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void synchronizePolicies_existing_wrong_replace_and_add_new() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
