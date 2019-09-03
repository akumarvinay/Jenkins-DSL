# Jenkins-DSL
DSL Library for Jenkins builds

Info about shared Libraries
  https://jenkins.io/doc/book/pipeline/shared-libraries/

Pipeline Basic Steps:
  https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#code-timeout-code-enforce-time-limit

Example for Junit:
https://www.mkyong.com/maven/jacoco-java-code-coverage-maven-example/
https://github.com/LableOrg/java-maven-junit-helloworld

# commit-msg hook validation 
#!/bin/bash; C:/Program\ Files/Git/usr/bin/sh.exe

echo "$(cat $1)"
echo "Hello World"

reg_ex="AP[0-9]+-[0-9]+"

if ! grep -iEq "$reg_ex" $1 ; then
        echo "You are not giving proper commmit message in the format AP1111-111"
        exit 1
else
        echo "Comgrats"
fi

