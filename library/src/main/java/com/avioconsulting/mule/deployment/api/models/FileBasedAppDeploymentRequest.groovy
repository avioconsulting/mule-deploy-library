package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.models.RamlFile
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChild

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

abstract class FileBasedAppDeploymentRequest {
    protected final Map<String, String> autoDiscoveries = [:]

    static final List<String> IGNORE_DC_FILES = [
            'exchange_modules', // we don't deal with Exchange dependencies
            '.gitignore',
            'exchange.json', // see above
            '.designer.json'
    ]

    static List<String> IGNORE_DC_FILES_EXCEPT_EXCHANGE = IGNORE_DC_FILES - ['exchange_modules']

    static boolean isIgnored(Path something,
                             boolean ignoreExchange = true) {
        def parent = something.parent
        def list = ignoreExchange ? IGNORE_DC_FILES : IGNORE_DC_FILES_EXCEPT_EXCHANGE
        if (parent) {
            if (list.contains(parent.toString())) {
                return true
            }
            return isIgnored(parent,
                             ignoreExchange)
        }
        return list.contains(something.toString())
    }

    @Lazy
    protected PomInfo parsedPomProperties = {
        def zipOrJarPath = getFile().toPath()
        FileSystems.newFileSystem(zipOrJarPath,
                                  null).withCloseable { fs ->
            def pomXmlPath = Files.walk(fs.getPath('/META-INF/maven')).find { p ->
                p.endsWith('pom.xml')
            } as Path
            assert pomXmlPath: 'Was not able to find pom.xml in ZIP/JAR'
            def parser = new XmlSlurper().parseText(pomXmlPath.text)
            def props = parser.properties.children().collectEntries { NodeChild node ->
                [node.name(), node.text()]
            }
            def textFromNode = { String element ->
                def res = parser[element][0] as NodeChild
                res.text()
            }
            return new PomInfo(textFromNode('groupId'),
                               textFromNode('artifactId'),
                               textFromNode('version'),
                               props)
        } as PomInfo
    }()

    boolean isMule4Request() {
        isMule4Request(file)
    }

    static boolean isMule4Request(File file) {
        file.name.endsWith('.jar')
    }

    abstract File getFile()

    def setAutoDiscoveryId(String propertyName,
                           String autoDiscoveryId) {
        this.autoDiscoveries[propertyName] = autoDiscoveryId
    }

    String getArtifactId() {
        return this.parsedPomProperties.artifactId
    }

    abstract String getAppVersion()

    abstract String getEnvironment()

    List<RamlFile> getRamlFilesFromApp(String rootRamlDirectory,
                                       boolean ignoreExchange) {
        return FileSystems.newFileSystem(file.toPath(),
                                         null).withCloseable { fs ->
            def apiPath = fs.getPath(rootRamlDirectory)
            if (!Files.exists(apiPath)) {
                return []
            }
            // we intentionally get everything, NOT just .raml files, in case the RAML references
            // JSON examples, etc.
            Files.walk(apiPath).findAll { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                !Files.isDirectory(p) &&
                        !isIgnored(relativeToApiDirectory,
                                   ignoreExchange)
            }.collect { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                new RamlFile(relativeToApiDirectory.toString(),
                             p.text)
            }
        }
    }
}
