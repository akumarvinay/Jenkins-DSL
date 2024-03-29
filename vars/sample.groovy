def call(body)
{
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def applicationName = config.applicationName ?: 'SAMPLE'
	def buildNode = config.buildNode ?: 'master'
	def mvnGoals = config.mvnGoals
	currentBuild.result = 'SUCCESS'
	node("${buildNode}"){
		try
		{
			stage("Checkout")
			{
			//git branch: 'master', credentialsId: 'GitHub-Authentication', url: 'https://github.com/akumarvinay/SimpleWebApplication.git'
				step([$class: 'WsCleanup'])
				checkout scm
			}
			def M3_HOME = tool 'M3_HOME'
			stage("Build")
			{
				if ("${BRANCH_NAME}" == 'master')
				{
					sh """
						${M3_HOME}/bin/mvn clean install
					"""
					jacoco()
					junit testDataPublishers: [[$class: 'AttachmentPublisher']], testResults: 'target/surefire-reports/*.xml'
					HTMLPublisher
					{
						reportDir = config.reportDir
						reportFiles = config.reportFiles
						reportName = config.reportName
					}
				}
				else
				{
				 	sh """
						${M3_HOME}/bin/mvn ${mvnGoals}
					"""
				}
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
				applicationName = config.applicationName
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
			if ("${BRANCH_NAME}" == 'master')
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
		catch (Exception err)
		{			
            		echo 'Something failed, I should sound the klaxons!'
			currentBuild.result = 'FAILURE'
		}
		finally
		{
			stage("Email Notification")
			{
				sh """
				 echo "RESULT: ${currentBuild.result}"
				"""
				emailext body: 'Test', subject: 'Test Email', to: 'akumarvinay@gmail.com'
			}
		}
	}
}
