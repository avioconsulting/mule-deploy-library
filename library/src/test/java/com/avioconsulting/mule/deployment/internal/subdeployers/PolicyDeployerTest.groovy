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
                    is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false'))
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
                                               '654160', false, 1),
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
                                               '654159', false, 2)
                   ]))
    }

    @Test
    void getExistingPolicies_flat_response_shapes_include_disabled_state() {
        def rows = [[id: 7174311, groupId: Policy.mulesoftGroupId, assetId: 'client-id-enforcement',
                     assetVersion: '1.3.2', configurationData: [:], pointcutData: null, order: 1, disabled: true]]
        def response = rows
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request) || mockEnvironments(request)) return
            requests << "${request.method().name()} ${request.uri()}".toString()
            request.response().setStatusCode(200).putHeader('Content-Type', 'application/json').end(JsonOutput.toJson(response))
        }
        def spec = new ExistingApiSpec('1234', 'asset', '1.0.0', 'https://foo', 'DEV', 'v1', true)
        [rows, [value: rows], [policies: rows]].each { shape ->
            response = shape
            def policies = policyDeployer.getExistingPolicies(spec)
            assert policies.size() == 1
            assert policies[0].id == '7174311'
            assert policies[0].disabled
            assert policies[0].order == 1
            assert policies[0].policyConfiguration == [:]
        }
        response = [unexpected: []]
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException) {
            policyDeployer.synchronizePolicies(spec, [])
        }
        response = [policies: [[assetId: 'missing-identity']]]
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException) {
            policyDeployer.synchronizePolicies(spec, [])
        }
        assert requests.every { it == 'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false' }
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
                    is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false'))
        assertThat results,
                   is(equalTo([
                           new ExistingPolicy('openidconnect-access-token-enforcement',
                                              '1.2.0',
                                              [exposeHeaders: false],
                                              Policy.mulesoftGroupId,
                                              null,
                                               '654159', false, 2)
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
                    is(equalTo('/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false'))
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
                                               '654159', false, 2)
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
        if (request.uri() != '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false' || request.method() != io.vertx.core.http.HttpMethod.GET) {
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
                                order           : policy.order != null ? policy.order : index + 1,
                                pointcutData    : policy.policyPathApplications.collect { ppa ->
                                    [
                                            methodRegex     : ppa.httpMethods.collect { r -> r.toString() }.join('|'),
                                            uriTemplateRegex: ppa.regex
                                    ]
                                }.with {
                                    it.any() ? it : null
                                },
                                id              : policy.id,
                                disabled        : policy.disabled,
                                configurationData: policy.policyConfiguration,
                                groupId         : policy.groupId,
                                assetId         : policy.assetId,
                                assetVersion    : policy.version
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
                            'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false',
                           'POST /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies'
                   ]))
    }

    private void verifyInPlaceSynchronization(boolean disabled, boolean changed, DryRunMode mode) {
        setupDeployer(mode)
        def mutations = []
        def paths = [new PolicyPathApplication([HttpMethod.POST], '/desired/.*')]
        def desired = new Policy('client-id-enforcement', '1.3.2', [setting: 'desired'], Policy.mulesoftGroupId, paths)
        def state = [new ExistingPolicy(desired.assetId, desired.version,
                changed ? [setting: 'old'] : desired.policyConfiguration, desired.groupId,
                changed ? null : paths, '7174311', disabled, changed ? 4 : 1)]
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request) || mockEnvironments(request) || mockPolicyGet(request, state)) return
            request.bodyHandler { body ->
                def payload = new JsonSlurper().parseText(body.toString())
                mutations << [method: request.method().name(), uri: request.uri(), payload: payload]
                state[0] = new ExistingPolicy(desired.assetId, desired.version, payload.configurationData as Map,
                        desired.groupId, paths, '7174311', payload.disabled as boolean, payload.order as Integer)
                request.response().setStatusCode(200).putHeader('Content-Type', 'application/json').end('{}')
            }
        }
        def apiSpec = new ExistingApiSpec('1234', 'the-asset-id', '1.2.3', 'https://foo', 'DEV', 'v1', true)
        policyDeployer.synchronizePolicies(apiSpec, [desired])
        // A repeated run must not enable again, recreate, or update an already synchronized policy.
        policyDeployer.synchronizePolicies(apiSpec, [desired])
        if (mode != DryRunMode.Run) {
            assert mutations.empty
        } else {
            assert mutations == [[method: 'PATCH',
                    uri: '/apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies/7174311',
                    payload: [configurationData: [setting: 'desired'],
                              pointcutData: [[methodRegex: 'POST', uriTemplateRegex: '/desired/.*']],
                              order: 1, disabled: false]]]
        }
    }

    @Test
    void synchronizePolicies_enables_disabled_matching_policy_in_place() {
        verifyInPlaceSynchronization(true, false, DryRunMode.Run)
    }

    @Test
    void synchronizePolicies_enables_and_syncs_disabled_policy_settings_paths_and_order() {
        verifyInPlaceSynchronization(true, true, DryRunMode.Run)
    }

    @Test
    void synchronizePolicies_updates_enabled_policy_in_place() {
        verifyInPlaceSynchronization(false, true, DryRunMode.Run)
    }

    @Test
    void synchronizePolicies_disabled_policy_dry_run_does_not_mutate() {
        verifyInPlaceSynchronization(true, true, DryRunMode.OnlineValidate)
    }

    @Test
    void synchronizePolicies_duplicate_gav_reserves_exact_matches_and_updates_order() {
        def a = new Policy('policy', '1.0.0', [setting: 'a'], Policy.mulesoftGroupId)
        def b = new Policy('policy', '1.0.0', [setting: 'b'], Policy.mulesoftGroupId)
        def existing = [
                new ExistingPolicy(a.assetId, a.version, a.policyConfiguration, a.groupId, null, 'a', false, 1),
                new ExistingPolicy(b.assetId, b.version, b.policyConfiguration, b.groupId, null, 'b', true, 2)]
        def mutations = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request) || mockEnvironments(request) || mockPolicyGet(request, existing)) return
            request.bodyHandler { body ->
                mutations << [method: request.method().name(), id: request.path().tokenize('/').last(),
                              payload: new JsonSlurper().parseText(body.toString())]
                request.response().setStatusCode(200).putHeader('Content-Type', 'application/json').end('{}')
            }
        }
        def spec = new ExistingApiSpec('1234', 'asset', '1.0.0', 'https://foo', 'DEV', 'v1', true)
        // Reserve the exact instance for the second desired policy before matching the changed one.
        def changed = new Policy(a.assetId, a.version, [setting: 'changed'], a.groupId)
        policyDeployer.synchronizePolicies(spec, [changed, a])
        assert mutations*.method == ['PATCH', 'PATCH']
        assert mutations*.id == ['b', 'a']
        assert mutations*.payload == [
                [configurationData: [setting: 'changed'], pointcutData: null, order: 1, disabled: false],
                [configurationData: [setting: 'a'], pointcutData: null, order: 2, disabled: false]]
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
                            'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false',
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
                            'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false',
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
                            'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false',
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
                            'GET /apimanager/api/v1/organizations/the-org-id/environments/def456/apis/1234/policies?fullInfo=false'
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
