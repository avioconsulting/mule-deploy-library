pipeline {
    agent any

    environment {
        jdk = 'jdk8'
        mvn = 'Maven 3'
        version = "1.0.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Fetch dependencies') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn) {
                    // Used to clarify Maven phases a bit more than pure dependency as you go mode.
                    // For some reason the go offline goal doesn't fetch plugins right, so skip that but at least the project dependencies are fetched
                    sh 'mvn dependency:go-offline -DexcludeTypes=maven-plugin'
                }
            }
        }

        stage('Build/test') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn) {
                    sh "mvn org.codehaus.mojo:versions-maven-plugin:2.7:set -DnewVersion=${env.version} -DgenerateBackupPoms=false -DprocessAllModules=true"
                    // -B = batch mode, less noise
                    // -e shows detailed stack traces if errors happen
                    sh "mvn -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -e -B clean package"
                }
            }
        }

        stage('Deploy') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn) {
                    // -B = batch mode, less noise
                    // -e shows detailed stack traces if errors happen
                    sh "mvn -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -e -B clean deploy -DskipTests"
                    keepBuild()
                }
            }

            when {
                branch 'master'
            }
        }
    }

    options {
        buildDiscarder logRotator(numToKeepStr: '3')
        timestamps()
    }
}
