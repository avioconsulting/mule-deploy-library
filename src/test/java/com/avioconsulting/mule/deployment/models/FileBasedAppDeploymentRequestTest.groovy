package com.avioconsulting.mule.deployment.models

import com.avioconsulting.mule.deployment.models.AppFileInfo
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class FileBasedAppDeploymentRequestTest {
    class DummyRequest implements FileBasedAppDeploymentRequest {
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
        def appFileInfo = new AppFileInfo(zipFile.name,
                                          inputStream)

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [:],
                                                            appFileInfo).app

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
        def appFileInfo = new AppFileInfo(zipFile.name,
                                          zipFile.newInputStream())

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [
                                                                    existing: 'changed'
                                                            ],
                                                            appFileInfo).app

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
        def appFileInfo = new AppFileInfo(zipFile.name,
                                          zipFile.newInputStream())

        // act
        def stream = DummyRequest.getPropertyModifiedStream('api.dev.properties',
                                                            [
                                                                    mule4_existing: 'changed'
                                                            ],
                                                            appFileInfo).app

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
        def appFileInfo = new AppFileInfo(zipFile.name,
                                          zipFile.newInputStream())

        // act
        def stream = DummyRequest.getPropertyModifiedStream('doesnotexist',
                                                            [
                                                                    existing: 'changed'
                                                            ],
                                                            appFileInfo).app

        stream.bytes
    }
}
