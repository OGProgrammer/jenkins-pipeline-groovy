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

        // Get variables from the application manifest
        def applicationManifest = functions.getApplicationManifest(env_name, app_name)
        def app_repo = applicationManifest.app_repo
        def docker_repo = applicationManifest.docker_repo
        image_name = applicationManifest.image_name
        applicationManifest = null

        // Other parameters needed
        image_tag = "${env.BUILD_ID}-${env_name}"
        tagged_image_name = "${image_name}:${image_tag}"
        docker_build_file = "dockerfiles/${env_name}/Dockerfile"

        println "Building the app [name:${app_name}, repo:${app_repo}] with the docker image [name:${image_name}, repo:${docker_repo}] on the [${env_name}] environemnt."

        git poll: true, url: "${docker_repo}", branch: "${env_name}"

        // Clone the app repo into the directory that will be packed into our docker image
        dir("files/var/www/${app_name}") {
            git url: "${app_repo}", branch: "${env_name}"
        }
    }

    stage('build') {
        sh "docker build -f ${docker_build_file} -t ${tagged_image_name} ./"
    }

    stage('publish') {
        // Push the docker image tags up to docker hub
        sh """
            docker tag ${tagged_image_name} ${image_name}:latest
            docker push ${tagged_image_name}
            docker push ${image_name}:latest
        """
        // Save the build id (version) tag to our manifest repo
        functions.setLatestBuild(env_name, app_name, image_name, image_tag)
    }

}