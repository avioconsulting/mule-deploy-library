package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.*
import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import com.avioconsulting.mule.deployment.internal.subdeployers.*
import groovy.transform.Canonical
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@SuppressWarnings('GroovyAssignabilityCheck')
class DeployerTest {
    private Deployer deployer
    private List<CloudhubDeploymentRequest> deployedChApps
    private List<CloudhubV2DeploymentRequest> deployedChV2Apps
    private List<RuntimeFabricDeploymentRequest> deployedRtfApps
    private List<OnPremDeploymentRequest> deployedOnPremApps
    private List<DesignCenterSync> designCenterSyncs
    private List<ApiSyncCalls> apiSyncs
    private List<PolicySyncCalls> policySyncCalls
    private boolean failDeployment

    @Canonical
    class DesignCenterSync {
        ApiSpecification apiSpec
        FileBasedAppDeploymentRequest appFileInfo
    }

    @Canonical
    class ApiSyncCalls {
        ApiSpec apiSpec
        String appVersion
    }

    @Canonical
    class PolicySyncCalls {
        ExistingApiSpec apiSpec
        List<Policy> desiredPolicies
    }

    @BeforeEach
    void cleanup() {
        deployedChApps = []
        deployedChV2Apps = []
        deployedRtfApps = []
        deployedOnPremApps = []
        designCenterSyncs = []
        apiSyncs = []
        policySyncCalls = []
        failDeployment = false
        deployer = null
        // "default"
        setupDeployer(DryRunMode.Run)
    }

    private static List<RamlFile> getSimpleRamlFiles() {
        [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1'].join('\n'))
        ]
    }

    def setupDeployer(DryRunMode dryRunMode) {
        def mockCloudHubDeployer = [
                deploy        : { CloudhubDeploymentRequest request ->
                    if (failDeployment) {
                        throw new Exception('something did not work')
                    }
                    deployedChApps << request
                },
                isMule4Request: { CloudhubDeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.getNormalizedAppName().contains('mule4')
                }
        ] as ICloudHubDeployer
        def mockCloudHubV2Deployer = [
                deploy        : { CloudhubV2DeploymentRequest request ->
                    if (failDeployment) {
                        throw new Exception('something did not work')
                    }
                    deployedChApps << request
                },
                isMule4Request: { CloudhubV2DeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.contains('mule4')
                }
        ] as ICloudHubV2Deployer
        def mockRuntimeFabricDeployer = [
                deploy        : { RuntimeFabricDeploymentRequest request ->
                    if (failDeployment) {
                        throw new Exception('something did not work')
                    }
                    deployedChApps << request
                },
                isMule4Request: { RuntimeFabricDeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.contains('mule4')
                }
        ] as IRuntimeFabricDeployer
        def mockOnPremDeployer = [
                deploy        : { OnPremDeploymentRequest request ->
                    deployedOnPremApps << request
                },
                isMule4Request: { OnPremDeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.normalizedAppName.contains('mule4')
                }
        ] as IOnPremDeployer
        def mockDcDeployer = [
                synchronizeDesignCenterFromApp: { ApiSpecification apiSpec,
                                                  FileBasedAppDeploymentRequest appFileInfo ->
                    designCenterSyncs << new DesignCenterSync(apiSpec,
                                                              appFileInfo)
                }
        ] as IDesignCenterDeployer
        def mockApiDeployer = [
                synchronizeApiDefinition: { ApiSpec desiredApiManagerDefinition,
                                            String appVersion ->
                    apiSyncs << new ApiSyncCalls(desiredApiManagerDefinition,
                                                 appVersion)
                    return new ExistingApiSpec('api1234',
                                               'the-asset-id',
                                               '1.2.3',
                                               'https://foo',
                                               'DEV',
                                               'v1',
                                               true)
                },
                getExistingApiDefinition: { ApiSpec desiredApiManagerDefinition ->
                    apiSyncs << new ApiSyncCalls(desiredApiManagerDefinition)
                    return new ExistingApiSpec('api1234',
                            'the-asset-id',
                            '1.2.3',
                            'https://foo',
                            'DEV',
                            'v1',
                            true)
                }
        ] as IApiManagerDeployer
        def mockPolicyDeployer = [
                synchronizePolicies: { ExistingApiSpec apiSpec,
                                       List<Policy> desiredPolicies ->
                    policySyncCalls << new PolicySyncCalls(apiSpec,
                                                           desiredPolicies)
                }
        ] as IPolicyDeployer
        deployer = new Deployer(null,
                                dryRunMode,
                                new TestConsoleLogger(),
                                ['DEV'],
                                null,
                                // shouldn't need this since we mock so much
                                mockCloudHubDeployer,
                                mockCloudHubV2Deployer,
                                mockRuntimeFabricDeployer,
                                mockOnPremDeployer,
                                mockDcDeployer,
                                mockApiDeployer,
                                mockPolicyDeployer)
    }

