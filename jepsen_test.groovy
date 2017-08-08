def call(JEPSEN_BRANCH) {
    def BUILD_URL = 'git@github.com:UncP/jepsen.git'
    def UCLOUD_OSS_URL = "http://pingcap-dev.hk.ufileos.com"
    env.PATH = "/home/jenkins/bin:/bin:${env.PATH}"
    def tidb_sha1, tikv_sha1, pd_sha1

    catchError {
        node('jepsen') {
            stage('prepare'){
               def ws = pwd()
               dir("${ws}/jepsen") {
                   // checkout scm
                   git credentialsId: 'github-iamxy-ssh', url: "$BUILD_URL", branch: "${JEPSEN_BRANCH}"
                   githash = sh(returnStdout: true, script: "git rev-parse HEAD").trim()

                   sh "cd docker && bash run.sh"
               }
            }
            stage('test') {
                echo "success"
            }
        }
    }
}

return this
