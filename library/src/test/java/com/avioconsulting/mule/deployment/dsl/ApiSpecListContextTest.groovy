package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.junit.Test

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
    void list_multiple() {
        // arrange
        def context = new ApiSpecListContext()
        def closure = {
            apiSpecification {
                name 'Mule Deploy Design Center Test Project'
            }
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
                   is(equalTo(2))
        assertThat result[0],
                   is(instanceOf(ApiSpecification))
        assertThat result[1],
                   is(instanceOf(ApiSpecification))
    }
}
