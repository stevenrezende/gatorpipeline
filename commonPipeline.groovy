def call() {
    pipeline {
        
        
        parameters {
            string(name: 'CHECK_TYPE', defaultValue: 'lint', description: 'Tipo de check a ser executado')
            string(name: 'TARGET_DIR', defaultValue: '.', description: 'Diretório alvo para os checks')
        }
        agent any

        stages {
            stage('Prepare Environment') {
                steps {
                    script {
                        // Verifica se o Docker está instalado e, se não estiver, instala o Docker
                        sh '''
                        if ! command -v docker &> /dev/null
                        then
                            echo "Docker not found, installing Docker..."
                            apt-get update
                            apt-get install -y apt-transport-https ca-certificates curl gnupg2 software-properties-common
                            curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
                            add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
                            apt-get update
                            apt-get install -y docker-ce-cli
                        else
                            echo "Docker is already installed"
                        fi

                        # Instalar kubectl
                        if ! command -v kubectl &> /dev/null
                        then
                            echo "kubectl not found, installing kubectl..."
                            curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                            curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
                            
                            chmod +x kubectl
                            mv kubectl /usr/local/bin/
                            

                        else
                            echo "kubectl is already installed"
                        fi
                        
                        '''
                    }
                }
            }

            stage ('Build Image') {
                steps {
                    script {
                        dockerapp = docker.build("stevenrezende/api-produto:${env.BUILD_ID}", '-f ./src/Dockerfile ./src') 
                    }                
                }
            }

            stage ('Push Image') {
                steps {
                    script {
                        docker.withRegistry('https://registry.hub.docker.com', 'docker_local') {
                            dockerapp.push('latest')
                            dockerapp.push("${env.BUILD_ID}")
                        }
                    }
                }
            }

        // stage ('Deploy Kubernetes') {
        //     environment {
        //         tag_version = "${env.BUILD_ID}"
        //     }
        //     steps {
        //         withKubeConfig([credentialsId: 'kubeconfg']) {
        //             sh 'sed -i "s/{{tag}}/$tag_version/g" ./k8s/deployment.yaml'
        //             sh 'kubectl apply -f ./k8s/deployment.yaml'
        //         }
        //     }
        // }
        }
    }
}