package com.avioconsulting.mule.deployment.models

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class FileBasedAppDeploymentRequestTest {
    @Test
    void modifyFileProps_no_changes() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def file = FileBasedAppDeploymentRequest.modifyFileProps('api.dev.properties',
                                                                 [:],
                                                                 zipFile)

        // assert
        assertThat 'No properties to change so do not do anything',
                   file,
                   is(equalTo(zipFile))
    }

    @Test
    void modifyFileProps_mule3() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def newZipFile = FileBasedAppDeploymentRequest.modifyFileProps('api.dev.properties',
                                                                       [
                                                                               existing: 'changed'
                                                                       ],
                                                                       zipFile)

        // assert
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        antBuilder.unzip(src: newZipFile.absolutePath,
                         dest: destination)
        def newProps = new Properties()
        newProps.load(new FileInputStream(new File(destination,
                                                   'classes/api.dev.properties')))
        assertThat newProps,
                   is(equalTo([
                           existing: 'changed',
                   ]))
        newProps = new Properties()
        newProps.load(new FileInputStream(new File(destination,
                                                   'classes/api.properties')))
        assertThat newProps,
                   is(equalTo([
                           'should.not.touch': 'this',
                   ]))
    }

    @Test
    void modifyFileProps_mule4() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.jar')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def newZipFile = FileBasedAppDeploymentRequest.modifyFileProps('api.dev.properties',
                                                                       [
                                                                               mule4_existing: 'changed'
                                                                       ],
                                                                       zipFile)

        // assert
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        antBuilder.unzip(src: newZipFile.absolutePath,
                         dest: destination)
        def newProps = new Properties()
        newProps.load(new FileInputStream(new File(destination,
                                                   'api.dev.properties')))
        assertThat newProps,
                   is(equalTo([
                           mule4_existing: 'changed',
                   ]))
    }

    @Test
    void modifyFileProps_not_found() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def exception = shouldFail {
            FileBasedAppDeploymentRequest.modifyFileProps('doesnotexist',
                                                          [
                                                                  existing: 'changed'
                                                          ],
                                                          zipFile)
        }

        // assert
        assertThat exception.message,
                   is(containsString('Expected to find the properties file you wanted to modify'))
    }
}
