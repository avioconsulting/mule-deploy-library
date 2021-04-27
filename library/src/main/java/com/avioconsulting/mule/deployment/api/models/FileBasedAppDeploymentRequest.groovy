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

    static boolean isIgnored(Path something) {
        def parent = something.parent
        if (parent) {
            if (IGNORE_DC_FILES.contains(parent.toString())) {
                return true
            }
            return isIgnored(parent)
        }
        return IGNORE_DC_FILES.contains(something.toString())
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

    abstract String getAppVersion()

    abstract String getEnvironment()

    List<RamlFile> getRamlFilesFromApp() {
        return FileSystems.newFileSystem(file.toPath(),
                                         null).withCloseable { fs ->
            def apiPath = fs.getPath('/api')
            if (!Files.exists(apiPath)) {
                return []
            }
            Files.walk(apiPath).findAll { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                !Files.isDirectory(p) &&
                        !isIgnored(relativeToApiDirectory)
            }.collect { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                new RamlFile(relativeToApiDirectory.toString(),
                             p.text)
            }
        }
    }
}
