pipeline {
    agent any

    stages {
        stage('Build and test') {
            steps {
                withMaven(jdk: 'jdk8', maven: 'Maven 3') {
                    sh 'mvn clean test'
                }
            }
        }
    }
}
