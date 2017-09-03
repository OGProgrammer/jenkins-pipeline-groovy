import hudson.model.*

node {
    deleteDir()

    def groovyScriptsPath = 'jenkins-pipeline-groovy'

    // This is some stupid shit but groovy pipeline won't see other groovy scripts so you gotta do this mickey mouse
    stage('bootstrap') {
        dir(groovyScriptsPath) {
            sh """
                git clone git@github.com:OGProgrammer/jenkins-pipeline-groovy.git --branch=master .
            """
        }
    }

    stage("prep") {
        // Clean up previous build
        deleteDir()

        // Get Parameters
        def env_name = "${env_name}"
        def app_name = "${app_name}" //git@github.com:OGProgrammer/test-app.git
        println "Preforming tests on [${env_name}] environemnt for [${app_name}] application."

        dir(groovyScriptsPath) {
            functions = load("functions.groovy")
        }

        git url: "${app_name}", branch: env_name
    }

    stage("php_lint") {
        sh 'find . -name "*.php" -print0 | xargs -0 -n1 php -l'
    }
}