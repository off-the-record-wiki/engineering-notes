# MicroService Architecture - Resilience & Observability and Monitoring

#### Resilience Pattern

* 장애가 발생할 것이라고 가정하며 장애를 수용하기 위한 모델이다.
  * 특히 분산 시스템에서 예상치 못한 장애 처리시 필요하다.
  * 네트워크 또는 컨테이너 장애 시 마이크로서비스는 요청을 다시 시도하기 위한 전략이 필요하다.
* 최악의 시나리오에 대비할 수 있는 장애를 허용(극복)하는 마이크로서비스 설계이다.
  * 단일 마이크로서비스가 실패하거나 잠시 응답하지 못할 수 있다.
  * 서비스 과부하, 늦은 응답, 네트워크 문제 등
* 마이크로서비스의 회복성(Resilience)	설계
  * 장애에 대한 복원성(회복성)
  * 부분적 장애 허용
  * 장애가 발생할 것이라고 가정하고 복원성(회복성)을 위한 마이크로서비스를 설계한다.
* 장애 허용(Fault	Tolerance)
  * 다운타임이나 데이터 손실 없이 대응
  * 장애 후 App을 완전히 작동하는 상태로 되돌린다. 
  * 장애를 우아하게 처리(Graceful Shutdown)
* 회복성(Resilience)
  * 마이크로서비스 시스템은 장애 또는 중단하더라도 효과적으로 계속 작동 가능하다.
* 내결합성(Fault Tolerance)
  * 일부 구성 요소 내에서 하나 이상의 오류가 발생하는 경우에는 시스템은 정상적으로 작동한다.
* 서비스 간 통신은 분산되고 많은 내부 또는 외부 네트워크 트래픽이 트랜잭션을 위해 생성된다.
  * 모놀리식에 비해 내구성 보장이 어렵다.
  * 서비스 간 통신 필요성과 외부 서비스에 대한 종속성이 증가한다.
  * 오류가 발생할 가능성이 증가한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/dc96932f-cf6d-42b5-8467-38df86df1213.png)

#### Resilience Pattern(Retry)

* HTTP, gRPC 등을 사용하는 서비스 간 통신
  * 네트워크, 서버 및 물리적 오류가 발생할 수 있다.
  * 트랜잭션의 서비스 중 하나가 500 Error Code 반환하면 트랜잭션이 중단되고 오류가 반환된다.
  * 사용자가 동일한 트랜잭션을 다시 시작하면 정상 작동한다.
* 마이크로서비스는 일시적인 오류로 인해 실패할 가능성이 높다.
  * 이런 오류는 짧은 시간 내에 발생하면 이후에 수정된다.
* [Retry Pattern](https://en.wikipedia.org/wiki/Abort,_Retry,_Fail%3F)
  * 실패할 경우 작업을 자동으로 재시도한다.
  * 마이크로서비스가 자체 수정 ⭢ 스스로를 수정할 시간 허용한다.
  * 서비스에 지속적인 장애가 발생, 장시간 사용할 수 없는 경우라면 실패한 요청이 지나치게 많아지게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e1c6d7cd-573b-4c66-9373-0c2d08771aa1.png)

#### Resilience Pattern(Circuit Breaker)

* 전자시스템 회로를 보호하기 위해 시스템에 장애가 발생할 경우 더 이상의 부하 전달을 중지하는 기능이다.
* Circuit Breaker Pattern
  * 외부 의존성(종속성)에 대한 장애로부터 보호한다.
  * 한 서비스의 장애가 다른 서비스에 연쇄 효과를 미치지 않도록 차단한다.
* 서비스 간 통신 모니터링 ⭢ 오류 추적
  * 시스템의 오류가 특정 임계값(Threshold)을 초과 ⭢ Circuit Breaker가 작동되고(open) 통신을 차단 ⭢ 미리 결정된 오류 메시지 반환
  * Circuit Breaker가 열려 있는 동안 통신 트래픽을 계속 모니터링한다.
  * 요청된 서비스가 성공적인 결과를 반환하면 Circuit Breaker가 종료된다.(close)
* **Close(닫힘)** : Circuit Breaker가 닫힌 상태. 모든 요청이 실행된다.
* **Open(열림)** : Circuit Breaker가 열린 상태. 오류가 발생하는 동안 애플리케이션이 작업을 반복적으로 실행하려고 하지 않도록 처리한다.
* **Half Open(반열림)** : Circuit Breaker는 오류가 계속 발생하는지 확인하기 위해 몇 가지 작업을 실행한다. 오류가 발생하면 Circuit	Breaker가 열리고 오류가 발생하지 않으면 Circuit Breaker는 닫힌다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8c81140f-dcb5-4bd4-8431-dc0b3ee7601a.png)

