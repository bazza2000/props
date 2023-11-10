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
        label("${env_name.toLowerCase()}-slave")
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
            versionNumberBuilder {
                projectStartDate('')
                versionNumberString('${BUILD_DATE_FORMATTED, "yyyyMMddHHmmss"}')
                worstResultForIncrement('NOT_BUILT')
                useAsBuildDisplayName(true)
                environmentVariableName('BUILD_VERSION')
                environmentPrefixVariable('')
                buildsToday('')
                buildsThisWeek('')
                buildsThisMonth('')
                buildsThisYear('')
                buildsAllTime('')
            }
        }
        environment {
            TF_VER = sh(script: 'cat .terraform_version', , returnStdout: true).trim()
        }
        steps {
            environmentVariables {
                 propertiesFile('.terraform_version')
            }
        }
        publishers {
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
