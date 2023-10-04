package com.avioconsulting.mule.deployment.api.models.deployment

class ApplicationName {

    private String baseAppName
    private Boolean usePrefix
    private Boolean useSuffix
    private String prefix
    private String suffix
    private String normalizedAppName
    private static final String EMPTY_STRING=''
    private static final MAX_SIZE_APPLICATION_NAME = 42

    ApplicationName(String baseAppName, Boolean usePrefix, Boolean useSuffix, String prefix, String suffix) {
        this.baseAppName = baseAppName
        this.usePrefix = usePrefix
        this.useSuffix = useSuffix
        this.prefix = prefix
        this.suffix = suffix
        getNormalizedAppName()
    }

    String getBaseAppName(){
        return this.baseAppName
    }

    String getNormalizedAppName() {
        runGuards()
        if(normalizedAppName == null){
            if(!useSuffix && !prefix){
                normalizedAppName = baseAppName.toLowerCase()
            } else {
                normalizedAppName = buildAppNameByOptions().toLowerCase()
            }
        }
        runNameLengthGuard(normalizedAppName)
        return normalizedAppName
    }

    private void runGuards(){
        assert (baseAppName != null && !baseAppName.isBlank() && !baseAppName.contains(" ")) : "you should specify an non-empty baseAppName. It shouldn't contain spaces as well"
        if (usePrefix == true) assert (prefix != null && !prefix.isBlank() && !prefix.contains(" "))  : "as you going to use a prefix, you should specify a non-empty one. Prefix should not contain spaces"
        if (useSuffix == true) assert (suffix != null && !suffix.isBlank() && !suffix.contains(" "))  : "as you going to use a suffix, you should specify a non-empty one. Prefix should not contain spaces"

    }

    private void runNameLengthGuard(String normalizedAppName){
        if (normalizedAppName.length() >= MAX_SIZE_APPLICATION_NAME){
            throw new Exception("Maximum size of application name is ${MAX_SIZE_APPLICATION_NAME} and the provided name has ${normalizedAppName.length()} characters")
        }
    }


    private String buildAppNameByOptions(){
        // calculate prefix
        String updatedPrefix=EMPTY_STRING
        if(usePrefix){
            updatedPrefix = prefix
        }
        // calculate suffix
        String updatedSuffix=EMPTY_STRING
        if(useSuffix){
            updatedSuffix = suffix
        }
        def nameParts = [updatedPrefix, baseAppName, updatedSuffix]
        nameParts.findAll {it != null && !it.isBlank()}.join("-")
    }

}
