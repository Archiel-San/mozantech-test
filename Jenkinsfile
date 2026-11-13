
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run the Testcontainers integration tests (needs Docker)'
        )
    }

    environment {
        WILDFLY_HOST = 'wildfly'
        WILDFLY_CREDS = credentials('wildfly-admin')
        APP_URL = 'http://wildfly:8080/mozantech'
    }

    stages {

        stage('Build and test') {
            steps {
                sh '''
                    chmod +x mvnw

                    ./mvnw -B clean verify \
                        -DskipTests=$([ "$RUN_TESTS" = "false" ] && echo true || echo false)
                '''
            }
            post {
                always {
                    junit(
                        allowEmptyResults: true,
                        testResults: '**/surefire-reports/*.xml, **/failsafe-reports/*.xml'
                    )
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts(
                    artifacts: 'target/mozantech.war',
                    fingerprint: true
                )
            }
        }

        stage('Deploy to WildFly') {
            steps {
                sh '''
                    ./mvnw -B wildfly:deploy -DskipTests \
                        -Dwildfly.hostname=$WILDFLY_HOST \
                        -Dwildfly.port=9990 \
                        -Dwildfly.username=$WILDFLY_CREDS_USR \
                        -Dwildfly.password=$WILDFLY_CREDS_PSW
                '''
            }
        }

        stage('Smoke test') {
            steps {
                sh '''
                    for i in $(seq 1 30); do

                        code=$(curl -s -o /dev/null \
                            -w "%{http_code}" \
                            "$APP_URL/items" || true)

                        # 200 = successful response
                        # 401 = endpoint requires authentication

                        if [ "$code" = "200" ] || [ "$code" = "401" ]; then
                            echo "Application is up (HTTP $code)"
                            exit 0
                        fi

                        sleep 5
                    done

                    echo "Application did not start"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo 'Successfully deployed to WildFly!'
        }

        failure {
            echo 'Pipeline failed: check the stage logs above'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}
