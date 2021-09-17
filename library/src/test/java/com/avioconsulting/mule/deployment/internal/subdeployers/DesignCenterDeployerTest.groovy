package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.BaseTest
import com.avioconsulting.mule.deployment.TestConsoleLogger
import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.AppBuilding
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DesignCenterDeployerTest extends BaseTest implements AppBuilding {
    private DesignCenterDeployer deployer

    @Before
    void clean() {
        setupDeployer(DryRunMode.Run)
    }

    def setupDeployer(DryRunMode dryRunMode) {
        deployer = new DesignCenterDeployer(clientWrapper,
                                            new TestConsoleLogger(),
                                            dryRunMode,
                                            environmentLocator)
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
            url = request.uri()
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
                   is(equalTo('/designcenter/api-designer/projects'))
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
            urls << request.uri()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                def jsonResult
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
                                    path: 'examples/foo.raml',
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
                            ],
                            [
                                    path: 'exchange_modules/something/else',
                                    type: 'FILE'
                            ]
                    ]
                } else {
                    jsonResult = 'the contents'
                }
                end(JsonOutput.toJson(jsonResult))
            }
        }

        // act
        def result = deployer.getExistingDesignCenterFiles('ourprojectId',
                                                           'somebranch')

        // assert
        assertThat result,
                   is(equalTo([
                           new RamlFile('stuff.raml',
                                        'the contents'),
                           new RamlFile('examples/foo.raml',
                                        'the contents')
                   ]))
        assertThat urls,
                   is(equalTo([
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files',
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files/stuff.raml',
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files/examples%2Ffoo.raml'
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
            urls << request.uri()
            methods << request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 204
                end()
            }
        }

        // act
        deployer.deleteDesignCenterFiles('ourprojectId',
                                         'somebranch',
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
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files/file1',
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files/file2'
                   ]))
    }

    @Test
    void deleteDesignCenterFiles_subdirectory() {
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
            urls << request.uri()
            methods << request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.response().with {
                statusCode = 204
                end()
            }
        }

        // act
        deployer.deleteDesignCenterFiles('ourprojectId',
                                         'somebranch',
                                         [
                                                 new RamlFile('subdir/file1',
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
                           '/designcenter/api-designer/projects/ourprojectId/branches/somebranch/files/subdir%2Ffile1'
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
            url = request.uri()
            method = request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as List<Map>
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
                                         'somebranch',
                                         files)

        // assert
        assertThat method,
                   is(equalTo('POST'))
        assertThat url,
                   is(equalTo('/designcenter/api-designer/projects/ourprojectId/branches/somebranch/save?commit=true&message=fromAPI'))
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

    @Test
    void pushToExchange() {
        // arrange
        String anypointOrgId = null
        String url = null
        String method = null
        String ownerGuid = null
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            url = request.uri()
            method = request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 204
                end()
            }
        }
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        def files = [
                new RamlFile('folder/file3.raml',
                             'the contents3'),
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.pushToExchange(apiSpec,
                                'ourprojectId',
                                '1.2.3')

        // assert
        assertThat method,
                   is(equalTo('POST'))
        assertThat url,
                   is(equalTo('/designcenter/api-designer/projects/ourprojectId/branches/master/publish/exchange'))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
        assertThat sentPayload,
                   is(equalTo([
                           main      : 'file1.raml',
                           apiVersion: 'v1',
                           version   : '1.2.3',
                           assetId   : 'hello-api',
                           name      : 'Hello API',
                           groupId   : 'the-org-id',
                           classifier: 'raml',
                           metadata  : [
                                   branchId : 'master',
                                   projectId: 'ourprojectId'
                           ]
                   ]))
    }

    @Test
    void pushToExchange_with_subVersion() {
        // arrange
        String anypointOrgId = null
        String url = null
        String method = null
        String ownerGuid = null
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            url = request.uri()
            method = request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 204
                end()
            }
        }
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        def files = [
                new RamlFile('folder/file3.raml',
                        'the contents3'),
                new RamlFile('file1.raml',
                        file1RamlContents),
                new RamlFile('file2.raml',
                        'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                files)

        // act
        deployer.pushToExchange(apiSpec,
                'ourprojectId',
                '1.0.0-1')

        // assert
        assertThat method,
                is(equalTo('POST'))
        assertThat url,
                is(equalTo('/designcenter/api-designer/projects/ourprojectId/branches/master/publish/exchange'))
        assertThat anypointOrgId,
                is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                ownerGuid,
                is(equalTo('the_id'))
        assertThat sentPayload,
                is(equalTo([
                        main      : 'file1.raml',
                        apiVersion: 'v1',
                        version   : '1.0.0-1',
                        assetId   : 'hello-api',
                        name      : 'Hello API',
                        groupId   : 'the-org-id',
                        classifier: 'raml',
                        metadata  : [
                                branchId : 'master',
                                projectId: 'ourprojectId'
                        ]
                ]))
    }

    @Test
    void pushToExchange_old_version_alongside_new() {
        // arrange
        String anypointOrgId = null
        String url = null
        String method = null
        String ownerGuid = null
        Map sentPayload = null
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            anypointOrgId = request.getHeader('X-ORGANIZATION-ID')
            url = request.uri()
            method = request.method().toString()
            ownerGuid = request.getHeader('X-OWNER-ID')
            request.bodyHandler { body ->
                sentPayload = new JsonSlurper().parseText(body.toString()) as Map
            }
            request.response().with {
                statusCode = 204
                end()
            }
        }
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        def files = [
                new RamlFile('folder/file3.raml',
                             'the contents3'),
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        // a 2.x.x deploy of the v1 definition sitting alongside the v2 definition
        deployer.pushToExchange(apiSpec,
                                'ourprojectId',
                                '2.0.1')

        // assert
        assertThat method,
                   is(equalTo('POST'))
        assertThat url,
                   is(equalTo('/designcenter/api-designer/projects/ourprojectId/branches/master/publish/exchange'))
        assertThat anypointOrgId,
                   is(equalTo('the-org-id'))
        assertThat 'Design center needs this',
                   ownerGuid,
                   is(equalTo('the_id'))
        assertThat sentPayload,
                   is(equalTo([
                           main      : 'file1.raml',
                           apiVersion: 'v1',
                           version   : '1.0.1',
                           assetId   : 'hello-api',
                           name      : 'Hello API',
                           groupId   : 'the-org-id',
                           classifier: 'raml',
                           metadata  : [
                                   branchId : 'master',
                                   projectId: 'ourprojectId'
                           ]
                   ]))
    }

    static def mockDesignCenterProjectId(HttpServerRequest request,
                                         String projectName,
                                         String projectId) {
        def mocked = false
        if (request.uri() == '/designcenter/api-designer/projects') {
            mocked = true
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson([
                        [
                                id  : projectId,
                                name: projectName
                        ]
                ]))
            }
        }
        mocked
    }

    static def mockFileUpload(HttpServerRequest request,
                              String projectId) {
        def mocked = false
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/save?commit=true&message=fromAPI") {
            mocked = true
            request.response().with {
                statusCode = 204
                end()
            }
        }
        mocked
    }

    static def mockExchangePush(HttpServerRequest request,
                                String projectId) {
        def mocked = false
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/publish/exchange") {
            mocked = true
            request.response().with {
                statusCode = 204
                end()
            }
        }
        mocked
    }

    static def mockAcquireLock(HttpServerRequest request,
                               String projectId) {
        def mocked = false
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/acquireLock"
                && request.method() == HttpMethod.POST) {
            mocked = true
            request.response().with {
                statusCode = 200
                end()
            }
        }
        mocked
    }

    static def mockReleaseLock(HttpServerRequest request,
                               String projectId) {
        def mocked = false
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/releaseLock"
                && request.method() == HttpMethod.POST) {
            mocked = true
            request.response().with {
                statusCode = 200
                end()
            }
        }
        mocked
    }

    static def mockGetExistingFiles(HttpServerRequest request,
                                    String projectId,
                                    Map<String, String> filesAndContents) {
        def mocked = false
        def filenames = filesAndContents.keySet()
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/files") {
            mocked = true
            def responsePayload = filenames.collect { fileName ->
                [
                        path: fileName,
                        type: 'FILE'
                ]
            }
            request.response().with {
                statusCode = 200
                putHeader('Content-Type',
                          'application/json')
                end(JsonOutput.toJson(responsePayload))
            }
        }
        filenames.each { fileName ->
            if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/files/${fileName}" &&
                    request.method() == HttpMethod.GET) {
                mocked = true
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                              'application/json')
                    end(JsonOutput.toJson(filesAndContents[fileName]))
                }
            }
        }
        mocked
    }

    static def mockDeleteFile(HttpServerRequest request,
                              String projectId,
                              String fileName) {
        def mocked = false
        if (request.uri() == "/designcenter/api-designer/projects/${projectId}/branches/master/files/${fileName}"
                && request.method() == HttpMethod.DELETE) {
            mocked = true
            request.response().with {
                statusCode = 204
                end()
            }
        }
        mocked
    }

    @Test
    void synchronizeDesignCenter_no_existing_files() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                locked = false
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (locked && mockFileUpload(request,
                                         'abcd')) {
                filesUploaded = true
                return
            }
            if (locked && mockExchangePush(request,
                                           'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [:])) {
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        def files = [
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         files,
                                         '1.2.3')

        // assert
        assertThat filesUploaded,
                   is(equalTo(true))
        assertThat exchangePushed,
                   is(equalTo(true))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }

    @Test
    void synchronizeDesignCenter_no_raml_files() {
        // arrange
        withHttpServer { HttpServerRequest request ->
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def apiSpec = new ApiSpecification('Hello API',
                                           [])

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         [],
                                         '1.2.3')

        // assert
    }

    @Test
    void synchronizeDesignCenter_removes_existing_files() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        def deleted = false
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                locked = false
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (locked && mockFileUpload(request,
                                         'abcd')) {
                filesUploaded = true
                return
            }
            if (locked && mockExchangePush(request,
                                           'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [
                                             'file1.raml'             : file1RamlContents,
                                             'file_to_be_deleted.raml': 'the contents'
                                     ])) {
                return
            }
            if (locked && mockDeleteFile(request,
                                         'abcd',
                                         'file_to_be_deleted.raml')) {
                deleted = true
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }

        def files = [
                new RamlFile('file1.raml',
                             file1RamlContents)
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         files,
                                         '1.2.3')

        // assert
        assertThat filesUploaded,
                   is(equalTo(false))
        assertThat exchangePushed,
                   is(equalTo(true))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
        assertThat deleted,
                   is(equalTo(true))
    }

    @Test
    void synchronizeDesignCenter_raml_not_changed() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                // we should not be doing any locking anyways
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (mockFileUpload(request,
                               'abcd')) {
                filesUploaded = true
                return
            }
            if (mockExchangePush(request,
                                 'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [
                                             'file1.raml': file1RamlContents,
                                             'file2.raml': 'the contents2'
                                     ])) {
                return
            }
            if (request.uri().endsWith('graphql')) {
                println 'Received mock Exchange query, saying we got 1 asset'
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                              'application/json')
                    def response = [
                            data: [
                                    assets: [
                                            [
                                                    '__typename': 'Asset',
                                                    assetId     : 'foo',
                                                    version     : '1.0.201910193',
                                                    versionGroup: 'v1'
                                            ]
                                    ]
                            ]
                    ]
                    end(JsonOutput.toJson(response))
                }
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def files = [
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         files,
                                         '1.2.3')

        // assert
        assertThat filesUploaded,
                   is(equalTo(false))
        assertThat exchangePushed,
                   is(equalTo(false))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }

    @Test
    void synchronizeDesignCenter_raml_not_changed_no_exchange_asset() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                locked = false
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (locked && mockFileUpload(request,
                                         'abcd')) {
                filesUploaded = true
                return
            }
            if (locked && mockExchangePush(request,
                                           'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [
                                             'file1.raml': file1RamlContents,
                                             'file2.raml': 'the contents2'
                                     ])) {
                return
            }
            if (request.uri().endsWith('graphql')) {
                println 'Received mock Exchange query, saying we got nothing'
                request.response().with {
                    statusCode = 200
                    putHeader('Content-Type',
                              'application/json')
                    def response = [
                            data: null
                    ]
                    end(JsonOutput.toJson(response))
                }
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def files = [
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents2')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         files,
                                         '1.2.3')

        // assert
        assertThat filesUploaded,
                   is(equalTo(false))
        assertThat exchangePushed,
                   is(equalTo(true))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }

    @Test
    void synchronizeDesignCenter_raml_changed() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        def file1RamlContents = [
                '#%RAML 1.0',
                'title: stuff',
                'version: v1'
        ].join('\n')
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                locked = false
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (locked && mockFileUpload(request,
                                         'abcd')) {
                filesUploaded = true
                return
            }
            if (locked && mockExchangePush(request,
                                           'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [
                                             'file1.raml': file1RamlContents,
                                             'file2.raml': 'the contents2'
                                     ])) {
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def files = [
                new RamlFile('file1.raml',
                             file1RamlContents),
                new RamlFile('file2.raml',
                             'the contents updated')
        ]
        def apiSpec = new ApiSpecification('Hello API',
                                           files)

        // act
        deployer.synchronizeDesignCenter(apiSpec,
                                         files,
                                         '1.2.3')

        // assert
        assertThat filesUploaded,
                   is(equalTo(true))
        assertThat exchangePushed,
                   is(equalTo(true))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }

    @Test
    void synchronizeDesignCenterFromApp() {
        // arrange
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                locked = false
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (locked && mockFileUpload(request,
                                         'abcd')) {
                filesUploaded = true
                return
            }
            if (locked && mockExchangePush(request,
                                           'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [:])) {
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def request = buildFullApp()
        def apiSpec = new ApiSpecification('Hello API',
                                           request.getRamlFilesFromApp('/api',
                                                                       false))

        // act
        deployer.synchronizeDesignCenterFromApp(apiSpec,
                                                request)

        // assert
        assertThat filesUploaded,
                   is(equalTo(true))
        assertThat exchangePushed,
                   is(equalTo(true))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }

    @Test
    void synchronizeDesignCenter_online_validate() {
        // arrange
        setupDeployer(DryRunMode.OnlineValidate)
        def filesUploaded = false
        def exchangePushed = false
        def locked = false
        withHttpServer { HttpServerRequest request ->
            if (mockAuthenticationOk(request)) {
                return
            }
            if (mockAcquireLock(request,
                                'abcd')) {
                locked = true
                return
            }
            if (mockReleaseLock(request,
                                'abcd')) {
                // we should not be doing any locking anyways
                return
            }
            if (mockDesignCenterProjectId(request,
                                          'Hello API',
                                          'abcd')) {
                return
            }
            if (mockFileUpload(request,
                               'abcd')) {
                filesUploaded = true
                return
            }
            if (mockExchangePush(request,
                                 'abcd')) {
                exchangePushed = true
                return
            }
            if (mockGetExistingFiles(request,
                                     'abcd',
                                     [:])) {
                return
            }
            request.response().with {
                statusCode = 404
                end("Unexpected request ${request.absoluteURI()}")
            }
        }
        def appInfo = buildFullApp()
        def apiSpec = new ApiSpecification('Hello API',
                                           appInfo.getRamlFilesFromApp('/api',
                                                                       false))

        // act
        deployer.synchronizeDesignCenterFromApp(apiSpec,
                                                appInfo)

        // assert
        assertThat filesUploaded,
                   is(equalTo(false))
        assertThat exchangePushed,
                   is(equalTo(false))
        assertThat 'We should always unlock if we lock',
                   locked,
                   is(equalTo(false))
    }
}
