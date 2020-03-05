pipeline {
    agent any

    environment {
        jdk = 'jdk8'
        mvn = 'Maven 3'
        version = "1.0.${env.BUILD_NUMBER}"
        standard_avio_mvn_settings = '3144821b-28b7-414d-99b5-10ce1bee8c09'
    }

    stages {
        stage('Fetch dependencies') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings,
                          // Don't want to capture artifacts w/ un-set version (see build/test stage)
                          options: [artifactsPublisher(disabled: true)]) {
                    // Used to clarify Maven phases a bit more than pure dependency as you go mode.
                    mavenFetchDependencies()
                }
            }
        }

        stage('Build/test') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings,
                          // only want to capture artifact if we're deploying (see below)
                          options: [artifactsPublisher(disabled: true)]) {
                    mavenSetVersion(env.version)
                    quietMaven 'clean package'
                }
            }
        }

        stage('Deploy') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings) {
                    quietMaven 'clean deploy -DskipTests'
                    // keeps buildDiscarder from getting rid of stuff we've published
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

    post {
        always {
            cleanWs()
        }
        failure {
            handleError([message: 'Build failed'])
        }
        unstable {
            handleError([message: 'Build failed'])
        }
    }
}
