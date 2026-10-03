// Pipeline declarativo para Jenkins. Requiere en el agente: JDK 21, Maven, Chromium + chromedriver (la imagen de ./jenkins ya los trae).
// Credencial necesaria (tipo "Secret text"): qalab-report-token  → el token de tu proyecto en el panel.
pipeline {
    agent any
    options { timestamps(); timeout(time: 40, unit: 'MINUTES') }
    parameters {
        choice(name: 'PERSONA', choices: ['todas', 'estandar', 'lento', 'intermitente', 'visual', 'amnesia', 'expira', 'descuadre'],
               description: 'Usuario de prueba de qalab. «todas» las corre una tras otra.')
    }
    stages {
        stage('Pruebas y reporte') {
            steps {
                script {
                    def personas = params.PERSONA == 'todas' ? ['estandar', 'lento', 'intermitente', 'visual', 'amnesia', 'expira', 'descuadre'] : [params.PERSONA]
                    for (p in personas) {
                        // «estandar» debe pasar; el resto falla a propósito → la etapa queda UNSTABLE, no FAILURE
                        catchError(buildResult: p == 'estandar' ? 'FAILURE' : 'UNSTABLE', stageResult: p == 'estandar' ? 'FAILURE' : 'UNSTABLE') {
                            sh "rm -rf target && mvn -B -ntp test -Dqalab.user=${p}"
                        }
                        junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml', skipMarkingBuildUnstable: true
                        archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true
                        withCredentials([string(credentialsId: 'qalab-report-token', variable: 'QALAB_REPORT_TOKEN')]) {
                            withEnv(["QALAB_USER=${p}"]) { sh './scripts/report.sh' }
                        }
                    }
                }
            }
        }
    }
}
