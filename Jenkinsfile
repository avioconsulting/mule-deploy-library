pipeline {
    agent any

    environment {
        jdk = 'jdk8'
        mvn = 'Maven 3'
        version = "1.0.${env.BUILD_NUMBER}"
        standard_avio_mvn_settings = '3144821b-28b7-414d-99b5-10ce1bee8c09'
    }

    stages {
        stage('Build and unit test') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings,
                          // only want to capture artifact if we're deploying (see below)
                          options: [artifactsPublisher(disabled: true)]) {
                    mavenSetVersion(env.version)
                    // would usually use package but we have a multi module interdependent project
                    quietMaven 'clean install'
                }
                archiveArtifacts 'cli/target/appassembler/**/*'
            }
        }

        stage('Mutation Test/Site Report') {
            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings) {
                    dir('library') {
                        // simple way to guarantee the mutation test has run before generating the site
                        quietMaven 'clean test-compile org.pitest:pitest-maven:mutationCoverage groovydoc:generate site'
                        publishHTML([allowMissing         : false,
                                     alwaysLinkToLastBuild: true,
                                     keepAll              : true,
                                     reportDir            : 'target/site',
                                     reportFiles          : 'index.html',
                                     reportName           : 'Maven site'])
                    }
                }
            }
        }

        stage('Integration test') {
            environment {
                ANYPOINT_CREDS = credentials('anypoint-jenkins')
                ANYPOINT_CLIENT_CREDS = credentials('anypoint-client-creds-dev')
            }

            steps {
                withMaven(jdk: env.jdk,
                          maven: env.mvn,
                          mavenSettingsConfig: env.standard_avio_mvn_settings,
                          // only want to capture artifact if we're deploying (see below)
                          options: [artifactsPublisher(disabled: true)]) {
                    // don't need to run integration tests on other modules (and they are not defined)
                    dir('library') {
                        quietMaven "clean test-compile surefire:test@integration-test -Danypoint.username=${env.ANYPOINT_CREDS_USR} -Danypoint.password=${env.ANYPOINT_CREDS_PSW} -Danypoint.client.id=${env.ANYPOINT_CLIENT_CREDS_USR} -Danypoint.client.secret=${env.ANYPOINT_CLIENT_CREDS_PSW}"
                    }
                }
            }

            options {
                // we manipulate apps in the same environment, etc.
                lock('anypoint-integration-test')
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
