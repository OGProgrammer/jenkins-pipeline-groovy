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
        // Get Parameters
        /** An environment variable for TEST_APP_REPO must be set! */
        def env_name = "${env_name}"
        def app_repo = "${TEST_APP_REPO}" // git@github.com:OGProgrammer/test-app.git
        println "Preforming tests on [${env_name}] environemnt for [${app_repo}] application."

        dir(groovyScriptsPath) {
            functions = load("functions.groovy")
        }

        git url: "${app_repo}", branch: env_name
    }

    stage("php_lint") {
        sh 'find . -name "*.php" -print0 | xargs -0 -n1 php -l'
    }
}