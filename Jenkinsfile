pipeline {
   agent none
   environment {
       field = 'some'
   }
   stages {
       stage ('Preparation') {
           environment {
               TF_VER = sh(script: 'cat .terraform-version', , returnStdout: true).trim()
           }
           steps {
               echo "Hello world"
               echo "PATH=${TF_VER}"
               sh 'echo "JP=$TF_VER"'
          }
      }
   }
}
