def buildAndPushDockerImage(String dockerBuildArgRegistryHost, String environment, String domain, String dockerBuildArgImageTag) {
  log.console("building ${domain} ${environment} domain image: ${env.RELEASE_DESCRIPTION}")
  log.console("dockerBuildArgRegistryHost:, ${dockerBuildArgRegistryHost}")
  log.console("dockerBuildArgImageTag:, ${dockerBuildArgImageTag}")

  def environmentAbbreviations = [acceptance: 'acc', production: 'prod']

  try {
    docker.withRegistry(env.DOCKER_REGISTRY_HOST, env.DOCKER_REGISTRY_AUTH) {
      def image = docker.build(
        "ois/signals-${domain}:${env.BUILD_NUMBER}", [
          '--shm-size 1G ',
          "--build-arg BUILD_ENV=${environmentAbbreviations[environment]} ",
          "--build-arg DOCKER_REGISTRY=${dockerBuildArgRegistryHost} ",
          "--build-arg DOCKER_IMAGE_TAG=${dockerBuildArgImageTag} ",
          "${env.WORKSPACE}/signalen/domains/${domain}"
        ].join(' ')
      )

      image.push()
      image.push(environment)
    }
  } catch (Throwable throwable) {
    log.error("build of signals-${domain} Docker image failed ${env.RELEASE_DESCRIPTION}")
    // slack.error("build of signals-${domain} ${releaseDisplayName} Docker image failed")
    throw throwable
  }
}

def call(String dockerBuildArgRegistryHost, String environment, def domains) {
  log.console("dockerBuildArgRegistryHost, environment: ${dockerBuildArgRegistryHost} ${environment}")

  def steps = [:]

  domains.each {domain -> steps["BUILD_IMAGE_${domain}_${environment}".toUpperCase()] = {
    buildAndPushDockerImage dockerBuildArgRegistryHost, environment, domain
  }}

  parallel steps
}
