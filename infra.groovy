import hudson.model.*

node {

    stage('prep') {
        deleteDir()
        def env_name = "${env_name}"
        def region = "${region}"
        def action = "${action}"
        println "Preforming [${action}] with AWS infrastrucutre [${env_name}] in the [${region}] region."

        functions = load("${WORKSPACE}/functions.groovy")

        // Build the terraform vars
        tfVarsFileData = ''
        def newLine = System.getProperty('line.separator')

        // Jenkins parameters set manually
        tfVarsFileData += "env_name = \"${env_name}\"${newLine}"
        tfVarsFileData += "region = \"${region}\"${newLine}"

        // Get all the terraform variables for our infrastructure from a manifest
        def infrastructureManifest = functions.getInfrastructureManifest(env_name, region)
        for (data in infrastructureManifest) {
            tfVarsFileContents += "${data.key} = \"${data.value}\"${lineSeparator}"
        }
        infrastructureManifest = null

    }

    stage ('plan') {
        git url: "git@github.com:OGProgrammer/terraform-aws-ecs-infrastructure.git", branch: "master"
        writeFile file: 'variables.tfvars', text: tfVarsFileData
        sh "./tf-${action}.sh variables.tfvars"
    }
}