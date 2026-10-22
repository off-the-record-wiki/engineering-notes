# Jenkins - Implementing Continuous Integration(CI) with Jenkins

#### Jenkins와 Git Repository 연결하기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0abc45e8-7e26-4496-93bf-2acf4a0e3461.png)

* `SCM` : Git
* `Repository URL` : 레포지토리 URL
* `Branch Specifier` : `*/main` 혹은 `*/master`과 같은 배포할 브랜치명
* `Script Path` : `Jenkinsfile`(대소문자 주의)

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            // Docker 컨테이너에서 격리된 환경으로 빌드 실행
            agent {
                docker {
                    image 'gradle:8-jdk17'
                }
            }
            steps {
                // 캐시 제거 후 빌드하여 깨끗한 상태 보장
                sh 'gradle clean build'
            }
        }
    }
}
```

#### Jenkins에서 JUnit5 테스트 보고서 게시

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
    }

    post {
        always {
            // JUnit 테스트 결과를 Jenkins 대시보드에 수집 및 표시
            junit 'build/test-results/test/*.xml'
        }
    }
}
```

#### 전체 파이프라인 흐름 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/83fb9497-9cbf-4e9c-8146-dfc9edb6cfe3.png)
