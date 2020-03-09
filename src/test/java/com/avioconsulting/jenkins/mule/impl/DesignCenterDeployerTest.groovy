package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.FileBasedDeploymentRequest
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DesignCenterDeployerTest implements HttpServerUtils {
    HttpServer httpServer
    private DesignCenterDeployer deployer
    int port
    private HttpClientWrapper clientWrapper

    @Before
    void startServer() {
        httpServer = Vertx.vertx().createHttpServer()
        port = 8080
        clientWrapper = new HttpClientWrapper("http://localhost:${port}",
                                              'the user',
                                              'the password',
                                              'the-org-id',
                                              System.out)
        deployer = new DesignCenterDeployer(clientWrapper,
                                            System.out)
    }

    @After
    void stopServer() {
        clientWrapper.close()
        httpServer.close()
    }

    @Test
    void getRamlFilesFromApp_is_apikit() {
        // arrange
        def tempDir = new File('target/temp')
        def tempAppDirectory = new File(tempDir,
                                        'designcenterapp')
        tempAppDirectory.deleteDir()
        tempAppDirectory.mkdirs()
        def apiDirectory = new File(tempAppDirectory,
                                    'api')
        FileUtils.touch(new File(apiDirectory,
                                 'stuff.yaml'))
        def folder = new File(apiDirectory,
                              'folder')
        FileUtils.touch(new File(folder,
                                 'lib.yaml'))
        def exchangeModules = new File(apiDirectory,
                                       'exchange_modules')
        FileUtils.touch(new File(exchangeModules,
                                 'junk'))
        FileUtils.touch(new File(apiDirectory,
                                 'exchange.json'))
        def antBuilder = new AntBuilder()
        def zipFile = new File(tempDir,
                               'designcenterapp.zip')
        FileUtils.deleteQuietly(zipFile)
        antBuilder.zip(destfile: zipFile,
                       basedir: tempAppDirectory)
        def request = new FileBasedDeploymentRequest() {
            @Override
            InputStream getApp() {
                zipFile.newInputStream()
            }
        }

        // act
        def result = deployer.getRamlFilesFromApp(request)

        // assert
        Assert.fail("write it, assert RAML contents, build a temporary ZIP file")
    }

    @Test
    void getRamlFilesFromApp_is_not_apikit() {
        // arrange

        // act

        // assert
        Assert.fail("write it")
    }
}
