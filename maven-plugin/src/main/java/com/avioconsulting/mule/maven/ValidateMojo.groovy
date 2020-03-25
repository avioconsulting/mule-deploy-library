package com.avioconsulting.mule.maven

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = 'validate')
class ValidateMojo extends BaseMojo {
    @Parameter(defaultValue = 'cryptoKey,autoDiscClientId,autoDiscClientSecret,jarFile')
    private List<String> placeholderProperties
    @Parameter(defaultValue = 'DEV,QA,PRD')
    private List<String> environmentsToTest

    @Override
    Map<String, String> getAdditionalProperties() {
        placeholderProperties.collectEntries { prop ->
            [prop, 'placeholder']
        }
    }

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        log.info "Running ${groovyFile} through DSL to verify syntax"
        processDsl()
        log.info "Successfully processed ${groovyFile} through DSL"
    }
}
