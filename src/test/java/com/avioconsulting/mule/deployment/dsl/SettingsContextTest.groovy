package com.avioconsulting.mule.deployment.dsl

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@SuppressWarnings(["UnnecessaryQualifiedReference", "GroovyAssignabilityCheck"])
class SettingsContextTest {
    @Test
    void required_only() {
        // arrange
        def context = new SettingsContext()
        def closure = {
            username 'the_username'
            password 'the_password'
        }
        closure.delegate = context

        // act
        closure.call()

        // assert
        context.with {
            assertThat username,
                       is(equalTo('the_username'))
            assertThat password,
                       is(equalTo('the_password'))
        }
    }

    @Test
    void with_optional() {
        // arrange
        def context = new SettingsContext()
        def closure = {
            username 'the_username'
            password 'the_password'
            organizationName 'the_org'
        }
        closure.delegate = context

        // act
        closure.call()

        // assert
        context.with {
            assertThat username,
                       is(equalTo('the_username'))
            assertThat password,
                       is(equalTo('the_password'))
            assertThat organizationName,
                       is(equalTo('the_org'))
        }
    }

    @Test
    void missing_required() {
        // arrange
        def context = new SettingsContext()
        def closure = {
//            username 'the_username'
//            password 'the_password'
//            organizationName 'the_org'
        }
        closure.delegate = context
        closure.call()

        // act
        List<Exception> exceptions = []
        exceptions << shouldFail {
            context.username
        }
        exceptions << shouldFail {
            context.password
        }
        exceptions << shouldFail {
            context.organizationName
        }

        // assert
        def exceptionMessages = exceptions.collect { e -> e.message }.unique()
        assertThat exceptionMessages.size(),
                   is(equalTo(1))
        def exceptionMessage = exceptionMessages[0]
        assertThat exceptionMessage,
                   is(equalTo("""Your deployment request is not complete. The following errors exist:
- username missing
- password missing
""".trim()))
    }
}
