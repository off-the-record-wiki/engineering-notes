# Jenkins - Jenkins Jobs: Freestyle & Pipeline

#### Job

* Jenkins에서 Job이란, Jenkins가 수행해야 할 작업의 기본 단위이다.
* 소스 코드 가져오기, 소스 컴파일하기 등 Jenkins에게 지시할 일련의 작업 순서와 명세서를 의미한다.

#### Jenkins에서 제공하는 2가지 Job

* Freestyle Project
  * 초기 설정이 매우 직관적이고 간단하다.
  * 하지만 빌드 과정이 복잡해지거나 여러 단계로 나뉘면 관리하기가 어렵다.
* Pipeline Project

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4e1ab1aa-f7ac-4a80-9102-20e9ac89eb3b.png)

* 코드를 직접 작성하여 전체 빌드, 테스트, 배포 과정을 정의하는 방식이다.
* `Jenkinsfile`을 활용하여 작업 단계를 스크립트로 기록한다.
* 코드로 관리하기 때문에 수정 이력 버전 관리가 가능하고 조건문이나 반복문을 활용해 훨씬 복잡하고 유연한 CI/CD 워크플로우 구성이 가능하다.

#### Freestyle vs Pipeline

* UI 기반인 Freestyle 작업만으로 구성하기에는 한게가 존재한다는 점을 명확히 인지해야 한다.
* 누군가 구성을 변경할 경우 추적이 어려우며 플러그인이 필요할 때마다 수많은 플러그인을 추가 및 구성해야 하는 번거로움이 존재한다.
* 반면 Pipeline의 경우 전체 빌드 및 배포 구성이 일반적인 소프트웨어 코드처럼 저장된다.
* Freestyle과 달리 코드 변경 사항을 추적할 수 있고 현대 CI/CD에 필수적인 병렬 실행, 여러 단계 구성 등 복잡한 워크플로우를 코드로 유연하게 제어할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d7f45259-f934-428b-9104-02fbd0f0331c.png)

* Jenkins의 타임존은 위와 같이 변경할 수 있다.
