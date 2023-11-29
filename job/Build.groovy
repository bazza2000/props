def currentFolder = new File(__FILE__).getParent()
def parentFolder = new File(currentFolder).getParent()
def Team = new ConfigSlurper().parse(new File(parentFolder + "/common.conf").toURL())
def App = new ConfigSlurper().parse(new File(currentFolder + "/common.conf").toURL())

def GenerateJob(def Team, def App, def env_name, def job_name) {
    def jobname = Team.Name + "-" + env_name + "-" + App.Name + "-" + job_name

    def aws_region = "eu-west-2"
    def account_id = Team.AccountID[env_name]
    def ecr_repo = "${account_id}.dkr.ecr.${aws_region}.amazonaws.com/${App.Name.toLowerCase()}"

    job(jobname) {
        logRotator {
            numToKeep(30)
            daysToKeep(30)
        }      
        scm {
            git {
                remote {
                    url(App.gitUrl)
                    credentials(App.gitCredentials)
                }
                branch("main")
            }
        }
        triggers {
            scm("* * * * *")
        }
        wrappers {
            timestamps()
        }

        steps {
            environmentVariables {
       shell('echo TF_VER=`cat .terraform-version` > .jobvars')
       propertiesFile('.jobvars')
                shell('''# TERRAFORM FORMAT
docker run --rm hashicorp/terraform:${TF_VER} fmt
# TERRAFORM DOC
docker run --rm -v $(pwd):/data cytopia/terraform-docs terraform-docs-012 --sort-by required md . > README.md
# TERRAFORM VALIDATE
docker run --rm hashicorp/terraform:${TF_VER} validate . --no-color
# TERRAFORM LINT
docker run --rm -v $(pwd):/data -t ghcr.io/terraform-linters/tflint --format=junit --force > tflint-report.xml
# TERRAFORM SECURITY CHECK - TFSEC
docker run --rm -v "$(pwd):/src" aquasec/tfsec /src --soft-fail -f junit > tfsec-report.xml
# TERRAFORM SECURITY CHECK - CHECKOV
docker run --rm -v "$(pwd):/tf" --workdir /tf bridgecrew/checkov --directory /tf --soft-fail -o junitxml > checkov-report.xml

''')
    }
            //echo "PATH=${TF_VER}"
        }
        publishers {
            archiveJunit('*-report.xml') {
                allowEmptyResults()
            }
            git {
                pushOnlyIfSuccess()
                tag('origin','${TF_VER}-${BUILD_VERSION}') {
                    message('Build by Jenkins')
                    create()
                }
            }
        }
    }
}

def job_name = "Build"

["BUILD"].each {
  def env_name = it
  GenerateJob(Team, App, env_name, job_name)
}
