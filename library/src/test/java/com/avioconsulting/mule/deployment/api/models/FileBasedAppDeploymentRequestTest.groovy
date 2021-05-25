package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.AppBuilding
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.apache.commons.io.FileUtils
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class FileBasedAppDeploymentRequestTest implements AppBuilding {
    @Test
    void getRamlFilesFromApp_is_not_apikit() {
        // arrange
        def tempDir = new File('target/temp')
        def tempAppDirectory = new File(tempDir,
                                        'designcenterapp')
        tempAppDirectory.deleteDir()
        tempAppDirectory.mkdirs()
        def file = new File(tempAppDirectory,
                            'stuff.xml')
        FileUtils.touch(file)
        file.text = '<hi/>'
        def request = buildZip(tempDir,
                               tempAppDirectory)

        // act
        def result = request.getRamlFilesFromApp('/api')

        // assert
        assertThat result,
                   is(equalTo([]))
    }

    @Test
    void getRamlFilesFromApp_is_apikit() {
        // arrange
        def request = buildFullApp()

        // act
        def result = request.getRamlFilesFromApp('/api')
                .sort { item -> item.fileName } // consistent for test

        // assert
        def expectedStuffContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        assertThat result,
                   is(equalTo([
                           new RamlFile('folder/lib.yaml',
                                        'howdy1'),
                           new RamlFile('stuff.raml',
                                        expectedStuffContents)
                   ]))
    }

    @Test
    void getRamlFilesFromApp_directory_specified() {
        // arrange
        def request = buildFullApp()

        // act
        def result = request.getRamlFilesFromApp('/api/folder')
                .sort { item -> item.fileName } // consistent for test

        // assert
        assertThat result,
                   is(equalTo([
                           new RamlFile('lib.yaml',
                                        'howdy1')
                   ]))
    }
}
