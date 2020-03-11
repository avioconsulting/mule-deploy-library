package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.models.*
import com.avioconsulting.mule.deployment.subdeployers.ICloudHubDeployer
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class DeployerTest {
    private Deployer deployer
    private ICloudHubDeployer mockCloudHubDeployer
    private List<CloudhubDeploymentRequest> deployedApps

    @Before
    void setupDeployer() {
        deployedApps = []
        mockCloudHubDeployer = [
                deploy: { CloudhubDeploymentRequest request ->
                    deployedApps << request
                }
        ] as ICloudHubDeployer
        deployer = new Deployer(null,
                                System.out,
                                null,
                                mockCloudHubDeployer)
    }

    @Test
    void deployApplication_cloudhub() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')
        def stream = new FileInputStream(file)
        def request = new CloudhubDeploymentRequest(stream,
                                                    'DEV',
                                                    'new-app',
                                                    new CloudhubWorkerSpecRequest('3.9.1',
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    file.name,
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
        assertThat deployedApps.size(),
                   is(equalTo(1))
        Assert.fail("write it, add design center stuff")
    }

    @Test
    void deployApplication_onprem() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void deployApplication_features_all_no_api_spec() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void deployApplication_app_deployment_feature_disabled() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void deployApplication_design_center_feature_disabled() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
