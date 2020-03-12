package com.avioconsulting.mule.deployment.models

import java.nio.file.FileSystems
import java.nio.file.Files

abstract class FileBasedAppDeploymentRequest {
    boolean isMule4Request() {
        isMule4Request(file)
    }

    static boolean isMule4Request(File file) {
        file.name.endsWith('.jar')
    }

    abstract File getFile()

    static File modifyFileProps(String propertiesFileToAddTo,
                                Map<String, String> propertiesToAdd,
                                File file) {
        if (propertiesToAdd.isEmpty()) {
            return file
        }
        def isMule4 = isMule4Request(file)
        // Mule 4 props files live at the root of the JAR. Mule 3's are in a classes subdirectory
        propertiesFileToAddTo = isMule4 ? propertiesFileToAddTo : "classes/${propertiesFileToAddTo}"
        FileSystems.newFileSystem(file.toPath(),
                                  null).withCloseable { fs ->
            def entry = fs.getPath("/${propertiesFileToAddTo}")
            if (!Files.exists(entry)) {
                def propertiesFilesFound = Files.newDirectoryStream(fs.getPath('/')).withCloseable { directoryStream ->
                    directoryStream.findAll { p ->
                        !Files.isDirectory(p)
                    }.collect { p ->
                        // trim off the leading slash
                        p.toString()[1..-1]
                    }
                }
                throw new Exception("ERROR: Expected to find the properties file you wanted to modify, ${propertiesFileToAddTo}, in the ZIP archive, but did not! Only files seen were ${propertiesFilesFound}.")
            }
            def temp = fs.getPath('/tmp.properties')
            Files.move(entry,
                       temp)
            def modifiedStream = modifyProperties(temp.newInputStream(),
                                                  propertiesToAdd)
            Files.copy(modifiedStream,
                       entry)
            Files.delete(temp)
        }
        return file
    }

    static InputStream modifyProperties(InputStream input,
                                        Map propertiesToAdd) {
        def props = new Properties()
        props.load(input)
        props.putAll(propertiesToAdd)
        def bos = new ByteArrayOutputStream()
        props.store(bos,
                    'Modified by deployment process')
        new ByteArrayInputStream(bos.toByteArray())
    }
}
