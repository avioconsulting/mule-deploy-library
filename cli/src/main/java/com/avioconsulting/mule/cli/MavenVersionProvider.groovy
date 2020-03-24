package com.avioconsulting.mule.cli

import picocli.CommandLine

class MavenVersionProvider implements CommandLine.IVersionProvider {
    @Override
    String[] getVersion() throws Exception {
        def version = versionFromMavenProps ?: 'DEV-VERSION'
        return [
                "Mule Deployer ${version}".toString(),
                "Picocli ${CommandLine.VERSION}".toString(),
                'JVM ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})',
                'OS: ${os.name} ${os.version} ${os.arch}'
        ].toArray(new String[0])
    }

    private static String getVersionFromMavenProps() {
        def mavenProps = MavenVersionProvider.getResourceAsStream('/META-INF/maven/com.avioconsulting.mule/mule-deploy-cli/pom.properties')
        if (!mavenProps) {
            return null
        }
        def props = new Properties()
        props.load(mavenProps)
        return props['version']
    }
}
