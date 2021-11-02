package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecificationTest {
    private static List<RamlFile> getSimpleRamlFiles() {
        [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1'].join('\n'))
        ]
    }

    @Test
    void minimum_params() {
        // arrange

        // act
        def result = new ApiSpecification('Product API',
                                          simpleRamlFiles,
                                          null)

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
        assertThat result.designCenterBranchName,
                   is(equalTo('master'))
        assertThat result.sourceDirectory,
                   is(equalTo('/api'))
        assertThat result.soapApi,
                   is(equalTo(false))
    }

    @Test
    void no_version_in_raml() {
        // arrange
        def files = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff'].join('\n'))
        ]

        // act
        def result = new ApiSpecification('Product API',
                                          files,
                                          'stuff-v1.raml'
        )

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }

    @Test
    void raml_library() {
        // arrange
        def files = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v2',
                              'uses:',
                              ' stuff: lib.raml'].join('\n')),
                new RamlFile('lib.raml',
                             ['#%RAML 1.0 Library',
                              'types:',
                              ' Foo: string'].join('\n'))
        ]

        // act
        def result = new ApiSpecification('Product API',
                                          files,
                                          'stuff-v1.raml'
        )

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v2'))
    }

    @Test
    void raml_include() {
        // arrange
        def files = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1',
                              'uses:',
                              ' foo: something.raml'].join('\n')),
                new RamlFile('something.raml',
                             ['#%RAML 1.0 Library',
                              'types:',
                              ' NullablePickedEnumStatus:',
                              '  type: string|nil',
                              '  enum: ["A","B","C",null]'].join('\n'))
        ]

        // act
        def result = new ApiSpecification('Product API',
                                          files,
                                          'stuff-v1.raml'
        )

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }

    @Test
    void raml_include_uses_root_directory_slash() {
        // arrange
        def files = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1',
                              'uses:',
                              ' foo: /something.raml'].join('\n')),
                new RamlFile('something.raml',
                             ['#%RAML 1.0 Library',
                              'types:',
                              ' NullablePickedEnumStatus:',
                              '  type: string|nil',
                              '  enum: ["A","B","C",null]'].join('\n'))
        ]

        // act
        def result = new ApiSpecification('Product API',
                                          files,
                                          'stuff-v1.raml'
        )

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }

    @Test
    void raml_include_subdir() {
        // arrange
        def files = [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1',
                              'uses:',
                              ' foo: stuff/something.raml'].join('\n')),
                new RamlFile('stuff/something.raml',
                             ['#%RAML 1.0 Library',
                              'types:',
                              ' NullablePickedEnumStatus:',
                              '  type: string|nil',
                              '  enum: ["A","B","C",null]'].join('\n'))
        ]

        // act
        def result = new ApiSpecification('Product API',
                                          files,
                                          'stuff-v1.raml'
        )

        // assert
        assertThat result.name,
                   is(equalTo('Product API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('product-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }

    @Test
    void minimum_params_upper_case() {
        // arrange

        // act
        def result = new ApiSpecification('SystemStuff API',
                                          simpleRamlFiles,
                                          null
        )

        // assert
        assertThat result.name,
                   is(equalTo('SystemStuff API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('systemstuff-api'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }

    @Test
    void specify_everything() {
        // arrange

        // act
        def result = new ApiSpecification('SystemStuff API',
                                          simpleRamlFiles,
                                          'stuff-v1.raml',
                                          'nope',
                                          'http://nope',
                                          'prop',
                                          'someOtherBranch',
                                          'api2')

        // assert
        assertThat result.name,
                   is(equalTo('SystemStuff API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('nope'))
        assertThat result.mainRamlFile,
                   is(equalTo('stuff-v1.raml'))
        assertThat result.autoDiscoveryPropertyName,
                   is(equalTo('prop'))
        assertThat result.designCenterBranchName,
                   is(equalTo('someOtherBranch'))
        assertThat result.sourceDirectory,
                   is(equalTo('api2'))
    }

    @Test
    void not_found() {
        // arrange

        // act
        def exception = shouldFail {
            new ApiSpecification('SystemStuff API',
                                 simpleRamlFiles,
                                 'oops.raml',
                                 'nope')
        }

        // assert
        assertThat exception.message,
                   is(containsString("You specified 'oops.raml' as your main RAML file but it does not exist in your application under /api!"))
    }

    @Test
    void not_apikit() {
        // arrange

        // act
        def result = new ApiSpecification('SystemStuff SOAP API',
                                          'v1',
                                          'nope')

        // assert
        assertThat result.name,
                   is(equalTo('SystemStuff SOAP API'))
        assertThat result.exchangeAssetId,
                   is(equalTo('nope'))
        assertThat result.mainRamlFile,
                   is(nullValue())
        assertThat result.soapApi,
                   is(equalTo(true))
        assertThat result.apiMajorVersion,
                   is(equalTo('v1'))
    }
}
