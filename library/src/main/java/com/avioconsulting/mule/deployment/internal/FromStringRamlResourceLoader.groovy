package com.avioconsulting.mule.deployment.internal

import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.mule.apikit.loader.ResourceLoader

class FromStringRamlResourceLoader implements ResourceLoader {
    private final List<RamlFile> ramlFiles

    FromStringRamlResourceLoader(List<RamlFile> ramlFiles) {
        this.ramlFiles = ramlFiles
    }

    @Override
    URI getResource(String path) {
        new URI(path)
    }

    @Override
    InputStream getResourceAsStream(String relativePath) {
        def file = ramlFiles.find { r ->
            r.fileName == relativePath
        }
        if (!file) {
            return null
        }
        new ByteArrayInputStream(file.contents.bytes)
    }
}
