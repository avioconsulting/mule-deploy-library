package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail

class ApplicationNameTest {

    @Test
    void valid_name() {
        // arrange

        // act
        def name = (new ApplicationName('avio-consulting', true, true, 'app-prefix', 'app-suffix')).normalizedAppName
        // assert
        assert name == 'app-prefix-avio-consulting-app-suffix'
    }

    @Test
    void name_with_spaces() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio consulting', true, true, 'app-prefix', 'app-suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("name must be alphanumeric and can include dash"))
    }

    @Test
    void prefix_with_special_char() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio-consulting', true, true, 'app@prefix', 'app-suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("as you going to use a prefix, the prefix must be alphanumeric and can include dash"))
    }

    @Test
    void suffix_with_special_char() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio-consulting', true, true, 'app-prefix', 'app$suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("as you going to use a suffix, the suffix must be alphanumeric and can include dash"))
    }

}
