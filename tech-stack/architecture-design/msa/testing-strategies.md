# MicroService Architecture - Testing Strategies

#### Microservices Testing Strategies

* 마이크로서비스는 독립적으로 배포, 운영되는 여러 작은 컴포넌트로 구성되어 있어, 서비스 간 연계가 많고 복잡성이 높다.
* 테스트를 통해 각 서비스의 독립성을 유지하면서 신뢰성과 품질 확보가 필요하다.
* Test Coverage
  * 서비스 전반에서 테스트가 얼마나 충분히 진행되었는지 측정
  * Jacoco, SonarQube
  * 코드 커버리지의 가시성을 확보

#### Microservices Unit Testing

* 마이크로서비스를 테스트하려면 단위 테스트가 목록의 맨 위에 위치한다.
* 단위 테스트는 애플리케이션의 가장 작은 기능이나 단위를 분석해 알려진 입력이 주어졌을 때 각 단위가 제대로 동작하는지 평가한다.
* 단위 테스트는 마이크로서비스 애플리케이션 기능과 클래스의 신뢰성과 안정성 평가 기능을 담당한다.
* 개발 단계에서는 모든 코드 구성 요소가 예상대로 작동하는지 확인하기 위해 단위 테스트를 수행한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1bbd13de-32a1-4347-80c8-b463aebf1094.png)

#### Microservices Integration Testing

* 서비스 간 통신 테스트 ⭢ 통합 테스트
* 네트워크를 통한 연계에서 가장 기본적인 성공 및 실패 시나리오 확인 가능
* 통합 테스트는 구성 요소 간의 통신 경로 확인 ⭢ 인터페이스 오류 식별, 구성 요소 기능적 종속성을 보장
* 하위 시스템이 피어와 상호 작용할 때 통신 채널이 제대로 작동하는지 확인한다.
* MSA에서는 통합 코드와 외부 구성 요소가 제대로 통신하는지 확인한다.
* 통합 테스트는 코드의 스키마가 데이터베이스 스키마와 일치하는지 확인한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/64cea82a-19a8-4930-90e4-1acd1abdc3eb.png)

#### Contract testing

* 두 시스템 통합이 제대로 작동하는지 확인한다.
* Contract 테스트는 각 서비스의 상호 작용을 기록하고 저장하며, 양측이 계약을 준수하는지 확인한다.
* 외부 서비스와 해당 경계 간의 상호 작용은 계약 준수 여부를 테스트하여 계약에서 소비 서비스에 대한 기대치와 일치하는지 확인한다.
* 모든 서비스에 대한 테스트는 독립적으로 수행 ⭢ 각 서비스를 한 번에 하나씩 호출해 결과를 확인한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1f63478a-5253-4a4f-83e5-209135d843d2.png)

#### E2E testing

* E2E 테스트는 분산 애플리케이션이 처음부터 제공까지 의도한 대로 수행되고, 마이크로서비스 간에 주요 충돌이 없으며, 시스템 구성이 적절한지 확인한다.
* 테스트는 효율적이나 실행 및 유지 관리가 어렵다.
* 중요한 변경 사항을 발견하려면 각 애플리케이션에서 E2E 테스트 양을 줄이고 단위 및 통합 테스트와 같은 하위 수준 테스트 접근 방식에 더 의존적이어야 한다.
* 잘 계획된 마이크로서비스 테스트 접근 방식은 더 많은 단위 테스트, 더 적은 통합 및 구성 요소 테스트, 그리고 최소한의 E2E 테스트로 구성한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d3fcd5b3-745b-45a3-ae7c-617ea8baa90e.png)

#### Testing Automation(CI/CD)

* 지속적인 테스트 자동화로 빠르고 안정적인 배포 가능
* Jenkins, Github Actions, GitLab CI/CD
* 테스트 자동화 실행과 통합 - CI/CD 파이프라인 내에서 단위 테스트, 통합 테스트, E2E 테스트
* 자동화 구성 사례 - 배포 단계에서 테스트 실패 시 즉시 배포 중단 및 알림 제공, 테스트 결과 리포트를 자동 생성하고 팀과 공유
