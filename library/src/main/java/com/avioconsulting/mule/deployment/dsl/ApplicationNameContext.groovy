package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName

class ApplicationNameContext extends BaseContext{

    String baseAppName
    Boolean usePrefix
    Boolean useSuffix
    String prefix
    String suffix


    ApplicationName createApplicationName(){
        return new ApplicationName(baseAppName,usePrefix,useSuffix,prefix,suffix)
    }

    @Override
    List<String> findOptionalProperties() {
        return null
    }
}
