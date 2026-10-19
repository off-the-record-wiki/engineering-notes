# Jenkins - Jenkins Pipeline Project

#### Pipeline Script

```groovy
// 선언형 파이프라인의 시작. 전체 CI/CD 흐름을 정의하는 블록
pipeline {

    // 어느 젠킨스 에이전트(노드)에서든 실행 허용. 사용 가능한 워커에 배정됨
    agent any 

    // 파이프라인을 이루는 단계(stage)들의 묶음
    stages {
        // 'Build' 라는 이름의 단계. 젠킨스 UI에 이 이름으로 표시됨
        stage('Build') {
            steps {
                echo 'make a new burger'
                sh 'mkdir -p kitchen'
                sh 'touch kitchen/hambuger.txt'
                sh 'echo "Patty" >> kitchen/hambuger.txt'
                sh 'cat kitchen/hambuger.txt'
            }
        }
    }

    // 파이프라인(또는 스테이지)의 모든 단계가 끝난 뒤 실행되는 후처리 블록
    post {
        // 성공/실패/중단 등 결과와 무관하게 항상 실행
        always {
            // 워크스페이스를 비움
            cleanWs()
        }
    }
}
```

#### 빌드 성공 시 결과물 저장 방법

```groovy
pipeline {
    agent any

    // 변수 정의
    environment {
        BUILD_FILE_NAME = 'hambuger.txt'
    }

    stages {
        stage('Build') {
            steps {
                // 빌드 시작 전 워크스페이스를 먼저 비워 깨끗한 상태에서 시작
                cleanWs()
                echo 'make a new burger'
   
                // 위에 선언한 변수명 사용 시 반드시 큰따옴표여야만 한다.
                echo "$BUILD_FILE_NAME"
                // 여러 개의 명령어를 하나로 합치기
                sh '''
                    mkdir -p kitchen
                    touch kitchen/hambuger.txt
                    echo "Patty" >> kitchen/hambuger.txt
                    cat kitchen/hambuger.txt
                '''
            }
        }
    }
    
    // 파이프라인의 모든 단계가 끝난 뒤 실행되는 후처리 블록
    post {
        // 빌드가 성공했을 때만 실행
        success {
            // 특정 폴더 아래 모든 파일을 빌드 산출물(아티팩트)로 보관 → 젠킨스 UI에서 다운로드 가능
            archiveArtifacts artifacts: "kitchen/**"
        }
    }
}
```

* Jenkins Pipeline에서 종료 코드 0은 프로그램이 성공적으로 실행되었음을 나타낸다.
* 종료 코드가 1 ~ 255 사이의 숫자라면 실패를 나타낸다.
* 하나의 Stage 실행 중 종료 코드가 반환되면 그 이후 작업은 수행되지 않는다.
