package com.avioconsulting.mule.deployment.internal.models

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
class CloudhubAppPropertiesTest {

    private static Stream<Arguments> getData() {
        Stream.of(
                Arguments.of('Experience', 'exp-stuff'),
                Arguments.of('Process', 'prc-stuff'),
                Arguments.of('System', 'sys-stuff'),
                Arguments.of(null, 'unknownapp')
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getData")
    void layer_is_correct(String expectedLayer, String appName) {
        // arrange

        // act
        def props = new CloudhubAppProperties(appName,
                                              'dev',
                                              'abc',
                                              'the_id',
                                              'the_secret')

        // assert
        assertThat props.apiVisualizerLayer,
                   is(equalTo(expectedLayer))
    }
}
