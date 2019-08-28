def call(body)
{
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def applicationName = config.applicationName ?: 'SAMPLE'
	def buildNode = config.buildNode ?: 'master'
	def mvnGoals = config.mvnGoals
	node("${buildNode}"){
		stage("Checkout")
		{
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
			jacoco()
			junit testDataPublishers: [[$class: 'AttachmentPublisher']], testResults: 'target/surefire-reports/*.xml'
		}
		// def SONAR_TOOL = tool 'SonarScanner'
		stage("Sonar Scan")
		{
			sonarscan 
			{
				applicationName = 'tomcat-application'
				projectName = 'myfirstApp'
    	        projectKey = 'myproject'
    			projectVersion = '1.0'
    			sonarLanguage = 'java'
    			sonarSources = 'src'
			}
		}
		stage("Sonar QualityGate Check")
		{
			timeout(time: 1, unit: 'HOURS')
			{
				def qualityGate = waitForQualityGate()
				if (qualityGate.status != 'OK')
				{
					error "Pipeline aborted due to quality gate failure: ${qualityGate.status}"
				}
			}
		}
		stage("DockerBuild and Publish")
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
		stage("Kubernetes deployment to Stage/Dev")
		{			    
        	// https://jenkins.io/doc/book/pipeline/docker/
        	docker.image('ubuntu').inside
        	{
            		kubernetesDeploy configs: '*.yml', kubeConfig: [path: ''], kubeconfigId: 'KubeAuthentication', secretName: '', ssh: [sshCredentialsId: '*', sshServer: ''], textCredentials: [certificateAuthorityData: '', clientCertificateData: '', clientKeyData: '', serverUrl: 'https://']
        	}
    	}
		stage("Automation TestSuites Execution")
		{
			sh """
				echo "Invoking Automation test case execution"
			"""
		}
		def branch = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD')
		echo branch
		if (branch == 'master')
	         {
			stage("Manual Deploy Validation")
			{
				timeout(time: 1, unit: 'HOURS') 
				{
					input id: 'UserInput', message: 'Is OK to proceed', ok: 'Deploy to Prod', submitter: 'akumarvinay@gmail.com'
				}
			}	
			stage("Prod Deployment")
			{

			}
		}
	}
}
