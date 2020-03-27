package com.avioconsulting.mule.deployment.api.models

import groovy.xml.QName

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
            def parser = new XmlParser().parseText(pomXmlPath.text)
            def props = [:]
            def getLocalValue = { Node node ->
                (node.value() as QName).localPart
            }
            parser['properties'][0].each { Node node ->
                def key = (node.name() as QName).localPart
                props[key] = getLocalValue(node)
            }
            return new PomInfo(getLocalValue(parser['groupId'][0] as Node),
                               getLocalValue(parser['artifactId'][0] as Node),
                               getLocalValue(parser['version'][0] as Node),
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
