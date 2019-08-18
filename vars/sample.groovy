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
		 step([$class: 'WsCleanup'])
		 checkout scm
		}
		def M3_HOME = tool 'M3_HOME'
		stage("Build")
		{
			sh """
		   	${M3_HOME}/bin/mvn ${mvnGoals}
		   	"""
		}
		def SONAR_TOOL = tool 'SonarScanner'
		stage("Sonar Scan")
		{
			withSonarQubeEnv('SONAR_POC') 
			{ // If you have configured more than one global server connection, you can specify its name
      			sh """
				  ${M3_HOME}/bin/mvn clean package sonar:sonar
				"""
    		}
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
