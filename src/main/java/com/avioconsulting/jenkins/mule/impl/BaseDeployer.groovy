package com.avioconsulting.jenkins.mule.impl

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext

abstract class BaseDeployer implements HttpRequestInterceptor {
    protected final String baseUrl
    protected final CloseableHttpClient httpClient
    protected final PrintStream logger
    private final String anypointOrganizationId
    private final String username
    private final String password
    private String accessToken
    protected final int retryIntervalInMs
    protected final int maxTries

    BaseDeployer(String baseUrl,
                 String anypointOrganizationId,
                 String username,
                 String password,
                 int retryIntervalInMs,
                 int maxTries,
                 PrintStream logger) {
        this.maxTries = maxTries
        this.retryIntervalInMs = retryIntervalInMs
        this.password = password
        this.username = username
        this.anypointOrganizationId = anypointOrganizationId
        this.logger = logger
        this.baseUrl = baseUrl
        this.httpClient = HttpClients.custom()
                .addInterceptorFirst(this)
                .build()
    }

    protected def addStandardStuff(request,
                                   String environmentId) {
        request.addHeader('X-ANYPNT-ENV-ID',
                          environmentId)
        request.addHeader('X-ANYPNT-ORG-ID',
                          anypointOrganizationId)
    }

    String locateEnvironment(String environmentName) {
        def request = new HttpGet("${baseUrl}/accounts/api/organizations/${anypointOrganizationId}/environments")
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'Retrieve environments')
            def listing = result.data
            def environment = listing.find { env ->
                env.name == environmentName
            }
            if (!environment) {
                def valids = listing.collect { env ->
                    env.name
                }
                throw new Exception("Unable to find environment '${environmentName}'. Valid environments are ${valids}")
            }
            logger.println "Resolved environment '${environmentName}' to ID '${environment.id}'"
            environment.id
        }
        finally {
            response.close()
        }
    }

    /**
     *
     * @param username
     * @param password
     * @return - an auth token
     */
    String authenticate() {
        if (this.accessToken) {
            return this.accessToken
        }
        logger.println "Authenticating to Anypoint as user '${username}'"
        def payload = [
                username: username,
                password: password
        ]
        def request = new HttpPost("${baseUrl}/accounts/login").with {
            setEntity(new StringEntity(JsonOutput.toJson(payload)))
            addHeader('Content-Type',
                      'application/json')
            it
        }
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               "authenticate to Anypoint as '${username}'")
            logger.println 'Successfully authenticated'
            accessToken = result.access_token
        }
        finally {
            response.close()
        }
    }

    static protected def assertSuccessfulResponse(CloseableHttpResponse response,
                                                  String failureContext) {
        def status = response.statusLine.statusCode
        if (status < 200 || status > 299) {
            throw new Exception("Unable to ${failureContext}, got an HTTP ${status} with a response of '${response.entity.content.text}'")
        }
    }

    static protected def assertSuccessfulResponseAndReturnJson(CloseableHttpResponse response,
                                                               String failureContext) {
        assertSuccessfulResponse(response,
                                 failureContext)
        def contentType = response.getFirstHeader('Content-Type')
        assert contentType?.value?.contains('application/json'): "Expected a JSON response but got ${contentType}!"
        new JsonSlurper().parse(response.entity.content)
    }

    def close() {
        httpClient.close()
    }

    @Override
    void process(HttpRequest httpRequest,
                 HttpContext httpContext) throws HttpException, IOException {
        if (accessToken) {
            httpRequest.setHeader('Authorization',
                                  "Bearer ${accessToken}")
        }
    }

    InputStream modifyZipFileWithNewProperties(InputStream inputZipFile,
                                               String zipFileName,
                                               String propertiesFileToAddTo,
                                               Map propertiesToAdd) {
        if (propertiesToAdd.isEmpty()) {
            return inputZipFile
        }
        def isMule4 = zipFileName.endsWith('.jar')
        // Mule 4 props files live at the root of the JAR. Mule 3's are in a classes subdirectory
        propertiesFileToAddTo = isMule4 ? propertiesFileToAddTo : "classes/${propertiesFileToAddTo}"
        def factory = new ArchiveStreamFactory()
        // small semantic difference between JAR and ZIP and on-prem/Mule 4 Runtime Manager will
        // complain if it's not set right
        def format = isMule4 ? ArchiveStreamFactory.JAR : ArchiveStreamFactory.ZIP
        def archiveIn = factory.createArchiveInputStream(format,
                                                         inputZipFile)
        def pos = new PipedOutputStream()
        def archiveOut = factory.createArchiveOutputStream(format,
                                                           pos)
        Thread.start {
            ZipArchiveEntry inputEntry
            def found = false
            List<String> propertiesFilesFound = []
            try {
                while ((inputEntry = archiveIn.nextEntry as ZipArchiveEntry) != null) {
                    assert archiveIn.canReadEntryData(inputEntry)
                    archiveOut.putArchiveEntry(inputEntry)
                    if (!inputEntry.isDirectory()) {
                        if (inputEntry.name.endsWith('.properties')) {
                            propertiesFilesFound << inputEntry.name
                        }
                        if (inputEntry.name == propertiesFileToAddTo) {
                            found = true
                            logger.println "Modifying ${isMule4 ? 'Mule 4' : 'Mule 3'} properties file '${inputEntry.name}'"
                            def modifiedStream = modifyProperties(archiveIn,
                                                                  propertiesFileToAddTo,
                                                                  propertiesToAdd)
                            IOUtils.copy(modifiedStream,
                                         archiveOut)
                        } else {
                            IOUtils.copy(archiveIn,
                                         archiveOut)
                        }
                    }
                    archiveOut.closeArchiveEntry()
                }
            }
            finally {
                archiveIn.close()
                archiveOut.finish()
                archiveOut.close()
            }
            if (!found) {
                logger.println "ERROR: Expected to find the properties file you wanted to modify, ${propertiesFileToAddTo}, in the ZIP archive, but did not! Only files seen were ${propertiesFilesFound}."
            }
        }
        new PipedInputStream(pos)
    }

    InputStream modifyProperties(InputStream input,
                                 String propertiesFileToAddTo,
                                 Map propertiesToAdd) {
        def props = new Properties()
        props.load(input)
        props.putAll(propertiesToAdd)
        logger.println "Merged properties from ${propertiesFileToAddTo}: ${props}"
        def bos = new ByteArrayOutputStream()
        props.store(bos,
                    'Modified by deployment process')
        new ByteArrayInputStream(bos.toByteArray())
    }
}
