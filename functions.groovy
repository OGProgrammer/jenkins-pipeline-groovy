import groovy.json.JsonSlurper

/**
 * Note: There is a known bug with JsonSlurper where you must set these objects to null before getting another object.
 */

/**
 * Returns the latest build information.
 *
 * @param string environment The target environment that build data is being retrieved for. Ex. dev, prod, etc.
 */
def getLatestBuild(environment) {
    def directory = getManifest("master")

    def latestBuildFile = "latest-build-${environment}.json"

    def buildData = null;
    dir(directory) {
        buildData = readFile latestBuildFile
    }

    return new JsonSlurper().parseText(buildData)
}

/**
 * Updates the latest-build-*.json file with the last docker image build tag
 *
 * @param string environment The target environment that build data is being retrieved for. Ex. dev, prod, etc.
 * @param string build_name Example docker image name in docker hub ogprogrammer/test-app
 * @param string build_tag Example 1.0.0
 */
def setLatestBuild(environment, build_name, build_tag) {
    def directory = getManifest("master")

    dir(directory) {
        // Prevent someone else from pushing and messing up the build
        sh """
            git pull origin master
        """

        def latestBuildFile = "latest-build-${environment}.json"

        writeFile(
                file: latestBuildFile,
                text: """
{
    \"build_name\": \"${build_name}\",
    \"build_tag\": \"${build_tag}\",
    \"build_id\": \"${build_name}:${build_tag}\"
}
            """
        )

        // Commit and push this back up, make sure you have permissions
        sh """
            git add ${latestBuildFile}
            git commit -m \"AUTOCOMMIT BUILD JOB - UPDATED ${latestBuildFile} \"
            git push origin master
        """
    }
}

/**
 * Returns the manifest as an object for the given region and environment name.
 *
 * @param  string env_name The name of the environment.
 * @param  string region The name of the AWS region.
 * @return object An object representation of the manifest.
 */
def getInfrastructureManifest(env_name, region)
{
    def directory = getManifest("master")

    def buildData = readFile "${directory}/infrastructure/${region}/${env_name}.json"

    return new JsonSlurper().parseText(buildData)
}

/**
 * Returns the manifest as an object for the given region and environment name.
 */
def getDockerManifest()
{
    def directory = getManifest("master")
    def buildData = readFile "docker.json"
    return new JsonSlurper().parseText(buildData)
}

/**
 * Clones down a given branch of your manifest repo.
 *
 * An environment variable for MANIFEST_REPO must be set!
 *
 * @param  string branch The name of the branch in your manifest repo.
 * @return string The directory path relative to your current path.
 */
def getManifest(branch) {
    def directory = "manifest-${branch}"
    def manifest_repository = "${MANIFEST_REPO}" //git@github.com:OGProgrammer/terraform-example-manifest.git

    if (fileExists(directory)) {
        return directory
    }

    dir(directory) {
        sh """
            git clone ${manifest_repository} --branch=${branch} .
        """
    }

    return directory
}

return this