pipeline {
    agent any
    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }
    tools { maven 'maven-3.9' }
    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: true,
            description: 'Run the Testcontainers integration tests (needs Docker)')
    }
    environment {
        WILDFLY_HOST  = 'wildfly'
        WILDFLY_CREDS = credentials('wildfly-admin')    // creates WILDFLY_CREDS_USR / WILDFLY_CREDS_PSW
        APP_URL       = 'http://wildfly:8080/mozantech'
    }

    stages {
        stage('Build and test') {
            steps {
                sh "mvn -B clean verify -DskipTests=${!params.RUN_TESTS}"
            }
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }

        stage('Archive') {
            steps { archiveArtifacts artifacts: 'target/mozantech.war', fingerprint: true }
        }

        stage('Deploy to JBoss') {
            steps {
                sh '''
                    mvn -B wildfly:deploy -DskipTests \
                        -Dwildfly.hostname=$WILDFLY_HOST -Dwildfly.port=9990 \
                        -Dwildfly.username=$WILDFLY_CREDS_USR -Dwildfly.password=$WILDFLY_CREDS_PSW
                '''
            }
        }

        stage('Smoke test') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                        code=$(curl -s -o /dev/null -w "%{http_code}" $APP_URL/items || true)
                        # 200 or 401 both prove the app is up (401 = secured, as designed)
                        if [ "$code" = "200" ] || [ "$code" = "401" ]; then
                            echo "Application is up (HTTP $code)"; exit 0
                        fi
                        sleep 5
                    done
                    echo "Application did not start"; exit 1
                '''
            }
        }
    }

    post {
        success { echo 'Deployed: http://localhost:8082/mozantech' }
        failure { echo 'Pipeline failed: check the stage logs above' }
    }
}

