// Linux Jenkins agent: Java 17, Maven, Node/npm, Python 3.11+, pwsh 7, OpenSSH,
// browser OS dependencies and Allure CLI must be provisioned by the operator.
def runSuite(String suite, boolean collectOnly = false) {
    def previous = findFiles(glob: 'automation-tests/report/ci/*/summary.json').collect { it.path }
    withEnv(["SUITE_NAME=${suite}", "COLLECT_ONLY=${collectOnly}"]) {
        pwsh '''
            $targetEnvironment = if ($env:SUITE_NAME -eq 'framework') { 'local' } else { 'public' }
            $argsForSuite = @('-Suite', $env:SUITE_NAME, '-Environment', $targetEnvironment,
                '-OutputRoot', (Join-Path $env:WORKSPACE 'automation-tests/report/ci'),
                '-Python', (Join-Path $env:WORKSPACE '.venv/bin/python'))
            if ($env:COLLECT_ONLY -eq 'true') { $argsForSuite += '-CollectOnly' }
            if ($env:ALLOW_WRITE_TESTS -eq 'true') { $argsForSuite += '-AllowWrite' }
            if ($env:ALLOW_DESTRUCTIVE_COMMERCE -eq 'true') { $argsForSuite += '-AllowDestructiveCommerce' }
            & ./automation-tests/scripts/run-suite.ps1 @argsForSuite
            exit $LASTEXITCODE
        '''
    }
    def created = findFiles(glob: 'automation-tests/report/ci/*/summary.json').findAll { !previous.contains(it.path) }
    if (created.size() != 1) { error('Suite did not produce exactly one new evidence summary') }
    def summary = readJSON(file: created[0].path)
    if (summary.collectionOnly != collectOnly || summary.collected <= 0 || summary.exitCode != 0) {
        error('Suite evidence does not match the requested run or has failed')
    }
    if (!collectOnly && (summary.executed <= 0 || summary.statuses.failed > 0 || summary.statuses.error > 0 || summary.statuses.xpass > 0)) {
        error('Executed suite has failures, teardown errors, XPASS, or no executed tests')
    }
    if (!collectOnly && (summary.statuses.skipped > 0 || summary.statuses.xfail > 0)) {
        if (suite == 'framework') { error('Offline framework must execute without skipped or expected-failure tests') }
        unstable('Public suite contains skipped or expected-failure tests; inspect summary')
    }
}

def withPublicCredentials(Closure action) {
    withCredentials([
        string(credentialsId: 'booking-course-test-key', variable: 'COURSE_TEST_KEY'),
        usernamePassword(credentialsId: 'booking-admin-username-password', usernameVariable: 'BOOKING_ADMIN_USERNAME', passwordVariable: 'BOOKING_ADMIN_PASSWORD'),
        file(credentialsId: 'booking-migration-evidence', variable: 'BOOKING_MIGRATION_EVIDENCE')
    ]) { action() }
}

