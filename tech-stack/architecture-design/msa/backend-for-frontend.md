# MicroService Architecture - Backend for Frontend

#### Backend for Frontend

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d2349276-f85b-4911-a2dd-2ae850637e5a.png)

* 단일 API Gateway ⭢ SPOF
  * 단일하고 복잡한 API Gateway는 아키텍처에 병목 현상을 발생시킨다.
* 특정 Frontend 애플리케이션에 따라 API Gateway를 분리한다.
* BFF를 위한 여러 API Gateway를 생성하고 클라이언트에 따라 경계를 구분한다.
  * 사용자 인터페이스에 따라 여러 API Gateway를 생성
  * 클라이언트 애플리케이션에 내부 마이크로서비스와 연결되는 집중된 인터페이스를 제공
* 대규모 시스템 ⭢ 여러 API Gateway 노출
* 장점
  * 클라이언트 중심 인터페이스
  * Frontend 클라이언트 애플리케이션을 위한 집중화된 인터페이스
  * 데이터 표현 간소화 - 다양한 Backend를 조합해 Frontend에 필요한 데이터만을 전달한다.
* 단점
  * 지연 시간 증가
  * 여러 클라이언트 애플리케이션이 있는 대규모 마이크로서비스에 적합하다.
