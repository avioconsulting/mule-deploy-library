package com.avioconsulting.mule.deployment.internal.models


import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@RunWith(Parameterized)
class CloudhubAppPropertiesTest {
    private final String expectedLayer
    private final String appName

    @Parameterized.Parameters(name = "{0}")
    static Collection getData() {
        [
                ['Experience', 'exp-stuff'],
                ['Process', 'prc-stuff'],
                ['System', 'sys-stuff'],
                [null, 'unknownapp']
        ].collect { it.toArray(new Object[0]) }
    }

    CloudhubAppPropertiesTest(String expectedLayer,
                              String appName) {
        this.appName = appName
        this.expectedLayer = expectedLayer
    }

    @Test
    void layer_is_correct() {
        // arrange

        // act
        def props = new CloudhubAppProperties(this.appName,
                                              'dev',
                                              'abc',
                                              'the_id',
                                              'the_secret')

        // assert
        assertThat props.apiVisualizerLayer,
                   is(equalTo(this.expectedLayer))
    }
}
