package com.avioconsulting.mule.maven

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'validate')
class ValidateMojo extends BaseMojo {
    @Parameter(defaultValue = 'cryptoKey,autoDiscClientId,autoDiscClientSecret')
    private List<String> placeholderProperties
    @Parameter(defaultValue = 'DEV,QA,PRD')
    private List<String> environmentsToTest
    @Parameter(defaultValue = 'env')
    private String environmentProperty
    private String currentEnvironment
    // match up with JUnit/etc.
    @Parameter(property = 'skipTests')
    private boolean skip

    @Override
    Map<String, String> getAdditionalProperties() {
        placeholderProperties.collectEntries { prop ->
            [prop, 'placeholder']
        } + [
                (environmentProperty): currentEnvironment
        ]
    }

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            logger.println 'Skipping due to -DskipTests flag'
            return
        }
        def failed = false
        logger.println "Will validate environments: ${environmentsToTest}"
        environmentsToTest.each { env ->
            currentEnvironment = env
            logger.withLogContext("${env} environment") {
                try {
                    logger.println "Running ${groovyFile} through DSL to verify syntax"
                    processDsl()
                    logger.println "Successfully processed ${groovyFile} through DSL"
                }
                catch (e) {
                    // already logged by processDsl
                    failed = true
                }
            }
        }
        if (failed) {
            throw new Exception('Validation failed, see log messages')
        }
    }
}
