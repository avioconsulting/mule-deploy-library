package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.api.Deployer
import com.avioconsulting.mule.deployment.models.*
import com.avioconsulting.mule.deployment.subdeployers.ICloudHubDeployer
import com.avioconsulting.mule.deployment.subdeployers.IDesignCenterDeployer
import com.avioconsulting.mule.deployment.subdeployers.IOnPremDeployer
import groovy.transform.Canonical
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
    private boolean failDeployment

    @Canonical
    class DesignCenterSync {
        ApiSpecification apiSpec
        FileBasedAppDeploymentRequest appFileInfo
        String appVersion
    }

    @Before
    void setupDeployer() {
        deployedChApps = []
        deployedOnPremApps = []
        designCenterSyncs = []
        failDeployment = false
        def mockCloudHubDeployer = [
                deploy: { CloudhubDeploymentRequest request ->
                    if (failDeployment) {
                        throw new Exception('something did not work')
                    }
                    deployedChApps << request
                }
        ] as ICloudHubDeployer
        def mockOnPremDeployer = [
                deploy: { OnPremDeploymentRequest request ->
                    deployedOnPremApps << request
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
        deployer = new Deployer(null,
                                System.out,
                                ['DEV'],
                                null,
                                // shouldn't need this since we mock so much
                                mockCloudHubDeployer,
                                mockOnPremDeployer,
                                mockDcDeployer)
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
    void deployApplication_cloudhub() {
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
                                   apiSpec)

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
        def sync = designCenterSyncs[0]
        assertThat sync.appVersion,
                   is(equalTo('1.2.3'))
        assertThat sync.apiSpec,
                   is(equalTo(apiSpec))
        assertThat sync.appFileInfo.file,
                   is(equalTo(file))
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
    void deployApplication_onprem() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def apiSpec = new ApiSpecification('Hello API')
        def request = new OnPremDeploymentRequest('DEV',
                                                  'new-app',
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
    }

    @Test
    void deployApplication_app_deployment_feature_disabled() {
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
                                   [Features.DesignCenterSync])

        // assert
        assertThat 'We did not include the feature',
                   deployedChApps.size(),
                   is(equalTo(0))
        assertThat designCenterSyncs.size(),
                   is(equalTo(1))
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
                                   [Features.AppDeployment])

        // assert
        assertThat deployedChApps.size(),
                   is(equalTo(1))
        assertThat 'Feature disabled',
                   designCenterSyncs.size(),
                   is(equalTo(0))
    }
}
