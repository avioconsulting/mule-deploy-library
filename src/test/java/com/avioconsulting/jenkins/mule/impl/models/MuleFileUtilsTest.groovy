package com.avioconsulting.jenkins.mule.impl.models

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class MuleFileUtilsTest {
    class DummyRequest implements MuleFileUtils {
        @Override
        String getFileName() {
            return null
        }

        @Override
        InputStream getApp() {
            return null
        }

        @Override
        String getOverrideByChangingFileInZip() {
            return null
        }

        @Override
        Map<String, String> getAppProperties() {
            return null
        }
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
        def request = new DummyRequest()

        // act
        def stream = request.modifyZipFileWithNewProperties(inputStream,
                                                            zipFile.name,
                                                            'api.dev.properties',
                                                            [:])

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
        def request = new DummyRequest()

        // act
        def stream = request.modifyZipFileWithNewProperties(new FileInputStream(zipFile),
                                                            zipFile.name,
                                                            'api.dev.properties',
                                                            [
                                                                    existing: 'changed'
                                                            ])

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
        def request = new DummyRequest()

        // act
        def stream = request.modifyZipFileWithNewProperties(new FileInputStream(zipFile),
                                                            zipFile.name,
                                                            'api.dev.properties',
                                                            [
                                                                    mule4_existing: 'changed'
                                                            ])

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
        def request = new DummyRequest()

        // act
        def stream = request.modifyZipFileWithNewProperties(new FileInputStream(zipFile),
                                                            zipFile.name,
                                                            'doesnotexist',
                                                            [
                                                                    existing: 'changed'
                                                            ])

        stream.bytes
    }
}
