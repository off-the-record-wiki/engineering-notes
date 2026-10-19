# MicroService Architecture - Security Patterns

#### OWASP API Top 10 Vulnerabilities

> [참고 - OWASP Top 10](https://top10.owasp.org/2025/)

* Injection : 공격자가 악의적인 데이터를 명령어나 쿼리에 삽입해 실행. 입력값 검증 및 정제 필요. `Parameterized Query` 또는 ORM 사용
* Broken Authentication : 부적절한 인증 구현으로 사용자 계정 도용. Multi-Factor Authentication 적용. 안전한 인증 절차 및 토큰 관리 정책 수립.
* Excessive Data Exposure : 클라이언트가 필요하지 않은 데이터까지 전송해 민감 정보 노출. 데이터 전송 시 필수 정보만 제공. 민감정보를 별도의 보안 계층으로 관리.

#### [Rate Limiting](https://en.wikipedia.org/wiki/Rate_limiting)

* API에 유입되는 요청 수를 제한하여 시스템 과부하 및 남용 방지
* 공격, 과도한 트래픽으로 인한 서비스 장애 방지
* 서비스 안정성 및 가용성 보장 필수
* 장점 : 서비스 과부하 방지, 악의적 공격 최소화, 안정성 향상
* 단점 : 과도한 제한 시 사용자 경험 저하 가능성, 관리 및 설정의 복잡성

#### Rate Limiting Strategy - Throttling

* 정의 : 단기간 과다 요청 시 속도를 제한하거나 요청을 거부하는 방법
* 장점 : 급작스런 트래픽 스파이크 대응 가능
* 단점 : 정상 사용자 요청도 일시적 차단 가능성

#### Rate Limiting Strategy - IP Blocking

* 정의 : 특정 IP에서 오는 요청을 차단하거나 제한
* 장점 : 악성 IP로부터 보호 가능
* 단점 : 정상 사용자가 VPN 등을 사용할 경우 차단 위험 존재

#### Rate Limiting Strategy - Quota Management

* 정의 : 사용자나 앱 별로 명시적 요청 할당량 설정
* 장점 : 서비스 제공자에게 효율적 관리, 비즈니스 모델 연동 가능
* 단점 : 복잡한 사용자 관리 및 부가적 인프라 필요
