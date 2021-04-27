package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecContextTest {
    @Test
    void required_only() {
        // arrange
        def simpleRamlFiles = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1'].join('\n'))
        ]
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest(simpleRamlFiles)

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.mainRamlFile,
                       is(equalTo('stuff-v1.raml'))
            assertThat it.exchangeAssetId,
                       is(equalTo('foo-bar'))
            assertThat it.endpoint,
                       is(nullValue())
            assertThat it.autoDiscoveryPropertyName,
                       is(equalTo('auto-discovery.api-id'))
        }
    }

    @Test
    void includes_optional() {
        // arrange
        def simpleRamlFiles = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1'].join('\n')),
                new RamlFile('foo.raml',
                             ['#%RAML 1.0',
                              'title: other stuff',
                              'version: v2'].join('\n'))
        ]
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            exchangeAssetId 'the-asset-id'
            mainRamlFile 'foo.raml'
            endpoint 'https://foo'
            autoDiscoveryPropertyName 'the.auto.disc'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest(simpleRamlFiles)

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.mainRamlFile,
                       is(equalTo('foo.raml'))
            assertThat it.exchangeAssetId,
                       is(equalTo('the-asset-id'))
            assertThat it.endpoint,
                       is(equalTo('https://foo'))
            assertThat it.autoDiscoveryPropertyName,
                       is(equalTo('the.auto.disc'))
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
