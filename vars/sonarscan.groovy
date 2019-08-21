def call(body)
{
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def projectName = config.projectName
    def projectKey = config.projectKey
    def projectVersion = config.projectVersion
    def sonarLanguage = config.sonarLanguage
    def sonarSources = config.sonarSources
    def SONAR_TOOL = tool 'SonarScanner'
    withSonarQubeEnv('SONAR_POC') 
	{ // If you have configured more than one global server connection, you can specify its name
      	sh """
		    ${SONAR_TOOL}/bin/sonar-scanner -Dsonar.projectKey=${projectKey} \
		          -Dsonar.projectName=${projectName} \
				  -Dsonar.projectVersion=${projectVersion} \
				  -Dsonar.sources=${sonarSources} \
				  -Dsonar.language=${sonarLanguage}
		"""
    }