# Jenkins - Jenkins Fundamentals & Environment Setup

#### Jenkins

* 소프트웨어 개발 시 지속적 통합(CI)과 지속적 배포(CD)를 구현해주는 대표적인 무료 오픈 소스 자동화 서버이다.
* 개발자가 코드를 수정하고 저장소에 올리면 이후의 빌드, 테스트, 배포 과정을 자동으로 처리해준다.
* Jenkins를 도입하면 다음과 같은 이점이 생긴다.
  * 반복적인 수작업 탈출 : 개발자가 직접 컴파일하고 서버에 접속해 업로드하던 지루한 과정을 없앤다.
  * 빠른 피드백과 버그 최소화 : 코드를 푸시하자마자 자동으로 테스트가 실행되므로 문제 발생 시 즉각 피드백을 받을 수 있다.
  * 강력한 플러그인 연동 : 다양한 오픈 소스 플러그인을 지원하여 Docker, AWS, Github, Slack 등 기존에 사용하던 다양한 개발 도구들과 쉽게 연동이 가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/50f57a53-134e-4107-9408-c5561216ba17.png)

***

1. 코드 커밋 : 개발자가 새로운 기능이나 버그를 수정하고 소스 코드 저장소에 코드를 올린다.
2. 트리거 : 저장소에 변경 사항이 감지되면 Webhook 등을 통해 Jenkins에 자동으로 알림이 전달된다.
3. 빌드 및 테스트 : Jenkins가 즉시 최신 코드를 가져와 컴파일한다. 설정된 단위 테스트 및 E2E 테스트를 진행한다.
4. 배포 : 테스트 통과 시, 코드를 운영 서버(AWS 클라우드 등)에 자동으로 배포한다.

***

#### Jenkins Plugin

* [Stage View](https://plugins.jenkins.io/pipeline-stage-view/)
