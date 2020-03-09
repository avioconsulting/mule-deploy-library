package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.FileBasedDeploymentRequest
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
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
        def request = new FileBasedDeploymentRequest() {
            @Override
            InputStream getApp() {
                null
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
