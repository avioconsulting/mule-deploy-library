package com.avioconsulting.mule.deployment.internal

import com.avioconsulting.mule.deployment.internal.models.RamlFile
import org.raml.v2.api.loader.DefaultResourceLoader
import org.raml.v2.api.loader.ResourceUriCallback

/**
 * For some reason, the RAML parser does not close these InputStreams by default.
 * On Windows, this is a problem because MUnit uses a temporary directory to run
 * tests that is subsequently cleaned out and if the stream isn't closed,
 * then the file will remain locked and MUnit will be unable to clean out the
 * directory.
 *
 * This way, we keep track of the InputStreams we open and close them after
 * we parse the RAML. APIKit does a lot of complex stuff with this so it didn't
 * make sense to try and emulate exactly what it does.
 */
class EnsureWeCloseRamlLoader extends DefaultResourceLoader {
    private final List<RamlFile> ramlFiles

    EnsureWeCloseRamlLoader(List<RamlFile> ramlFiles) {
        this.ramlFiles = ramlFiles
    }

    @Override
    InputStream fetchResource(String resourceName, ResourceUriCallback callback) {
        def file = ramlFiles.find { r ->
            r.fileName == resourceName
        }
        if (!file) {
            return null
        }
        new ByteArrayInputStream(file.contents.bytes)
    }
}
