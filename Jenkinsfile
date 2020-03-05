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
                    mavenFetchDependencies()
                }
            }
        }

        stage('Build/test') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn) {
                    mavenSetVersion(env.version)
                    quietMaven 'clean package'
                }
            }
        }

        stage('Deploy') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn) {
                    quietMaven 'clean deploy -DskipTests'
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