    @Test
    void deployApplication_fail() {
        // arrange
        failDeployment = true
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3',)
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)

        // act
        def exception = shouldFail {
            deployer.deployApplication(request,
                                       new ApiSpecificationList([apiSpec]))
        }

        // assert
        assertThat exception.cause.message,
                   is(containsString('something did not work'))
    }

    @Test
    void deployApplication_cloudhub_mule_3() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app-mule3', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles,
                                           null,
                                           'the-asset-id',
                                           'https://foo')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]),
                                   [
                                           new Policy('openidconnect-access-token-enforcement',
                                                      '1.2.0',
                                                      [exposeHeaders: false],
                                                      Policy.mulesoftGroupId)
                                   ])

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat apiSyncs.size(),
                   is(equalTo(1))
        apiSyncs[0].with {
            assertThat appVersion,
                       is(equalTo('1.2.3'))
            it.apiSpec.with {
                assertThat it.endpoint,
                           is(equalTo('https://foo'))
                assertThat it.isMule4OrAbove,
                           is(equalTo(false))
                assertThat it.environment,
                           is(equalTo('DEV'))
                assertThat it.instanceLabel,
                           is(equalTo('DEV - Automated'))
                assertThat it.exchangeAssetId,
                           is(equalTo('the-asset-id'))
            }

        }
        assertThat deployedChApps[0].cloudhubAppInfo.properties,
                   is(equalTo([
                           env                                               : 'dev',
                           'auto-discovery.api-id'                           : 'api1234',
                           'crypto.key'                                      : 'theKey',
                           'anypoint.platform.client_id'                     : 'theClientId',
                           'anypoint.platform.client_secret'                 : 'theSecret',
                           'anypoint.platform.config.analytics.agent.enabled': true
                   ]))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        def sync = designCenterSyncs[0]
        assertThat sync.appFileInfo.appVersion,
                   is(equalTo('1.2.3'))
        assertThat sync.apiSpec,
                   is(equalTo(apiSpec))
        assertThat sync.appFileInfo.file,
                   is(equalTo(file))
        assertThat policySyncCalls.size(),
                   is(equalTo(1))
        def policySync = policySyncCalls[0]
        assertThat policySync.apiSpec.id,
                   is(equalTo('api1234'))
        assertThat policySync.desiredPolicies.size(),
                   is(equalTo(1))
    }

    @Test
    void deployApplication_cloudhub_mule_4() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('4.2.2',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app-mule4', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles,
                                           null,
                                           'the-asset-id',
                                           'https://foo')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]))

        // assert
        assertThat apiSyncs.size(),
                   is(equalTo(1))
        apiSyncs[0].with {
            it.apiSpec.with {
                assertThat it.isMule4OrAbove,
                           is(equalTo(true))
            }
        }
    }

    @Test
    void deployApplication_no_dc_deployment_for_tst_environment() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('TST',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]))

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat 'Not deploying to DEV, should not be a DC deployment',
                   designCenterSyncs.size(),
                   is(equalTo(0))
    }

    @Test
    void deployApplication_onprem_mule3() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                                                    new ApplicationName('new-app-mule3', null, null),
                                                  '1.2.3')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]))

        // assert
        assertThat deployedOnPremApps.size(),
                   is(equalTo(1))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        assertThat apiSyncs.size(),
                   is(equalTo(1))
        apiSyncs[0].with {
            it.apiSpec.with {
                assertThat it.isMule4OrAbove,
                           is(equalTo(false))
            }
        }
    }

    @Test
    void deployApplication_onprem_mule4() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)
        def request = new OnPremDeploymentRequest('DEV',
                                                  'clustera',
                                                  file,
                                                  new ApplicationName('new-app-mule4', 'client', 'DEV'),
                                                  '1.2.3')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]))

        // assert
        assertThat deployedOnPremApps.size(),
                   is(equalTo(1))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        assertThat apiSyncs.size(),
                   is(equalTo(1))
        apiSyncs[0].with {
            it.apiSpec.with {
                assertThat it.isMule4OrAbove,
                           is(equalTo(true))
            }
        }
    }

    @Test
    void deployApplication_features_all_no_api_spec() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3')

        // act
        deployer.deployApplication(request,
                                   null)

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat 'null apispec supplied',
                   designCenterSyncs.size(),
                   is(equalTo(0))
        assertThat 'null apispec supplied',
                   apiSyncs.size(),
                   is(equalTo(0))
        assertThat 'No API spec, cannot do policies',
                   policySyncCalls.size(),
                   is(equalTo(0))
    }

    @Test
    void deployApplication_app_deployment_only_dc_enabled() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList(new ApiSpecificationList([apiSpec])),
                                   null,
                                   [Features.DesignCenterSync])

        // assert
        assertThat 'We did not include the feature',
                   deployedChApps.size(),
                   is(equalTo(0))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        assertThat 'no feature supplied',
                   apiSyncs.size(),
                   is(equalTo(1)) // 1 for getExistingApiDefinition that is always called
    }

    @Test
    void deployApplication_design_center_feature_disabled() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello API',
                                           simpleRamlFiles)

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]),
                                   null,
                                   [Features.AppDeployment])

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat 'Feature disabled',
                   designCenterSyncs.size(),
                   is(equalTo(0))
        assertThat 'no feature supplied',
                   apiSyncs.size(),
                   is(equalTo(1)) // 1 for getExistingApiDefinition that is always called
    }

    @Test
    void deployApplication_soap() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec = new ApiSpecification('Hello SOAP API',
                                           'v1')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec]))

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat 'API sync should still happen',
                   apiSyncs.size(),
                   is(equalTo(1))
        assertThat 'If a SOAP API is in use, we should not sync to DC',
                   designCenterSyncs.size(),
                   is(equalTo(0))
    }

    @Test
    void multiple_api_specs() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('4.2.2',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    new ApplicationName('new-app-mule4', 'client', 'DEV'),
                                                    '1.2.3')
        def apiSpec1 = new ApiSpecification('Hello API v1',
                                            simpleRamlFiles)
        def spec2Files = [
                new RamlFile('stuff-v2.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v2'].join('\n'))
        ]
        def apiSpec2 = new ApiSpecification('Hello API v2',
                                            spec2Files,
                                            null,
                                            null,
                                            null,
                                            'otherProp',
                                            null,
                                            '/api_v2')

        // act
        deployer.deployApplication(request,
                                   new ApiSpecificationList([apiSpec1, apiSpec2]))

        // assert
        assertThat apiSyncs.size(),
                   is(equalTo(2))
        apiSyncs[0].with {
            it.apiSpec.with {
                assertThat it.isMule4OrAbove,
                           is(equalTo(true))
            }
        }
    }
}
