package com.avioconsulting.mule.maven

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.ArtifactHandler
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ValidateMojoTest implements MavenInvoke {
    def logger = new TestLogger()

    @BeforeAll
    static void setupApp() {
        buildApp()
    }

    @BeforeEach
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
            it.log = this.logger
            it.placeholderProperties = 'cryptoKey,autoDiscClientId,autoDiscClientSecret,jarFile'.split(',')
            it.environmentsToTest = 'DEV,QA,PRD'.split(',')
            it.environmentProperty = 'env'
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
        applicationName {
            baseAppName 'the-app'
            prefix 'AVI'
            suffix 'xxx'
            usePrefix false
            useSuffix false
        }
        appVersion '1.2.3'
        file '${builtFile}'
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
    d
    onPremApplication {
        environment 'DEV'
        applicationName {
            baseAppName 'the-app'
            prefix 'AVI'
            suffix 'xxx'
            usePrefix false
            useSuffix false
        }
        appVersion '1.2.3'
        file '${builtFile}'
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
                   is(containsString('Validation failed, see log message'))
        assertThat logger.errors,
                   is(equalTo([
                           'DEV environment - Unable to process DSL because class groovy.lang.MissingPropertyException No such property: d for class: stuff',
                           'QA environment - Unable to process DSL because class groovy.lang.MissingPropertyException No such property: d for class: stuff',
                           'PRD environment - Unable to process DSL because class groovy.lang.MissingPropertyException No such property: d for class: stuff',
                   ]))
    }

    @Test
    void fails_dsl_environment_stuff() {
        // arrange
        def dslText = """
def versionsForEnvironments = [
        DEV: '4.2.2',
        QA: '4.1.4',
        PRD: '4.1.3'
]
def serverForEnvironments = [
    DEV: 'svr1',
    QA: 'svr2'
]
muleDeploy {
    version '1.0'
    
    onPremApplication {
        environment params.env
        applicationName {
            baseAppName 'the-app'
            prefix 'AVI'
            suffix 'xxx'
            usePrefix false
            useSuffix false
        }
        appVersion versionsForEnvironments[params.env]
        file '${builtFile}'
        targetServerOrClusterName serverForEnvironments[params.env]
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
                   is(containsString('Validation failed, see log messages'))
        assertThat logger.errors,
                   is(equalTo([
                           'PRD environment - Unable to process DSL because class java.lang.Exception Your deployment request is not complete. The following errors exist:\n' +
                                   '- targetServerOrClusterName missing'
                   ]))
    }
}
