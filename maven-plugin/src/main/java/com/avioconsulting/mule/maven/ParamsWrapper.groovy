package com.avioconsulting.mule.maven

class ParamsWrapper {
    private final Properties properties

    ParamsWrapper(Properties properties) {
        this.properties = properties
    }

    def propertyMissing(String name) {
        if (!properties.containsKey(name)) {
            def options = properties.keySet()
            throw new Exception("Property ${name} was not found in your supplied params. Choices are ${options}")
        }
        properties[name]
    }
}
