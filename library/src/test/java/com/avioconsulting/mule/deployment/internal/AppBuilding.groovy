package com.avioconsulting.mule.deployment.internal

import com.avioconsulting.mule.deployment.api.models.ExchangeAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import org.apache.commons.io.FileUtils

trait AppBuilding {
    FileBasedAppDeploymentRequest buildFullFileBasedApp() {
        def tempDir = new File('target/temp')
        def tempAppDirectory = new File(tempDir,
                                        'designcenterapp')
        tempAppDirectory.deleteDir()
        tempAppDirectory.mkdirs()
        def apiDirectory = new File(tempAppDirectory,
                                    'api')
        def file = new File(apiDirectory,
                            'stuff.raml')
        FileUtils.touch(file)
        file.text = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        def folder = new File(apiDirectory,
                              'folder')
        file = new File(folder,
                        'lib.yaml')
        FileUtils.touch(file)
        file.text = 'howdy1'
        def exchangeModules = new File(apiDirectory,
                                       'exchange_modules')
        file = new File(exchangeModules,
                        'junk')
        FileUtils.touch(file)
        def exchangeChildDirectory = new File(exchangeModules,
                                              'subdir')
        file = new File(exchangeChildDirectory,
                        'junk')
        FileUtils.touch(file)
        FileUtils.touch(new File(apiDirectory,
                                 'exchange.json'))
        apiDirectory = new File(tempAppDirectory,
                                'api2')
        file = new File(apiDirectory,
                        'stuff-v2.raml')
        FileUtils.touch(file)
        file.text = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v2'
        ].join('\n')
        buildZip(tempDir,
                 tempAppDirectory)
    }

    FileBasedAppDeploymentRequest buildZip(File tempDir,
                                           File tempAppDirectory) {
        def antBuilder = new AntBuilder()
        def zipFile = new File(tempDir,
                               'designcenterapp.zip')
        FileUtils.deleteQuietly(zipFile)
        antBuilder.zip(destfile: zipFile,
                       basedir: tempAppDirectory)
        new TestFileBasedRequest(zipFile, "my-app", "1.0.0", "PROD")
    }
}
