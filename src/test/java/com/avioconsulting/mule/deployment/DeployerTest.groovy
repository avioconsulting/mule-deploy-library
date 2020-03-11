package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DeployerTest extends BaseTest {
    private Deployer deployer

    @Before
    void setupDeployer() {
        def envLocator = new EnvironmentLocator(clientWrapper,
                                                System.out)
        deployer = new Deployer(this.clientWrapper,
                                envLocator,
                                System.out)
    }

    @Test
    void deployApplication_cloudhub() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
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
