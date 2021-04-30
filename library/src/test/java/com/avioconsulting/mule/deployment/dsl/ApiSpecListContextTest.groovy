package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ApiSpecListContextTest {
    private static List<RamlFile> getSimpleRamlFiles() {
        [
                new RamlFile('stuff-v1.raml',
                             ['#%RAML 1.0',
                              'title: stuff',
                              'version: v1'].join('\n'))
        ]
    }

    @Test
    void list_single() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createApiSpecList(simpleRamlFiles)

        // assert
        assertThat result.size(),
                   is(equalTo(1))
        assertThat result[0],
                   is(instanceOf(ApiSpecification))
    }

    @Test
    void list_multiple_missing_auto_disc() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
            }
            apiSpecification {
                name 'Mule Deploy Design Center Test Project v2'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createApiSpecList(simpleRamlFiles)
        }

        // assert
        assertThat exception.message,
                   is(containsString('If you have multiple API specs, you must specify a unique `autoDiscoveryPropertyName` for all of them!'))
    }

    @Test
    void list_multiple_missing_auto_disc_unique_props() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop1'
            }
            apiSpecification {
                name 'Mule Deploy Design Center Test Project v2'
                autoDiscoveryPropertyName 'prop1'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createApiSpecList(simpleRamlFiles)
        }

        // assert
        assertThat exception.message,
                   is(containsString('If you have multiple API specs, you must specify a unique `autoDiscoveryPropertyName` for all of them!'))
    }

    @Test
    void list_multiple_same_name_missing_branch() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop1'
            }
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop2'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def exception = shouldFail {
            context.createApiSpecList(simpleRamlFiles)
        }

        // assert
        assertThat exception.message,
                   is(containsString('You either need separate design center project names for v1 and v2 OR you need different designCenterBranchName'))
    }

    @Test
    void list_multiple_different_names_ok() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop1'
            }
            apiSpecification {
                name 'Mule Deploy Design Center Test Project v2'
                autoDiscoveryPropertyName 'prop2'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createApiSpecList(simpleRamlFiles)

        // assert
        assertThat result.size(),
                   is(equalTo(2))
        assertThat result[0],
                   is(instanceOf(ApiSpecification))
        assertThat result[1],
                   is(instanceOf(ApiSpecification))
    }

    @Test
    void list_multiple_different_branches_ok() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop1'
            }
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
                autoDiscoveryPropertyName 'prop2'
                designCenterBranchName 'v1'
            }
        }
        closure.delegate = context
        closure.call()

        // act
        def result = context.createApiSpecList(simpleRamlFiles)

        // assert
        assertThat result.size(),
                   is(equalTo(2))
        assertThat result[0],
                   is(instanceOf(ApiSpecification))
        assertThat result[1],
                   is(instanceOf(ApiSpecification))
    }
}
