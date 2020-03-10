package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.AppFileInfo
import com.avioconsulting.jenkins.mule.impl.models.RamlFile
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

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
        def file = new File(apiDirectory,
                            'stuff.yaml')
        FileUtils.touch(file)
        file.text = 'howdy2'
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
        FileUtils.touch(new File(apiDirectory,
                                 'exchange.json'))
        def request = buildZip(tempDir,
                               tempAppDirectory)

        // act
        def result = deployer.getRamlFilesFromApp(request)
                .sort { item -> item.fileName } // consistent for test

        // assert
        assertThat result,
                   is(equalTo([
                           new RamlFile('folder/lib.yaml',
                                        'howdy1'),
                           new RamlFile('stuff.yaml',
                                        'howdy2')
                   ]))
    }

    private static AppFileInfo buildZip(File tempDir, File tempAppDirectory) {
        def antBuilder = new AntBuilder()
        def zipFile = new File(tempDir,
                               'designcenterapp.zip')
        FileUtils.deleteQuietly(zipFile)
        antBuilder.zip(destfile: zipFile,
                       basedir: tempAppDirectory)
        new AppFileInfo(zipFile.name,
                        zipFile.newInputStream())
    }

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
        def result = deployer.getRamlFilesFromApp(request)

        // assert
        assertThat result,
                   is(equalTo([]))
    }

    @Test
    void getDesignCenterProjectId_found() {
        // arrange
        String anypointOrgId = null
        String url = null
        String ownerGuid = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            url = request.absoluteURI()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        [
                                id  : 'blah',
                                name: 'the project'
                        ],
                        [
                                id  : 'foo',
                                name: 'other project'
                        ]
                ]))
            }
        }

        // act
        def result = deployer.getDesignCenterProjectId('the project')

        // assert
        assertThat result,
                   is(equalTo('blah'))
        assertThat url,
                   is(equalTo('http://localhost:8080/designcenter/api-designer/projects'))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
    }

    @Test
    void getDesignCenterProjectId_not_found() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        [
                                id  : 'blah',
                                name: 'the project'
                        ],
                        [
                                id  : 'foo',
                                name: 'other project'
                        ]
                ]))
            }
        }

        // act
        def exception = shouldFail {
            deployer.getDesignCenterProjectId('not found project')
        }

        // assert
        assertThat exception.message,
                   is(containsString("Unable to find ID for Design Center project 'not found project'"))
    }

    @Test
    void getExistingDesignCenterFiles() {
        // arrange
        String anypointOrgId = null
        List<String> urls = []
        String ownerGuid = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            urls << request.absoluteURI()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def jsonResult = null
                if (request.absoluteURI().endsWith('files')) {
                    jsonResult = [
                            [
                                    path: '.gitignore',
                                    type: 'FILE'
                            ],
                            [
                                    path: 'exchange.json',
                                    type: 'FILE'
                            ],
                            [
                                    path: 'stuff.raml',
                                    type: 'FILE'
                            ],
                            [
                                    path: 'howdy',
                                    type: 'FOLDER'
                            ],
                            [
                                    path: '.designer.json',
                                    type: 'FILE'
                            ],
                            [
                                    path: 'exchange_modules/something',
                                    type: 'FILE'
                            ]
                    ]
                } else {
                    jsonResult = '"the contents'
                }
                end(JsonOutput.toJson(jsonResult))
            }
        }

        // act
        def result = deployer.getExistingDesignCenterFiles('ourprojectId')

        // assert
        assertThat result,
                   is(equalTo([
                           new RamlFile('stuff.raml',
                                        'the contents')
                   ]))
        assertThat urls,
                   is(equalTo([
                           'http://localhost:8080/designcenter/api-designer/projects/ourprojectId/branches/master/files',
                           'http://localhost:8080/designcenter/api-designer/projects/ourprojectId/branches/master/files/stuff.raml'
                   ]))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
    }

    @Test
    void deleteDesignCenterFiles() {
        // arrange
        String anypointOrgId = null
        List<String> urls = []
        List<String> methods = []
        String ownerGuid = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            urls << request.absoluteURI()
            methods << request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 204
                end()
            }
        }

        // act
        deployer.deleteDesignCenterFiles('ourprojectId',
                                         [
                                                 new RamlFile('file1',
                                                              'blah'),
                                                 new RamlFile('file2',
                                                              'blah')
                                         ])

        // assert
        assertThat methods.unique(),
                   is(equalTo(['DELETE']))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
        assertThat urls,
                   is(equalTo([
                           'http://localhost:8080/designcenter/api-designer/projects/ourprojectId/branches/master/files/file1',
                           'http://localhost:8080/designcenter/api-designer/projects/ourprojectId/branches/master/files/file2'
                   ]))
    }

    @Test
    void uploadDesignCenterFiles_correct_request() {
        // arrange
        String anypointOrgId = null
        String url = null
        String method = null
        String ownerGuid = null
        List<Map> sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            url = request.absoluteURI()
            method = request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString())
            }
            request.response().with {
                statusCode = 204
                end()
            }
        }
        def files = [
                new RamlFile('file1.raml',
                             'the contents'),
                new RamlFile('file2.raml',
                             'the contents2')
        ]

        // act
        deployer.uploadDesignCenterFiles('ourprojectId',
                                         files)

        // assert
        assertThat method,
                   is(equalTo('POST'))
        assertThat url,
                   is(equalTo('http://localhost:8080/designcenter/api-designer/projects/ourprojectId/branches/master/save?commit=true&message=fromAPI'))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
        assertThat sentPayload,
                   is(equalTo([
                           [
                                   path   : 'file1.raml',
                                   content: 'the contents'
                           ],
                           [
                                   path   : 'file2.raml',
                                   content: 'the contents2'
                           ]
                   ]))
    }
}
