pipeline {
    agent { label 'bazel-debian' }
    stages {
        stage('GJF') {
            steps {
                sh 'mvn process-sources'
                script {
                    def formatOut = sh (script: 'git status --porcelain', returnStdout: true)
                    if (formatOut.trim()) {
                        def files = formatOut.split('\n').collect { it.split(' ').last() }
                        files.each { gerritComment path:it, message: 'Needs reformatting with GJF' }
                        gerritReview labels: [Formatting: -1]
                    } else {
                        gerritReview labels: [Formatting: 1]
                    }
                }
            }
        }
        stage('build') {
            steps {
                gerritReview labels: [Verified: 0], message: "Build started: ${env.BUILD_URL}"
                sh 'mvn test'
            }
        }
    }
    post {
        success { gerritReview labels: [Verified: 1] }
        unstable { gerritReview labels: [Verified: 0], message: "Build is unstable: ${env.BUILD_URL}" }
        failure { gerritReview labels: [Verified: -1], message: "Build failed: ${env.BUILD_URL}" }
    }
}
