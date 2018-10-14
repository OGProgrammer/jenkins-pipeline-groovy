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
        def env_name = "${env_name}"
        def app_name = "${app_name}"
        def region = "${region}"
        def action = "${action}"
        println "Preforming [${action}] on the [env:${env_name}, region:${region}] AWS ECS Cluster with the [${app_name}] Service."

        // Build the terraform vars
        tfvars_file_data = ''
        def newLine = System.getProperty('line.separator')

        // Jenkins parameters set manually
        tfvars_file_data += "env_name = \"${env_name}\"${newLine}"
        tfvars_file_data += "app_name = \"${app_name}\"${newLine}"
        tfvars_file_data += "region = \"${region}\"${newLine}"

        // Get s3 prefix for the state buckets
        def terraformManifest = functions.getTerraformManifest()
        for (data in terraformManifest) {
            tfvars_file_data += "${data.key} = \"${data.value}\"${newLine}"
        }
        terraformManifest = null

        // Get all the terraform variables for our infrastructure manifest
        def infrastructureManifest = functions.getInfrastructureManifest(env_name, region)
        for (data in infrastructureManifest) {
            tfvars_file_data += "${data.key} = \"${data.value}\"${newLine}"
        }
        infrastructureManifest = null

        // Get all the terraform variables for our application manifest
        def applicationManifest = functions.getApplicationManifest(env_name, app_name)
        // Get what docker tag to go deploy
        def docker_tag = applicationManifest.docker_tag
        for (data in applicationManifest) {
            // Put everything but docker tag in, we need to check later
            if (data.key != "docker_tag") {
                tfvars_file_data += "${data.key} = \"${data.value}\"${newLine}"
            }
        }
        applicationManifest = null

        // Now lets check for docker tag and put into tfvars file
        if (!docker_tag) {
            latestBuildInfo = functions.getLatestBuild(env_name, app_name)
            docker_tag = latestBuildInfo.build_tag
        }
        tfvars_file_data += "docker_tag = \"${docker_tag}\"${newLine}"
        latestBuildInfo = null

    }

    stage ('deploy') {
        dir("terraform-aws-ecs-service") {
            sh "git clone git@github.com:OGProgrammer/terraform-aws-ecs-service.git --branch=master ."
            writeFile file: 'variables.tfvars', text: tfvars_file_data
            sh "./tf-${action}.sh variables.tfvars"
        }
    }
}