# MicroService Architecture - API Gateway Patterns

#### API Gateway Pattern

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/517370eb-c313-4fae-9217-c87c8ebf1747.png)

* 클라이언트 애플리케이션에 대한 단일 진입점이다.
* 클라이언트와 백엔드 사이에 위치하며 여러 마이크로서비스 요청을 1개의 응답으로 처리한다.
* 각각의 API Gateway에서 내부 마이크로서비스로의 라우팅이 이루어진다.
* 장점
  * Reverse Proxy, Routing, Aggregation
  * Abstraction Backend Service 구성
  * 불필요한 네트워크 트래픽 요청 감소
  * Cross-cutting concerns 문제 처리
* 단점
  * 단일 API Gateway ⭢ SPOF 문제 위험
  * API Gateway의 비즈니스 복잡성 증가
  * API Gateway가 네트워크 트래픽의 병목으로 작용 ⭢ 확장성 필요

#### Service Registry/Discovery Pattern

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/213fc9e7-47e7-437d-bbad-f1c08c077854.png)

* 마이크로서비스를 배포하고 확장할 때 분산 애플리케이션에게 확장성을 제공한다.
* 더 많은 마이크로서비스 및 인스턴스 추가가 가능하다.
* 클러스터에서 마이크로서비스를 등록(Registry), 검색(Discovery)
  * 서비스를 주입하거나 결합 X
  * 마이크로서비스의 네트워크 위치 검색 기능
* 클라이언트 및 내부 마이크로서비스로 트래픽 라우팅 ⭢ API 게이트웨이
* 서비스 위치는 자주 변경될 수 있다.
  * 마이크로서비스 아키텍처에 더 동적인 구성 필요
  * IP 주소, 네트워크 위치 ⭢ 동적 할당 필요
  * 자동 확장 기능 ⭢ 변경 발생  
