package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@RunWith(Parameterized)
class AppStatusMapperTest {
    private final String inputValue
    private final AppStatus expectedAppStatusEnum
    private final DeploymentUpdateStatus expectedDeploymentUpdateStatusEnum

    @Parameterized.Parameters(name = '{0}')
    static Iterable<Object[]> data() {
        [
                [
                        'AppStatus - Undeploying',
                        'UNDEPLOYING',
                        AppStatus.Undeploying,
                        null
                ],
                [
                        'AppStatus - Deploying',
                        'DEPLOYING',
                        AppStatus.Deploying,
                        null
                ],
                [
                        'AppStatus - Failed',
                        'DEPLOY_FAILED',
                        AppStatus.Failed,
                        null
                ],
                [
                        'AppStatus - Started',
                        'STARTED',
                        AppStatus.Started,
                        null
                ],
                [
                        'AppStatus - Deleted',
                        'DELETED',
                        AppStatus.Deleted,
                        null
                ],
                [
                        'DeploymentUpdateStatus - Deploying',
                        'DEPLOYING',
                        null,
                        DeploymentUpdateStatus.Deploying
                ],
                [
                        'DeploymentUpdateStatus - Failed',
                        'DEPLOY_FAILED',
                        null,
                        DeploymentUpdateStatus.Failed
                ]
        ].collect { listOfArgs ->
            listOfArgs.toArray()
        }
    }

    AppStatusMapperTest(String description,
                        String inputValue,
                        AppStatus expectedAppStatusEnum,
                        DeploymentUpdateStatus expectedDeploymentUpdateStatusEnum) {
        this.expectedDeploymentUpdateStatusEnum = expectedDeploymentUpdateStatusEnum
        this.expectedAppStatusEnum = expectedAppStatusEnum
        this.inputValue = inputValue
    }

    @Test
    void parseAppStatus() {
        // arrange
        def mapper = new AppStatusMapper()
        def inputMap = [:]
        if (this.expectedAppStatusEnum) {
            inputMap['status'] = inputValue
        }
        if (this.expectedDeploymentUpdateStatusEnum) {
            inputMap['deploymentUpdateStatus'] = inputValue
        }

        // act
        def result = mapper.parseAppStatus(inputMap)

        // assert
        if (this.expectedAppStatusEnum) {
            assertThat result.appStatus,
                       is(equalTo(this.expectedAppStatusEnum))
        }
        if (this.expectedDeploymentUpdateStatusEnum) {
            assertThat result.deploymentUpdateStatus,
                       is(equalTo(this.expectedDeploymentUpdateStatusEnum))
        }
    }
}
