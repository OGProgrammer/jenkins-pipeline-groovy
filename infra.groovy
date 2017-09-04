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

    stage('prep') {
        def env_name = "${env_name}"
        def region = "${region}"
        def action = "${action}"
        println "Preforming [${action}] with AWS infrastrucutre [${env_name}] in the [${region}] region."

        // Build the terraform vars
        tfvars_file_data = ''
        def newLine = System.getProperty('line.separator')

        // Jenkins parameters set manually
        tfvars_file_data += "env_name = \"${env_name}\"${newLine}"
        tfvars_file_data += "region = \"${region}\"${newLine}"

        // Get all the terraform variables for our infrastructure from a manifest
        def infrastructureManifest = functions.getInfrastructureManifest(env_name, region)
        for (data in infrastructureManifest) {
            tfvars_file_data += "${data.key} = \"${data.value}\"${newLine}"
        }
        infrastructureManifest = null

        // Get docker manifest vars
        def dockerManifest = functions.getDockerManifest()
        for (data in dockerManifest) {
            tfvars_file_data += "${data.key} = \"${data.value}\"${newLine}"
        }
        dockerManifest = null

    }

    stage ('plan') {
        git url: "git@github.com:OGProgrammer/terraform-aws-ecs-infrastructure.git", branch: "master"
        writeFile file: 'variables.tfvars', text: tfvars_file_data
        sh "./tf-${action}.sh variables.tfvars"
    }
}