pipeline {
    agent { label 'booking-linux' }
    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        // Keep deployment and subsequent validation in the same cross-job reservation.
        lock(resource: 'booking-public-environment')
        timestamps()
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '20', artifactDaysToKeepStr: '7', artifactNumToKeepStr: '10'))
    }
    parameters {
        booleanParam(name: 'DEPLOY_APP', defaultValue: false, description: 'Activate a release using the audited remote release contract')
        booleanParam(name: 'RUN_PUBLIC_TESTS', defaultValue: false, description: 'Execute public API/UI/E2E; creates and deletes teaching data')
        booleanParam(name: 'ALLOW_WRITE_TESTS', defaultValue: false, description: 'Authorize run-owned test writes and cleanup')
        booleanParam(name: 'ALLOW_DESTRUCTIVE_COMMERCE', defaultValue: false, description: 'Authorize serial inventory/payment/coupon tests after compensation acceptance')
        booleanParam(name: 'MIGRATION_ACK', defaultValue: false, description: 'DBA completed migration and per-row history audit; fresh evidence credential required')
        booleanParam(name: 'TEST_SUPPORT_READY', defaultValue: false, description: 'Protected test-support and compensation cleanup deployed and accepted')
    }
    stages {
        stage('Checkout') {
            steps { deleteDir(); checkout scm }
        }
        stage('Secret scan') {
            steps { sh 'python3 automation-tests/scripts/scan-publish-secrets.py' }
        }
        stage('Backend focused tests and build') {
            steps {
                sh '''mvn -B -pl booking-api -am -Dtest=ApplicationDevSecretSourceTest,AdminAccountInitializerTest,CourseTestSupportControllerTest,CourseTestCleanupServiceTest,CourseTestCleanupServiceTransactionTest,OrderInventoryServiceTest,InventorySchemaMigrationTest,InventoryCompensationServiceTest,OrderCouponLifecycleTest,AdminOrderCancellationInventoryTest -Dsurefire.failIfNoSpecifiedTests=false clean package'''
            }
        }
        stage('Frontend build') {
            steps {
                dir('frontend') { sh 'npm ci && npm run build' }
                sh 'tar -czf frontend-release.tar.gz -C frontend/dist .'
            }
        }
        stage('Python and browser prerequisites') {
            steps {
                sh '''python3 -m venv .venv
                    .venv/bin/python -m pip install -r automation-tests/requirements.txt
                    .venv/bin/python -m playwright install chromium firefox webkit'''
                pwsh './ci/jenkins/verify-prerequisites.ps1 -Mode offline'
            }
        }
        stage('Offline framework') {
            steps { script { runSuite('framework') } }
        }
        stage('API UI E2E collection') {
            steps {
                script { ['api-read', 'api-write', 'customer-ui', 'admin-ui', 'smoke', 'e2e', 'full'].each { runSuite(it, true) } }
            }
        }
        stage('Controlled deployment') {
            when { expression { params.DEPLOY_APP } }
            steps {
                    script {
                        withPublicCredentials {
                            withCredentials([
                                sshUserPrivateKey(credentialsId: 'booking-deploy-ssh', keyFileVariable: 'BOOKING_SSH_KEY', usernameVariable: 'BOOKING_SSH_USER'),
                                file(credentialsId: 'booking-known-hosts', variable: 'BOOKING_KNOWN_HOSTS')
                            ]) {
                                pwsh '''
                                    $releaseId = $env:BUILD_NUMBER + '-' + $env:GIT_COMMIT.Substring(0, 12)
                                    ./ci/jenkins/deploy-public.ps1 -Jar booking-api/target/booking-api-1.0.0-SNAPSHOT.jar -FrontendArchive frontend-release.tar.gz -ReleaseId $releaseId -Execute
                                '''
                            }
                        }
                    }
            }
        }
        stage('Public validation') {
            when { expression { params.RUN_PUBLIC_TESTS } }
            steps {
                    script {
                        withPublicCredentials {
                            pwsh './ci/jenkins/verify-prerequisites.ps1 -Mode public'
                            // Each write test runs the framework empty-cleanup preflight before setup.
                            // full covers all API/UI/E2E including lifecycle; -n 0 is enforced by run-suite.
                            runSuite('full')
                            runSuite('smoke')
                        }
                    }
            }
        }
    }
    post {
        always {
            script {
                def results = findFiles(glob: 'automation-tests/report/ci/*/summary.json')
                results.each { entry ->
                    def summary = readJSON(file: entry.path)
                    echo "Evidence: collectionOnly=${summary.collectionOnly}, executed=${summary.executed}, passed=${summary.statuses.passed}, failed=${summary.statuses.failed}, error=${summary.statuses.error}, skipped=${summary.statuses.skipped}, xfail=${summary.statuses.xfail}, xpass=${summary.statuses.xpass}"
                    if (!summary.collectionOnly && (summary.statuses.failed > 0 || summary.statuses.error > 0 || summary.statuses.xpass > 0 || summary.exitCode != 0)) {
                        currentBuild.result = 'FAILURE'
                    } else if (!summary.collectionOnly && (summary.statuses.skipped > 0 || summary.statuses.xfail > 0)) {
                        if (currentBuild.currentResult == 'SUCCESS') { currentBuild.result = 'UNSTABLE' }
                    }
                }
                junit(testResults: '**/target/surefire-reports/TEST-*.xml,automation-tests/report/ci/*/junit.xml', allowEmptyResults: true)
                def allurePaths = findFiles(glob: 'automation-tests/report/ci/*/allure-results/*-result.json').collect {
                    [path: it.path.substring(0, it.path.lastIndexOf('/'))]
                }.unique()
                try {
                    if (allurePaths) { allure(results: allurePaths, reportBuildPolicy: 'ALWAYS', includeProperties: false) }
                } finally {
                    archiveArtifacts artifacts: 'automation-tests/report/ci/*/summary.json,automation-tests/report/ci/*/junit.xml,automation-tests/report/ci/*/pytest.html,automation-tests/report/ci/*/allure-results/*.json,automation-tests/report/ci/*/allure-results/*.txt,automation-tests/report/ci/*/browser/**/*sanitized-action-trace.json,automation-tests/report/ci/*/browser/**/capture-diagnostics.json', excludes: '**/*.zip,**/.env*,**/*storage*,**/node_modules/**', allowEmptyArchive: true
                }
            }
        }
    }
}
