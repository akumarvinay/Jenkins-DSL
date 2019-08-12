def call(body)
{
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
        def applicationName = config.applicationName ?: 'SAMPLE'
	def buildNode = config.buildNode
	def mvnGoals = config.mvnGoals
	node("${buildNode}"){
		stage("Checkout"){
		 //git branch: 'master', credentialsId: 'GitHub-Authentication', url: 'https://github.com/akumarvinay/SimpleWebApplication.git'
		 cleanWS()
		 checkout scm
		}
		def M3_HOME = tool 'M3_HOME'
		stage("Build")
		{
			sh """
		   	${M3_HOME}/bin/mvn ${mvnGoals}
		   	"""
		}
		stage("DockerBuild")
		{
			docker.withRegistry('', 'DockerCred')
			{
				docker.withTool("docker")
		   		{
					def base = docker.build("akumarvinay/${applicationName}")
					sh "docker images"
					base.push("${BUILD_NUMBER}")
					
				}
		 
			}
		}
	}
}
