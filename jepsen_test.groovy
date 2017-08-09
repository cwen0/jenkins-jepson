def call(RELEASE_URL, JEPSEN_BRANCH, TIDB_BRANCH, TIKV_BRANCH, PD_BRANCH) {
    def BUILD_URL = 'git@github.com:pingcap/jepsen.git'
    //def UCLOUD_OSS_URL = "http://pingcap-dev.hk.ufileos.com"
    env.PATH = "/data/jenkins/bin:/bin:${env.PATH}"
    //def tidb_sha1, tikv_sha1, pd_sha1

    catchError {
        node('jepsen') {
            stage('prepare'){
                def ws = pwd()
                dir("${ws}/jepsen") {
                    // checkout scm
                    git credentialsId: 'github-iamxy-ssh', url: "$BUILD_URL", branch: "${JEPSEN_BRANCH}"
                    githash = sh(returnStdout: true, script: "git rev-parse HEAD").trim()
                }
                sh "docker cp ./jepsen jepsen-control:/jepsen"
            }
            stage('test') {
                sh "docker exec jepsen-control bash -c 'cd /jepsen/tidb/ && ./run.sh ${RELEASE_URL}'"
            }
        }
        result = "SUCCESS"
    }
    stage('Summary') {
        def duration = ((System.currentTimeMillis() - currentBuild.startTimeInMillis) / 1000 / 60).setScale(2, BigDecimal.ROUND_HALF_UP)
        def slackmsg = "[${env.JOB_NAME.replaceAll('%2F','/')}-${env.BUILD_NUMBER}] `${result}`" + "\n" +
                "Elapsed Time: `${duration}` Mins" + "\n"  +
                "tidb Branch: `${TIDB_BRANCH}`" + "\n" +
                "tikv Branch: `${TIKV_BRANCH}`" + "\n" +
                "pd   Branch: `${PD_BRANCH}`" + "\n"

        if (result != "SUCCESS") {
            slackSend channel: '#octopus', color: 'danger', teamDomain: 'pingcap', tokenCredentialId: 'slack-pingcap-token', message: "${slackmsg}"
        } else {
            slackSend channel: '#octopus', color: 'good', teamDomain: 'pingcap', tokenCredentialId: 'slack-pingcap-token', message: "${slackmsg}"
        }
    }
}

return this
