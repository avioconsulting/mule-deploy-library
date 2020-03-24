package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.Features
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class FeaturesContextTest {
    @Test
    void all() {
        // arrange
        def context = new FeaturesContext()
        def closure = {
            all
        }
        closure.delegate = context

        // act
        closure.call()
        def features = context.createFeatureList()

        // assert
        assertThat features,
                   is(equalTo([
                           Features.All
                   ]))
    }

    @Test
    void multiple() {
        // arrange
        def context = new FeaturesContext()
        def closure = {
            designCenterSync
            apiManagerDefinitions
        }
        closure.delegate = context

        // act
        closure.call()
        def features = context.createFeatureList()

        // assert
        assertThat features,
                   is(equalTo([
                           Features.DesignCenterSync,
                           Features.ApiManagerDefinitions
                   ]))
    }

    @Test
    void all_with_multiple() {
        // arrange
        def context = new FeaturesContext()
        def closure = {
            all
            designCenterSync
            apiManagerDefinitions
        }
        closure.delegate = context

        // act
        closure.call()
        def exception = shouldFail {
            context.createFeatureList()
        }

        // assert
        assertThat exception.message,
                   is(containsString('You cannot combine All with other features'))
    }

    @Test
    void nothing() {
        // arrange
        def context = new FeaturesContext()
        def closure = {
        }
        closure.delegate = context

        // act
        closure.call()
        def exception = shouldFail {
            context.createFeatureList()
        }

        // assert
        assertThat exception.message,
                   is(containsString('No features were specified. Either A) remove the enabledFeatures section, B) Add All inside enabledFeatures, or C) Add specific features'))
    }
}
