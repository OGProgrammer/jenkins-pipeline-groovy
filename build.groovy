import hudson.model.*

node {
    deleteDir()

    def groovyScriptsPath = 'jenkins-pipeline-groovy'

    stage('bootstrap') {
        dir(groovyScriptsPath) {
            sh """
                git clone git@github.com:OGProgrammer/jenkins-pipeline-groovy.git --branch=master .
            """
        }
        dir(groovyScriptsPath) {
            functions = load("functions.groovy")
        }
    }

    stage("prep") {
        // Jenkins parameters
        def env_name = "${env_name}"
        def image_name = "${image_name}" // thisisjustatest/test-app
        def docker_repo = "${docker_repo}" // git@github.com:OGProgrammer/docker-test-app.git
        def app_repo = "${TEST_APP_REPO}" // git@github.com:OGProgrammer/test-app.git

        // Other parameters needed
        imageName = "${image_name}"
        imageTag = "${env.BUILD_ID}-${env_name}"
        imageNameAndTag = "${imageName}:${imageTag}"
        dockerBuildFile = "dockerfiles/${env_name}/Dockerfile"

        println "Preforming a docker image [${image_name}] with the docker repo [${docker_repo}] and the [${app_repo}] application for the [${env_name}] environemnt."

        git poll: true, url: "${docker_repo}", branch: "master"

        // Clone the app repo into the directory that will be packed into our docker image
        dir("files/var/www/test-app") {
            git url: "${app_repo}", branch: "master"
        }
    }

    stage('build') {
        sh "docker build -f ${dockerBuildFile} -t ${imageNameAndTag} ./"
    }

    stage('publish') {
        // Push the docker image tags up to docker hub
        sh """
            docker tag ${imageNameAndTag} ${imageName}:latest
            docker push $imageNameAndTag
            docker push ${imageName}:latest
        """
        // Save the build id (version) tag to our manifest repo
        functions.setLatestBuild(env_name, imageName, imageTag)
    }

}