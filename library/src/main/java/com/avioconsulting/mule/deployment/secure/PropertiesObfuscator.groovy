package com.avioconsulting.mule.deployment.secure

class PropertiesObfuscator {

    private static PROPERTIES_TO_MATCH_EXP = "\\b(^.*key.*\$|^.*secret.*\$|^.*password.*\$|^.*client_id.*|^.*clientId.*|^.*pwd.*\$)\\b"
    public static MASKING_STRING = "**************"

    private static Map<String,String> obfuscateProperties(Map<String,String> mapToObfuscate){
        Map<String,String> obfuscatedMap = new LinkedHashMap<>()
        mapToObfuscate.keySet().forEach {key ->
            if(key.toLowerCase().matches(PROPERTIES_TO_MATCH_EXP.toLowerCase())){
                obfuscatedMap.put(key,MASKING_STRING)
            }else{
                obfuscatedMap.put(key,mapToObfuscate.get(key))
            }
        }
        obfuscatedMap
    }

    static Map<String,String> obfuscateMap(Map<String,String> mapToObfuscate, String propertiesEntryKey){
        def appInfo = mapToObfuscate
        if(appInfo.containsKey(propertiesEntryKey)){
            Map<String,String> propsToObfuscate = appInfo[propertiesEntryKey]
            appInfo.remove(propertiesEntryKey)
            appInfo[propertiesEntryKey] = PropertiesObfuscator.obfuscateProperties(propsToObfuscate)
        }
        appInfo
    }

}