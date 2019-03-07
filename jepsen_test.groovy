def call(RELEASE_URL, JEPSEN_BRANCH, TIDB_BRANCH, TIKV_BRANCH, PD_BRANCH, TIMEOUT) {
    def BUILD_URL = 'git@github.com:jepsen-io/jepsen.git'
    env.PATH = "/data/jenkins/bin:/bin:${env.PATH}"

    catchError {
        node('jepsen') {
            stage('prepare'){
                def ws = pwd()

                sh "docker exec jepsen-n1 bash -c 'rm -rf /opt/tidb/*.log'"
                sh "docker exec jepsen-n2 bash -c 'rm -rf /opt/tidb/*.log'"
                sh "docker exec jepsen-n3 bash -c 'rm -rf /opt/tidb/*.log'"
                sh "docker exec jepsen-n4 bash -c 'rm -rf /opt/tidb/*.log'"
                sh "docker exec jepsen-n5 bash -c 'rm -rf /opt/tidb/*.log'"

                dir("${ws}/jepsen") {
                    // checkout scm
                    git credentialsId: 'github-iamxy-ssh', url: "$BUILD_URL", branch: "${JEPSEN_BRANCH}"
                    githash = sh(returnStdout: true, script: "git rev-parse HEAD").trim()
                }
                sh "docker cp ${ws}/jepsen jepsen-control:/"
            }
            stage('test') {
                sh "docker exec jepsen-control bash -c 'cd /jepsen/tidb/ && timeout --preserve-status ${TIMEOUT} ./run.sh ${RELEASE_URL}'"
            }
            stage('clean') {
                sh "docker exec jepsen-control bash -c 'cd /jepsen/tidb/ && rm -rf store'"
                sh "docker exec jepsen-n1 bash -c 'rm -rf /tmp/jepsen/*'"
                sh "docker exec jepsen-n2 bash -c 'rm -rf /tmp/jepsen/*'"
                sh "docker exec jepsen-n3 bash -c 'rm -rf /tmp/jepsen/*'"
                sh "docker exec jepsen-n4 bash -c 'rm -rf /tmp/jepsen/*'"
                sh "docker exec jepsen-n5 bash -c 'rm -rf /tmp/jepsen/*'"
            }
        }
        currentBuild.result = "SUCCESS"
    }
    stage('Summary') {
        def duration = ((System.currentTimeMillis() - currentBuild.startTimeInMillis) / 1000 / 60).setScale(2, BigDecimal.ROUND_HALF_UP)
        def slackmsg = "[${env.JOB_NAME.replaceAll('%2F','/')}-${env.BUILD_NUMBER}] `${currentBuild.result}`" + "\n" +
                "Elapsed Time: `${duration}` Mins" + "\n"  +
                "tidb Branch: `${TIDB_BRANCH}`" + "\n" +
                "tikv Branch: `${TIKV_BRANCH}`" + "\n" +
                "pd   Branch: `${PD_BRANCH}`" + "\n" +
                "${env.RUN_DISPLAY_URL}"

        if (currentBuild.result != "SUCCESS") {
            slackSend channel: '#stability_test', color: 'danger', teamDomain: 'pingcap', tokenCredentialId: 'slack-pingcap-token', message: "${slackmsg}"
        }
    }
}

return this
