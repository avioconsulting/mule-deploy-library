package com.avioconsulting.mule.deployment.models

import groovy.transform.Canonical
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class FileBasedAppDeploymentRequestTest {
    @Canonical
    class DummyRequest implements FileBasedAppDeploymentRequest {
        InputStream app
        String fileName
    }

    @Test
    void getPropertyModifiedStream_no_changes() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def inputStream = new FileInputStream(zipFile)

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [:],
                                                            inputStream,
                                                            zipFile.name)

        // assert
        assertThat 'No properties to change so do not do anything',
                   stream,
                   is(equalTo(inputStream))
    }

    @Test
    void getPropertyModifiedStream_mule3() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [
                                                                    existing: 'changed'
                                                            ],
                                                            zipFile.newInputStream(),
                                                            zipFile.name)

        // assert
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        def newZipFile = new File('target/temp/newapp.zip')
        println 'begin reading bytes'
        newZipFile.bytes = stream.bytes
        println 'finished reading bytes'
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
    void getPropertyModifiedStream_mule4() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.jar')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [
                                                                    mule4_existing: 'changed'
                                                            ],
                                                            zipFile.newInputStream(),
                                                            zipFile.name)

        // assert
        def destination = new File('target/temp/modifiedapp')
        if (destination.exists()) {
            assert destination.deleteDir()
        }
        def newZipFile = new File('target/temp/newapp.zip')
        println 'begin reading bytes'
        newZipFile.bytes = stream.bytes
        println 'finished reading bytes'
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
    void getPropertyModifiedStream_not_found() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')

        // act
        def stream = DummyRequest.getPropertyModifiedStream('doesnotexist',
                                                            [
                                                                    existing: 'changed'
                                                            ],
                                                            zipFile.newInputStream(),
                                                            zipFile.name)

        stream.bytes
    }
}
