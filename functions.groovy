import groovy.json.JsonSlurper

/**
 * Returns the manifest as an object for the given region and environment name.
 * Note: Ensure that you set the object to null before performing any operations with
 * the object returned by this method (known bug with JsonSlurper and pipeline).
 *
 * @param  string env_name The name of the environment.
 * @param  string region The name of the AWS region.
 * @return object An object representation of the manifest.
 */
def getInfrastructureManifest(env_name, region)
{
    def directory = getManifest("master")

    def fileContents = readFile "${directory}/infrastructure/${region}/${env_name}.json"

    return new JsonSlurper().parseText(fileContents)
}

/**
 * Clones down a given branch of your manifest repo.
 *
 * @param  string branch The name of the branch in your manifest repo.
 * @return string The directory path relative to your current path.
 */
def getManifest(branch) {
    def directory = "manifest-${branch}"
    if (fileExists(directory)) {
        return directory
    }

    dir(directory) {
        sh """
            git clone git@github.com:OGProgrammer/terraform-example-manifest.git --branch=${branch} .
        """
    }

    return directory
}

return this