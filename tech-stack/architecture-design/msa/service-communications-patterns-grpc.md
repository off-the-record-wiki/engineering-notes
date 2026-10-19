# MicroService Architecture - Service Communications Patterns(gRPC)

#### gRPC 개요

* Google에서 개발한 오픈 소스 원격 프로시저 호출 시스템
* 서비스를 효율적으로 연결하고 분산 시스템을 구축하기 위한 프레임워크이다.
* 고성능, HTTP/2 프로토콜 사용 → 바이너리 메시지를 전송한다.
* 서비스 계약 정의 → Protocol Buffer(Protobuf) 언어 사용
  * Protobuf : 프로그래밍 언어에 관계없이 서비스 간 통신에 사용할 인터페이스를 정의한다.
  * 여러 언어에서 사용 가능한 크로스 플랫폼 클라이언트 및 서버 바인딩 생성
  * 프로그래밍 언어에 독립적, 서로 통신할 수 있는 서비스 생성이 간으하다.
  * Protobuf 계약 정의 : 각 서비스에서 정의된 계약을 사용해 통신 인프라 설정코드를 자동으로 생성한다.

#### gRPC 장/단점

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bbdcbcdb-5ad3-4b96-a242-e30b6882dc4c.png)

* 장점
  * 고성능 통신이 가능하다.
  * 낮은 대역폭 사용량으로 충분하다.
  * 다국어/플랫폼 지원이 가능하다.
  * HTTP/2 및 SSL/TLS를 지원하며 양방향 스트리밍 작업도 가능하다.
* 단점
  * 복잡성 증가/추가 라이브러리가 필요하다.
  * 클라이언트와 서비스 간 통신에 적합하지 않은 송수신 Payload는 읽지 못한다.
