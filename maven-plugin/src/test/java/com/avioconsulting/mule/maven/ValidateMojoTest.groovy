package com.avioconsulting.mule.maven

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.ArtifactHandler
import org.apache.maven.project.MavenProject
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.is

class ValidateMojoTest {
    def logger = new TestLogger()

    @Before
    void cleanup() {
        logger.errors.clear()
    }

    ValidateMojo getMojo(String groovyFileText) {
        def groovyFile = new File('stuff.groovy')
        groovyFile.text = groovyFileText
        def mockMavenProject = [
                getAttachedArtifacts: {
                    [
                            new DefaultArtifact('thegroup',
                                                'theart',
                                                '1.0.0',
                                                'compile',
                                                'jar',
                                                'mule-application',
                                                [:] as ArtifactHandler).with {
                                it.setFile(new File('foo.jar'))
                                it
                            }
                    ]
                }
        ] as MavenProject
        new ValidateMojo().with {
            it.groovyFile = groovyFile
            it.log = logger
            it.mavenProject = mockMavenProject ?: ([:] as MavenProject)
            it
        }
    }

    @Test
    void passes() {
        // arrange
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(dslText)

        // act
        mojo.execute()

        // assert
        // no fails means ok
    }

    @Test
    void fails_syntax() {
        // arrange
        def dslText = """
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment 'DEV'
        d
        applicationName 'the-app'
        appVersion '1.2.3'
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(dslText)

        // act
        def exception = shouldFail {
            mojo.execute()
        }

        // assert
        assertThat exception.message,
                   is(containsString('foobar'))
    }

    @Test
    void fails_dsl_environment_stuff() {
        // arrange
        def dslText = """
def versionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4'
]
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment params.env
        applicationName 'the-app'
        appVersion versionsForEnvironments[params.env]
        file 'path/to/file.jar'
        targetServerOrClusterName 'theServer'
    }
}
"""
        def mojo = getMojo(dslText)

        // act
        def exception = shouldFail {
            mojo.execute()
        }

        // assert
        assertThat exception.message,
                   is(containsString('foobar problem with tst and prd'))
    }
}
