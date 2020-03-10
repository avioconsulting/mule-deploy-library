package com.avioconsulting.jenkins.mule.impl.models

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecificationTest {
    @Test
    void minimum_params() {
        // arrange

        // act
        def result = new ApiSpecification('Product API')

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(nullValue())
    }

    @Test
    void minimum_params_upper_case() {
        // arrange

        // act
        def result = new ApiSpecification('SystemStuff API')

        // assert
        assertThat result.name,
                   is(equalTo('SystemStuff API'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
        assertThat result.exchangeAssetId,
                   is(equalTo('systemstuff-api'))
        assertThat result.mainRamlFile,
                   is(nullValue())
    }

    @Test
    void specify_everything() {
        // arrange

        // act
        def result = new ApiSpecification('SystemStuff API',
                                          [
                                                  apiMajorVersion: 'v2',
                                                  exchangeAssetId: 'nope',
                                                  mainRamlFile   : 'hello.raml'
                                          ])

        // assert
        assertThat result.name,
                   is(equalTo('SystemStuff API'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v2'))
        assertThat result.exchangeAssetId,
                   is(equalTo('nope'))
        assertThat result.mainRamlFile,
                   is(equalTo('hello.raml'))
    }

    @Test
    void wrong_params() {
        // arrange

        // act
        def exception = shouldFail {
            new ApiSpecification('SystemStuff API',
                                 [nope: 'bad'])
        }

        // assert
        assertThat exception.message,
                   is(containsString('No such property: nope for class'))
    }
}
