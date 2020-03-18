package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.api.models.*
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.internal.models.ApiSpec
import com.avioconsulting.mule.deployment.internal.models.ExistingApiSpec
import com.avioconsulting.mule.deployment.internal.subdeployers.*
import groovy.transform.Canonical
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeployerTest {
    private Deployer deployer
    private List<CloudhubDeploymentRequest> deployedChApps
    private List<OnPremDeploymentRequest> deployedOnPremApps
    private List<DesignCenterSync> designCenterSyncs
    private List<ApiSyncCalls> apiSyncs
    private List<PolicySyncCalls> policySyncCalls
    private boolean failDeployment

    @Canonical
    class DesignCenterSync {
        ApiSpecification apiSpec
        FileBasedAppDeploymentRequest appFileInfo
        String appVersion
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

    @Before
    void setupDeployer() {
        deployedChApps = []
        deployedOnPremApps = []
        designCenterSyncs = []
        apiSyncs = []
        policySyncCalls = []
        failDeployment = false
        def mockCloudHubDeployer = [
                deploy        : { CloudhubDeploymentRequest request ->
                    if (failDeployment) {
                        throw new Exception('something did not work')
                    }
                    deployedChApps << request
                },
                isMule4Request: { CloudhubDeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.contains('mule4')
                }
        ] as ICloudHubDeployer
        def mockOnPremDeployer = [
                deploy        : { OnPremDeploymentRequest request ->
                    deployedOnPremApps << request
                },
                isMule4Request: { OnPremDeploymentRequest deploymentRequest ->
                    deploymentRequest.appName.contains('mule4')
                }
        ] as IOnPremDeployer
        def mockDcDeployer = [
                synchronizeDesignCenterFromApp: { ApiSpecification apiSpec,
                                                  FileBasedAppDeploymentRequest appFileInfo,
                                                  String appVersion ->
                    designCenterSyncs << new DesignCenterSync(apiSpec,
                                                              appFileInfo,
                                                              appVersion)
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
                                System.out,
                                ['DEV'],
                                null,
                                // shouldn't need this since we mock so much
                                mockCloudHubDeployer,
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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API')

        // act
        def exception = shouldFail {
            deployer.deployApplication(request,
                                       '1.2.3',
                                       apiSpec)
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
                                                    'new-app-mule3',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API',
                                           'v1',
                                           'main.raml',
                                           'the-asset-id',
                                           'https://foo')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec,
                                   [
                                           new Policy(Policy.mulesoftGroupId,
                                                      'openidconnect-access-token-enforcement',
                                                      '1.2.0',
                                                      [exposeHeaders: false])
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
                           env                              : 'dev',
                           'auto-discovery.api-id'          : 'api1234',
                           'crypto.key'                     : 'theKey',
                           'anypoint.platform.client_id'    : 'theClientId',
                           'anypoint.platform.client_secret': 'theSecret'
                   ]))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        def sync = designCenterSyncs[0]
        assertThat sync.appVersion,
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
    void deployApplication_no_policies() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void deployApplication_cloudhub_mule_4() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app-mule4',
                                                    new CloudhubWorkerSpecRequest('4.2.2',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API',
                                           'v1',
                                           'main.raml',
                                           'the-asset-id',
                                           'https://foo')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec)

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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec)

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
        def apiSpec = new ApiSpecification('Hello API')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app-mule3',
                                                  'clustera',
                                                  file)

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec)

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
        def apiSpec = new ApiSpecification('Hello API')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-ap-mule4',
                                                  'clustera',
                                                  file)

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec)

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
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
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
    }

    @Test
    void deployApplication_app_deployment_only_dc_enabled() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec,
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
                   is(equalTo(0))
    }

    @Test
    void deployApplication_design_center_feature_disabled() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def request = new CloudhubDeploymentRequest('DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')
        def apiSpec = new ApiSpecification('Hello API')

        // act
        deployer.deployApplication(request,
                                   '1.2.3',
                                   apiSpec,
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
                   is(equalTo(0))
    }
}
