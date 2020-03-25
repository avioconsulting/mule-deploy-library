package com.avioconsulting.mule.deployment.dsl

class LowerCaseEnumWrapper {
    private final Class anEnum

    LowerCaseEnumWrapper(Class anEnum) {
        this.anEnum = anEnum
    }

    def propertyMissing(String name) {
        def values = anEnum.values() as Object[]
        values.find { value ->
            (value.name() as String).toUpperCase() == name.toUpperCase()
        }
    }
}
