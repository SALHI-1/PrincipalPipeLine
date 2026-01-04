def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            // ID des identifiants Docker Hub créés dans Jenkins
            DOCKER_HUB_CREDS = credentials('CRED_DOCK')
            IMAGE_NAME = "${config.registry}/${config.appName}"
        }

        stages {
            stage('📥 Extraction') {
                steps {
                    // Récupère le code source du microservice
                    checkout scm
                }
            }

            stage('🏗️ Build Maven') {
                steps {
                    script {
                        // Donne les droits d'exécution au wrapper Maven présent dans le projet
                        sh "chmod +x mvnw"
                        // Compile le projet et génère le JAR dans /target
                        sh "./mvnw clean package -DskipTests"
                    }
                }
            }

            stage('🐳 Construction Docker') {
                steps {
                    script {
                        // Construit l'image à partir du Dockerfile du service
                        // Utilise le tag unique du build Jenkins et le tag 'latest'
                        sh "docker build -t ${IMAGE_NAME}:${env.BUILD_NUMBER} ."
                        sh "docker tag ${IMAGE_NAME}:${env.BUILD_NUMBER} ${IMAGE_NAME}:latest"
                    }
                }
            }

            stage('🚀 Publication Docker Hub') {
                steps {
                    script {
                        // Connexion sécurisée à Docker Hub (saaymo)
                        sh "echo \$DOCKER_HUB_CREDS_PSW | docker login -u \$DOCKER_HUB_CREDS_USR --password-stdin"
                        
                        // Envoi des images vers le dépôt distant
                        sh "docker push ${IMAGE_NAME}:${env.BUILD_NUMBER}"
                        sh "docker push ${IMAGE_NAME}:latest"
                    }
                }
            }

            stage('🧹 Nettoyage') {
                steps {
                    // Supprime l'image locale pour libérer de l'espace sur le serveur Jenkins
                    sh "docker rmi ${IMAGE_NAME}:${env.BUILD_NUMBER}"
                    sh "docker rmi ${IMAGE_NAME}:latest"
                }
            }
        }

        post {
            success {
                echo "✅ Le service ${config.appName} a été déployé avec succès sur Docker Hub !"
            }
            failure {
                echo "❌ Échec du pipeline pour ${config.appName}."
            }
        }
    }
}
