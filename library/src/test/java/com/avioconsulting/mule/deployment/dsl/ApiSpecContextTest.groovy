package com.avioconsulting.mule.deployment.dsl

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
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
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            exchangeAssetId 'the-asset-id'
            apiMajorVersion 'v2'
            mainRamlFile 'foo.raml'
            endpoint 'https://foo'
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
                       is(equalTo('v2'))
            assertThat it.mainRamlFile,
                       is(equalTo('foo.raml'))
            assertThat it.exchangeAssetId,
                       is(equalTo('the-asset-id'))
            assertThat it.endpoint,
                       is(equalTo('https://foo'))
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new ApiSpecContext()
        def closure = {
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createRequest()
        }

        // assert
        assertThat exception.message,
                   is(containsString("""Your API spec is not complete. The following errors exist:
- name missing"""))
    }
}
