package com.avioconsulting.mule.deployment.dsl

import org.junit.Assert
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest()

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.apiMajorVersion,
                       is(equalTo('v1'))
            assertThat it.mainRamlFile,
                       is(nullValue())
            assertThat it.exchangeAssetId,
                       is(equalTo('foo-bar'))
            assertThat it.endpoint,
                       is(nullValue())
        }
    }

    @Test
    void includes_optional() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }

    @Test
    void missing_required() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
