package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.internal.AppBuilding
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecContextTest implements AppBuilding {
    @Test
    void required_only() {
        // arrange
        def appRequest = buildFullApp()
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest(appRequest)

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.mainRamlFile,
                       is(equalTo('stuff.raml'))
            assertThat it.exchangeAssetId,
                       is(equalTo('foo-bar'))
            assertThat it.endpoint,
                       is(nullValue())
            assertThat it.autoDiscoveryPropertyName,
                       is(equalTo('auto-discovery.api-id'))
            assertThat it.designCenterBranchName,
                       is(equalTo('master'))
        }
    }

    @Test
    void soap() {
        // arrange
        def appRequest = buildFullApp()
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            soapEndpointWithVersion 'v1'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest(appRequest)

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.mainRamlFile,
                       is(nullValue())
            assertThat it.exchangeAssetId,
                       is(equalTo('foo-bar'))
            assertThat it.endpoint,
                       is(nullValue())
            assertThat it.autoDiscoveryPropertyName,
                       is(equalTo('auto-discovery.api-id'))
            assertThat it.designCenterBranchName,
                       is(nullValue())
            assertThat it.soapApi,
                       is(equalTo(true))
        }
    }

    @Test
    void soap_and_rest() {
        // arrange
        def appRequest = buildFullApp()
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            soapEndpointWithVersion 'v1'
            designCenterBranchName ' stuff'
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createRequest(appRequest)
        }

        // assert
        assertThat exception.message,
                   is(containsString('You used soapEndpointWithVersion but also supplied 1 or more of the following. These are not compatible! mainRamlFile, designCenterBranchName, sourceDirectory'))
    }

    @Test
    void includes_optional() {
        // arrange
        def appRequest = buildFullApp()
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            exchangeAssetId 'the-asset-id'
            mainRamlFile 'stuff-v2.raml'
            endpoint 'https://foo'
            autoDiscoveryPropertyName 'the.auto.disc'
            designCenterBranchName 'theBranch'
            sourceDirectory '/api2'
        }
        closure.delegate = context
        closure.call()

        // act
        def request = context.createRequest(appRequest)

        // assert
        request.with {
            assertThat it.name,
                       is(equalTo('Foo Bar'))
            assertThat it.mainRamlFile,
                       is(equalTo('stuff-v2.raml'))
            assertThat it.exchangeAssetId,
                       is(equalTo('the-asset-id'))
            assertThat it.endpoint,
                       is(equalTo('https://foo'))
            assertThat it.autoDiscoveryPropertyName,
                       is(equalTo('the.auto.disc'))
            assertThat it.designCenterBranchName,
                       is(equalTo('theBranch'))
            assertThat it.apiMajorVersion,
                       is(equalTo('v2'))
            assertThat it.sourceDirectory,
                       is(equalTo('/api2'))
        }
    }

    @Test
    void includes_optional_source_directory_not_found() {
        // arrange
        def appRequest = buildFullApp()
        def context = new ApiSpecContext()
        def closure = {
            name 'Foo Bar'
            exchangeAssetId 'the-asset-id'
            mainRamlFile 'stuff-v2.raml'
            endpoint 'https://foo'
            autoDiscoveryPropertyName 'the.auto.disc'
            designCenterBranchName 'theBranch'
            sourceDirectory '/doesnotexist'
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createRequest(appRequest)
        }

        // assert
        assertThat exception.message,
                   is(containsString("You specified 'stuff-v2.raml' as your main RAML file but it does not exist in your application under /doesnotexist!"))
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
