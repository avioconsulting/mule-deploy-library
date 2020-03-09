package com.avioconsulting.jenkins.mule.impl.models

import groovy.transform.Canonical
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class FileBasedDeploymentRequestTest {
    @Canonical
    class DummyRequest implements FileBasedDeploymentRequest {
        String fileName, overrideByChangingFileInZip
        InputStream app
        Map<String, String> appProperties
    }

    @Test
    void modifyZipFileWithNewProperties_no_changes() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def inputStream = new FileInputStream(zipFile)
        def request = new DummyRequest(zipFile.name,
                                       'api.dev.properties',
                                       inputStream,
                                       [:])

        // act
        def stream = request.modifyZipFileWithNewProperties()

        // assert
        assertThat 'No properties to change so do not do anything',
                   stream,
                   is(equalTo(inputStream))
    }

    @Test
    void modifyZipFileWithNewProperties_mule3() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def request = new DummyRequest(zipFile.name,
                                       'api.dev.properties',
                                       zipFile.newInputStream(),
                                       [
                                               existing: 'changed'
                                       ])

        // act
        def stream = request.modifyZipFileWithNewProperties()

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
    void modifyZipFileWithNewProperties_mule4() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.jar')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def request = new DummyRequest(zipFile.name,
                                       'api.dev.properties',
                                       zipFile.newInputStream(),
                                       [
                                               mule4_existing: 'changed'
                                       ])

        // act
        def stream = request.modifyZipFileWithNewProperties()

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
    void modifyZipFileWithNewProperties_not_found() {
        // arrange
        def antBuilder = new AntBuilder()
        def zipFile = new File('target/temp/ourapp.zip')
        if (zipFile.exists()) {
            assert zipFile.delete()
        }
        antBuilder.zip(destfile: zipFile.absolutePath,
                       basedir: 'src/test/resources/testapp')
        def request = new DummyRequest(zipFile.name,
                                       'doesnotexist',
                                       zipFile.newInputStream(),
                                       [
                                               existing: 'changed'
                                       ])

        // act
        def stream = request.modifyZipFileWithNewProperties()

        stream.bytes
    }
}
