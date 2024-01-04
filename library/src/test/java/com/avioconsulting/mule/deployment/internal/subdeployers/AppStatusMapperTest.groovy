package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class AppStatusMapperTest {

    private static Stream<Arguments> getData() {
        Stream.of(
                Arguments.of('AppStatus - Undeploying', 'UNDEPLOYING', null, AppStatus.Undeploying, null, null),
                Arguments.of('AppStatus - Deploying', 'DEPLOYING', null, AppStatus.Deploying, null, null),
                Arguments.of('AppStatus - Failed', 'DEPLOY_FAILED', null, AppStatus.Failed, null, null),
                Arguments.of('AppStatus - Started', 'STARTED', null, AppStatus.Started, null, null),
                Arguments.of('AppStatus - Deleted', 'DELETED', null, AppStatus.Deleted, null, null),

                Arguments.of('AppStatus - Unknown value', 'SOME_UNKNOWN_VALUE', null, AppStatus.Deleted, null, 'Unknown status value of SOME_UNKNOWN_VALUE detected from CloudHub!'),
                Arguments.of('DeploymentUpdateStatus - Deploying', 'UNDEPLOYING', 'DEPLOYING', null, DeploymentUpdateStatus.Deploying, null),
                Arguments.of('DeploymentUpdateStatus - Failed', 'UNDEPLOYING', 'DEPLOY_FAILED', null, DeploymentUpdateStatus.Failed, null),
                Arguments.of('DeploymentUpdateStatus - Unknown value', 'UNDEPLOYING', 'SOME_UNKNOWN_VALUE', null, DeploymentUpdateStatus.Failed, 'Unknown parsedDeployUpdateStatus value of SOME_UNKNOWN_VALUE detected from CloudHub!'),
        )
    }

    @ParameterizedTest(name = '{0}')
    @MethodSource("getData")
    void parseAppStatus(String description,
                        String inputAppStatusValue,
                        String inputDeploymentUpdatedStatusValue,
                        AppStatus expectedAppStatusEnum,
                        DeploymentUpdateStatus expectedDeploymentUpdateStatusEnum,
                        String expectedExceptionMessage) {
        // arrange
        def mapper = new AppStatusMapper()
        def inputMap = [
                status                : inputAppStatusValue,
                deploymentUpdateStatus: inputDeploymentUpdatedStatusValue
        ]

        // act
        if (expectedExceptionMessage) {
            def exception = shouldFail {
                mapper.parseAppStatus(inputMap)
            }
            assertThat exception.message,
                       is(containsString(expectedExceptionMessage))
        } else {
            def result = mapper.parseAppStatus(inputMap)

            // assert
            if (expectedAppStatusEnum) {
                assertThat result.appStatus,
                           is(equalTo(expectedAppStatusEnum))
            }
            if (expectedDeploymentUpdateStatusEnum) {
                assertThat result.deploymentUpdateStatus,
                           is(equalTo(expectedDeploymentUpdateStatusEnum))
            }
        }
    }
}
