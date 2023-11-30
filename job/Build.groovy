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
                            shell("""
                |set +e
                |echo ${App.tfsec_image} ${App.Name.toLowerCase()}
                """.stripMargin())
    }
            //echo "PATH=${TF_VER}"
        }
        publishers {
            jUnitResultArchiver {
                testResults('*-report.xml')
                skipMarkingBuildUnstable(true)
                allowEmptyResults(true)
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
