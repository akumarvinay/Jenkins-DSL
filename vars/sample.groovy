def call(body)
{
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
        def applicationName = config.applicationName ?: 'SAMPLE'
	node()
	{
		stage("sample")
		{
		 sh """
			echo "${applicationName} is This"
                    """
		}
	}
}
