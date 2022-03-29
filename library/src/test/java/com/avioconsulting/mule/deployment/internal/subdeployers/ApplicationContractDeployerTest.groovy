package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.RequestedContract
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

@SuppressWarnings('GroovyAccessibility')
class ApplicationContractDeployerTest extends BaseTest {
    private ApplicationContractDeployer applicationContractDeployer

    @Before
    void clean() {
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        applicationContractDeployer = new ApplicationContractDeployer(this.clientWrapper,
                this.environmentLocator,
                new TestConsoleLogger(),
                dryRunMode)
    }

    @Test
    void collectCurrentContracts_standard() {
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
                def result = buildClientApplicationsContractsGet()
                end(JsonOutput.toJson(result))
            }
        }
        // act
        def results = applicationContractDeployer.collectCurrentContracts(123)
        assertThat results.size(), is(equalTo(1))
        assertThat url, is(equalTo('/apiplatform/repository/v2/organizations/the-org-id/applications/123/contracts'))
    }

    @Test
    void findOurClientAppId_standard() {
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
                def result = null
                if (isClientApplicationsQueryGet(request) && request.getParam("query").contains("test-client"))
                    result = buildClientApplicationsQueryGet()
                else
                    result = [
                            applications: [],
                            total       : 0
                    ]
                end(JsonOutput.toJson(result))
            }
        }
        // act
        def result = applicationContractDeployer.findOurClientAppId('test-client')
        assertThat result, is(equalTo(123))
        assertThat url, is(equalTo('/apiplatform/repository/v2/organizations/the-org-id/applications?targetAdminSite=true&query=test-client'))
    }

    @Test
    void findOurClientAppId_not_found() {
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
                def result = null
                if (isClientApplicationsQueryGet(request) && request.getParam("query").contains("test-client"))
                    result = buildClientApplicationsQueryGet()
                else
                    result = [
                            applications: [],
                            total       : 0
                    ]
                end(JsonOutput.toJson(result))
            }
        }
        // act
        def result = applicationContractDeployer.findOurClientAppId('unknown-client')
        assertThat result, is(nullValue())
        assertThat url, is(equalTo('/apiplatform/repository/v2/organizations/the-org-id/applications?targetAdminSite=true&query=unknown-client'))
    }

    @Test
    void findApiManagerInstances_standard() {
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
                def result = null
                if (isApiManagerInstancesGet(request))
                    result = buildApiManagerInstancesGet()
                end(JsonOutput.toJson(result))
            }
        }
        def result = applicationContractDeployer.findApiManagerId("target-api", 'v1', 'abc-123')
        assertThat result, is(123)
        assertThat url, is(equalTo('/exchange/api/v2/assets/the-org-id/target-api/versionGroups/v1/instances'))
    }

    @Test
    void synchronizeApplicationContracts_standard() {
        def requests = []
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockEnvironments(request)) {
                return
            }
            requests << "${request.method().name()} ${request.uri()}".toString()
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                        'application/json')
                def result = 'Default response'
                if (isClientApplicationsQueryGet(request)) {
                    result = buildClientApplicationsQueryGet()
                }
                else if (isClientApplicationsContractsGet(request)) {
                    result = buildClientApplicationsContractsGet()
                }
                else if (isExchangeVersionInformationGet(request)) {
                    result = buildExchangeVersionInformationGet()
                }
                else if (isApiManagerInstancesGet(request)) {
                    result = buildApiManagerInstancesGet()
                }
                else if (isContractPost(request)) {
                    result = buildContractPost()
                }
                end(JsonOutput.toJson(result))
            }
        }
        // act
        def apiSpec = new ExistingApiSpec('1234',
                'the-asset-id',
                '1.2.3',
                'https://foo',
                'DEV',
                'v1',
                true)
        def clientApplicationName = 'test-client'
        def requestedContracts = [
                new RequestedContract(clientApplicationName, 'target-api'),
                new RequestedContract(clientApplicationName, 'target-two-api')
        ]
        applicationContractDeployer.synchronizeApplicationContracts(apiSpec, requestedContracts)
        assertThat requests, is(equalTo([
                "GET /apiplatform/repository/v2/organizations/the-org-id/applications?targetAdminSite=true&query=test-client",
                "GET /apiplatform/repository/v2/organizations/the-org-id/applications/123/contracts",
                "GET /exchange/api/v2/assets/the-org-id/target-two-api/asset",
                "GET /exchange/api/v2/assets/the-org-id/target-two-api/versionGroups/v1/instances",
                "POST /exchange/api/v2/organizations/the-org-id/applications/123/contracts"
        ]))
    }

    static boolean isClientApplicationsQueryGet(HttpServerRequest httpServerRequest) {
        return (httpServerRequest.uri().matches(/\/apiplatform\/repository\/v2\/organizations\/.+\/applications\?targetAdminSite=true&query=.+/)
                && httpServerRequest.method() == HttpMethod.GET)
    }

    static buildClientApplicationsQueryGet(Integer clientApplicationId = 123, String clientApplicationName = "test-client") {
        [
            total: 2,
            applications: [[
                    "audit"               : [
                            "created": [
                                    "date": ""
                            ],
                            "updated": []
                    ],
                    "masterOrganizationId": "",
                    "id"                  : clientApplicationId,
                    "name"                : clientApplicationName,
                    "description"         : null,
                    "coreServicesId"      : "",
                    "url"                 : null,
                    "clientId"            : "",
                    "clientSecret"        : "",
                    "grantTypes"          : [],
                    "redirectUri"         : [],
                    "owner"               : "",
                    "email"               : "",
                    "owners"              : [
                            [
                                    "id"                     : "",
                                    "createdAt"              : "",
                                    "updatedAt"              : "",
                                    "organizationId"         : "my-testing-group",
                                    "firstName"              : "",
                                    "lastName"               : "",
                                    "email"                  : "",
                                    "phoneNumber"            : "",
                                    "username"               : "",
                                    "idprovider_id"          : "",
                                    "enabled"                : true,
                                    "deleted"                : false,
                                    "lastLogin"              : "",
                                    "mfaVerificationExcluded": false,
                                    "mfaVerifiersConfigured" : "false",
                                    "isFederated"            : false,
                                    "type"                   : "",
                                    "roles"                  : [
                                            ""
                                    ]
                            ]
                    ]
            ]]
        ]
    }

    static boolean isClientApplicationsContractsGet(HttpServerRequest httpServerRequest) {
        return (httpServerRequest.uri().matches(/\/apiplatform\/repository\/v2\/organizations\/.+\/applications\/.+\/contracts/)
                && httpServerRequest.method() == HttpMethod.GET)
    }

    static buildClientApplicationsContractsGet(Integer contractId = 123, String exchangeAssetId = "target-api", String exchangeAssetName = "Target API") {
        [
                [
                        audit         : [
                                created: [
                                        date: ""
                                ],
                                updated: []
                        ],
                        organizationId: "my-testing-group",
                        id            : contractId,
                        status        : "",
                        tier          : null,
                        requestedTier : null,
                        apiVersion    : [
                                audit         : [
                                        created: [],
                                        updated: []
                                ],
                                organizationId: "my-testing-group",
                                id            : 2222222,
                                apiId         : 3333333,
                                name          : "v1:2222222",
                                instanceLabel : "Development - Automated",
                                productVersion: "v1",
                                deprecated    : false,
                                environmentId : "",
                                type          : "UNMANAGED",
                                api           : [
                                        audit            : [
                                                created: [],
                                                updated: []
                                        ],
                                        organizationId   : "",
                                        id               : 4444444,
                                        name             : "groupId:my-testing-group:assetId:${exchangeAssetId}",
                                        exchangeAssetName: exchangeAssetName
                                ]
                        ],
                        groupInstance : null
                ]
        ]
    }

    static boolean isExchangeVersionInformationGet(HttpServerRequest httpServerRequest) {
        return (httpServerRequest.uri().matches(/\/exchange\/api\/v2\/assets\/.+\/.+\/asset/)
                && httpServerRequest.method() == HttpMethod.GET)
    }

    static buildExchangeVersionInformationGet(String version = "1.0.0", String versionGroup = "v1", String exchangeAssetName = "target-api", String exchangeDisplayName = "Order Management API") {
        [
                "groupId"       : "",
                "assetId"       : exchangeAssetName,
                "version"       : version,
                "minorVersion"  : "1.0",
                "organizationId": "",
                "description"   : "",
                "versionGroup"  : versionGroup,
                "isPublic"      : false,
                "name"          : exchangeDisplayName,
                "type"          : "rest-api",
                "status"        : "published",
                "contactEmail"  : null,
                "contactName"   : null,
                "labels"        : [],
                "attributes"    : [],
                "categories"    : [],
                "customFields"  : [],
                "files"         : [],
                "dependencies"  : [
                        [
                                "organizationId": "",
                                "groupId"       : "",
                                "assetId"       : "common-services-library",
                                "version"       : "1.0.1"
                        ]
                ],
                "createdAt"     : "",
                "createdById"   : "",
                "versions"      : []
        ]
    }

    static boolean isApiManagerInstancesGet(HttpServerRequest httpServerRequest) {
        return (httpServerRequest.uri().matches(/\/exchange\/api\/v2\/assets\/.+\/.+\/versionGroups\/.+\/instances/)
                && httpServerRequest.method() == HttpMethod.GET)
    }

    static buildApiManagerInstancesGet(Integer apiManagerInstanceId = 123, String instanceName = "UnitTest - Automated", String exchangeAssetName = "target-api", String environmentId = 'abc-123') {
        [
                [
                        "organizationId": "",
                        "groupId": "",
                        "assetId": exchangeAssetName,
                        "productApiVersion": "v1",
                        "type": "managed",
                        "deprecated": false,
                        "endpointUri": null,
                        "environmentId": environmentId,
                        "isPublic": false,
                        "providerId": null,
                        "contractsCount": 0,
                        "version": "1.0.0",
                        "status": "active",
                        "name": instanceName,
                        "id": apiManagerInstanceId
                ]
        ]
    }

    static boolean isContractPost(HttpServerRequest httpServerRequest) {
        return (httpServerRequest.uri().matches(/\/exchange\/api\/v2\/organizations\/.+\/applications\/.+\/contracts/)
                && httpServerRequest.method() == HttpMethod.POST)
    }

    static buildContractPost(Integer contractId = 123, Integer clientApplicationId = 456, Integer apiManagerId = 789) {
        [
            "id": contractId,
            "status": "APPROVED",
            "applicationId": clientApplicationId,
            "clientId": "",
            "clientSecret": "",
            "api": [
                "version": "",
                "minorVersion": "",
                "organizationId": "",
                "id": apiManagerId,
                "deprecated": false,
                "groupId": "",
                "assetId": "",
                "productVersion": "",
                "environmentId": "",
                "assetVersion": "",
                "fullname": "",
                "environmentName": "",
                "environmentOrganizationName": "",
                "assetName": ""
            ]
        ]
    }
}