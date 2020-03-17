package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@RunWith(Parameterized)
class AppStatusMapperTest {
    private final String inputAppStatusValue
    private final AppStatus expectedAppStatusEnum
    private final DeploymentUpdateStatus expectedDeploymentUpdateStatusEnum
    private final String expectedExceptionMessage
    private final String inputDeploymentUpdatedStatusValue

    @Parameterized.Parameters(name = '{0}')
    static Iterable<Object[]> data() {
        [
                [
                        'AppStatus - Undeploying',
                        'UNDEPLOYING',
                        null,
                        AppStatus.Undeploying,
                        null,
                        null
                ],
                [
                        'AppStatus - Deploying',
                        'DEPLOYING',
                        null,
                        AppStatus.Deploying,
                        null,
                        null
                ],
                [
                        'AppStatus - Failed',
                        'DEPLOY_FAILED',
                        null,
                        AppStatus.Failed,
                        null,
                        null
                ],
                [
                        'AppStatus - Started',
                        'STARTED',
                        null,
                        AppStatus.Started,
                        null,
                        null
                ],
                [
                        'AppStatus - Deleted',
                        'DELETED',
                        null,
                        AppStatus.Deleted,
                        null,
                        null
                ],
                [
                        'AppStatus - Unknown value',
                        'SOME_UNKNOWN_VALUE',
                        null,
                        AppStatus.Deleted, // will be ignored
                        null,
                        'Unknown status value of SOME_UNKNOWN_VALUE detected from CloudHub!'
                ],
                [
                        'DeploymentUpdateStatus - Deploying',
                        'UNDEPLOYING', // don't care about this value, just need to not fail
                        'DEPLOYING',
                        null,
                        DeploymentUpdateStatus.Deploying,
                        null
                ],
                [
                        'DeploymentUpdateStatus - Failed',
                        'UNDEPLOYING', // don't care about this value, just need to not fail
                        'DEPLOY_FAILED',
                        null,
                        DeploymentUpdateStatus.Failed,
                        null
                ],
                [
                        'DeploymentUpdateStatus - Unknown value',
                        'UNDEPLOYING', // don't care about this value, just need to not fail
                        'SOME_UNKNOWN_VALUE',
                        null,
                        DeploymentUpdateStatus.Failed,
                        'Unknown parsedDeployUpdateStatus value of SOME_UNKNOWN_VALUE detected from CloudHub!'
                ]
        ].collect { listOfArgs ->
            listOfArgs.toArray()
        }
    }

    AppStatusMapperTest(String description,
                        String inputAppStatusValue,
                        String inputDeploymentUpdatedStatusValue,
                        AppStatus expectedAppStatusEnum,
                        DeploymentUpdateStatus expectedDeploymentUpdateStatusEnum,
                        String expectedExceptionMessage) {
        this.inputDeploymentUpdatedStatusValue = inputDeploymentUpdatedStatusValue
        this.expectedExceptionMessage = expectedExceptionMessage
        this.expectedDeploymentUpdateStatusEnum = expectedDeploymentUpdateStatusEnum
        this.expectedAppStatusEnum = expectedAppStatusEnum
        this.inputAppStatusValue = inputAppStatusValue
    }

    @Test
    void parseAppStatus() {
        // arrange
        def mapper = new AppStatusMapper()
        def inputMap = [
                status                : this.inputAppStatusValue,
                deploymentUpdateStatus: this.inputDeploymentUpdatedStatusValue
        ]

        // act
        if (this.expectedExceptionMessage) {
            def exception = shouldFail {
                mapper.parseAppStatus(inputMap)
            }
            assertThat exception.message,
                       is(containsString(expectedExceptionMessage))
        } else {
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
}
