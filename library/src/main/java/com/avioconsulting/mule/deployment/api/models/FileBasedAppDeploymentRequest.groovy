package com.avioconsulting.mule.deployment.api.models

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChild

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

abstract class FileBasedAppDeploymentRequest {
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

    abstract def setAutoDiscoveryId(String autoDiscoveryId)

    abstract String getAppVersion()

    abstract String getEnvironment()
}
