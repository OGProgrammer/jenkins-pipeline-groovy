import hudson.model.*

node {
    deleteDir()

    def groovy_scripts_path = 'jenkins-pipeline-groovy'

    stage('bootstrap') {
        dir(groovy_scripts_path) {
            sh """
                git clone git@github.com:OGProgrammer/jenkins-pipeline-groovy.git --branch=master .
            """
        }
        dir(groovy_scripts_path) {
            functions = load("functions.groovy")
        }
    }

    stage("prep") {
        // Get Jenkins Parameters
        def env_name = "${env_name}"
        def app_name = "${app_name}"

        // Get the app_repo from the application manifest
        def applicationManifest = functions.getApplicationManifest(env_name, app_name)
        def app_repo = applicationManifest.app_repo
        applicationManifest = null

        println "Testing the app [name:${app_name}, repo:${app_repo}] on the [${env_name}] environemnt."

        git poll: true, url: "${app_repo}", branch: "${env_name}"
    }

    stage("php_lint") {
        sh 'find . -name "*.php" -print0 | xargs -0 -n1 php -l'
    }
}