#### Resilience Pattern(Bulkhead Pattern)

* Bulkhead Pattern이란, 분산 시스템에서 장애를 격리하는 데 사용한다.
* 마이크로서비스에서 개별 서비스 인스턴스 또는 서비스 인스턴스 그룹을 서로 격리한다.
  * 시스템의 한 부분에서 장애가 발생해도 전체 시스템에 영향을 미치지 않도록 한다.
  * 한 위치에서 서비스를 격리해 다른 서비스에 영향을 미치지 않도록 한다.
  * 오류 장소를 격리하고 나머지 서비스를 보호한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3e7e37a6-0417-4a72-a552-395b4b69289e.png)

#### Resilience Pattern(Timeout Pattern)

* 서비스 응답을 무한정 기다리지 않고 예외를 발생시킨다.
  * 서비스 호출이 완료되는 데 예상보다 오래 걸리는 경우에 사용한다.
  * 최대 시간 제한을 설정해 Timeout 오류를 발생시킨다.
* 마이크로서비스에서 서비스 호출이 완료되는 데 너무 오래 걸리는 것을 방지한다.
* 특정 서비스가 요청을 완료하는 데 예상보다 오래 걸리는 경우 ⭢ 요청을 취소하고 빠른 실패(fail-fast) 처리를 한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/28b94c65-595e-48ed-aad0-6ff6060bb162.png)

#### Resilience Pattern(Fallback Pattern)

* 요청이 실패하거나 시간 초과될 경우 대체 동작이나 응답을 제공한다.
  * 서비스를 사용할 수 없는 경우
  * 클라이언트는 캐시된 버전의 데이터를 사용한다.
  * 사용자에게 기본 메시지를 표시해 대체 처리한다.
* 서비스 호출이 실패하거나 시간 초과될 경우 호출되는 Fallback 함수 정의
  * Fallback 함수에서 대체 응답을 제공한다.
  * 특정 서비스를 사용할 수 없거나 요청을 완료하는데 너무 오래 걸리는 경우
  * 오류 메시지 표시 혹은 나중에 다시 시도할 수 있는 옵션을 제공한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/49c18bb9-7b2b-474d-ae9f-ae181b33a1a8.png)

#### Observability: Distributed Logging/Tracing

* 마이크로서비스의 복잡한 종속성을 모니터링하고 관리하기 위한 전략이 필요하다.
* 마이크로서비스로 구성된 시스템의 동작과 성능을 모니터링, 이해, 관리한다.
* Distributed Logging
  * 여러 서비스 인스턴스 ⭢ 로그 수집, 저장 및 분석
  * 시간 경과에 따른 시스템 동작, 패턴, 추세 식별
  * 시스템 성능 확인 ⭢ 병목 현상 식별 ⭢ 문제 해결
* Distributed Tracing
  * 시스템을 통한 요청 흐름 추적 ⭢ 분산 추적 구현
  * 장바구니 항목 추가 ⭢ 장바구니 서비스 ⭢ 결제 처리
  * 서비스 ⭢ 제품 카탈로그 서비스 추적

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/15d51a8e-019c-49e7-a7c0-d419b5bb3e0a.png)

#### Microservices: Health Checks

* 시스템의 개별 마이크로서비스의 상태와 성능을 모니터링하는 프로세스
* 마이크로서비스의 상태 점검이란?
  * 시스템의 개별 마이크로서비스의 상태와 성능을 모니터링하는 방법
  * 마이크로서비스가 제대로 작동하고 요청을 처리할 수 있는지 확인하는 데 사용
* Liveness Checks : 프로세스가 살아 있는지(응답 불가·데드락 상태가 아닌지) 확인하고, 실패하면 컨테이너를 재시작한다.
* Readiness Checks : 트래픽을 받을 준비가 됐는지(DB·캐시 등 의존성 연결 완료) 확인하고, 실패하면 재시작 없이 로드밸런서에서만 제외한다.
* Performance Checks : 살아는 있지만 응답 지연·에러율 등 성능이 임계치를 벗어났는지 확인해 성능 저하 상태를 조기에 감지한다.
