package com.avioconsulting.mule.cli

class ParamsWrapper {
    private final Map<String, String> props

    ParamsWrapper(Map<String, String> props) {
        this.props = props
    }

    def propertyMissing(String name) {
        if (!props.containsKey(name)) {
            def options = props.keySet()
            throw new Exception("Property ${name} was not found in your supplied params. Choices are ${options}")
        }
        props[name]
    }
}
