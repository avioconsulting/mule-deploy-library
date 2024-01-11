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
        MatcherAssert.assertThat('fail', exception.message.contains("Name must be alphanumeric with dashes allowed within"))
    }

    @Test
    void empty_names() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName(null, false, false, null, null)).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Name must be alphanumeric with dashes allowed within"))
    }

    @Test
    void name_starting_with_dash() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('-avio-consulting', true, true, 'app-prefix', 'app-suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Name must be alphanumeric with dashes allowed within"))
    }

    @Test
    void name_ending_with_dash() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio-consulting-', true, true, 'app-prefix', 'app-suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Name must be alphanumeric with dashes allowed within"))
    }

    @Test
    void prefix_with_special_char() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio-consulting', true, true, 'app@prefix', 'app-suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Prefix must be alphanumeric with dashes allowed within"))
    }

    @Test
    void suffix_with_special_char() {
        // arrange

        // act
        def exception = shouldFail {
            (new ApplicationName('avio-consulting', true, true, 'app-prefix', 'app$suffix')).normalizedAppName
        }
        // assert
        MatcherAssert.assertThat('fail', exception.message.contains("Suffix must be alphanumeric with dashes allowed within"))
    }

}
