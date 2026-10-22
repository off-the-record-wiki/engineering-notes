# gRPC - Various Communication Patterns in gRPC

#### Unary RPC의 기본 통신 특징

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/498c4890-05c5-467f-b169-9c6d79f2f772.png)

* 1:1 통신 : 요청 메시지 하나와 응답 메시지 하나로 구성된다.
* 동기·비동기 지원 : 응답을 기다리는 동기 방식과 다른 작업을 수행하다 응답을 처리하는 비동기 방식 모두 가능하다.
* 명확한 종료 시점 : 서버가 응답과 상태 코드를 반환하면 호출이 종료된다.
* 독립적인 호출 : 각 요청은 일반적으로 별도의 RPC 호출로 처리된다.
* 오류 상태 전달 : 서버는 응답 대신 상태 코드와 오류 메시지를 반환할 수 있다.
* 타임아웃 설정 : 클라이언트가 deadline 또는 timeout을 지정해 무한 대기를 방지할 수 있다.
* 메타데이터 전달 : 인증 토큰이나 추적 ID와 같은 부가 정보를 헤더 형태로 주고받을 수 있다.
* 구현 단순 : 일반적인 REST 요청 - 응답 구조와 유사해 CRUD, 조회, 인증 등에 적합하다.

> **일반적인 요청–응답 흐름은 REST와 유사하지만, Unary RPC는 리소스가 아닌 원격 메서드 호출을 중심으로 설계된다.**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d98ce46b-13c2-4ecc-b9db-c9a72838285e.png)

* Unary RPC 통신에서 요청마다 새로운 네트워크 연결을 생성하지 않고, 기존에 수립된 Channel과 HTTP/2 연결을 재사용해 연결 설정 비용을 최소화한다.
* 또한 Keep-Alive와 재연결 정책을 통해 유휴 연결의 유효성을 확인하고 일시적인 네트워크 장애에 대응한다.
* Channel 재사용은 TCP 연결 설정과 TLS 핸드셰이크에서 발생하는 비용을 줄인다. Channel은 일반적으로 장기간 유지되며, 애플리케이션의 여러 호출에서 공유된다.
* HTTP/2는 하나의 연결에서 여러 요청을 동시에 처리할 수 있는 다중화를 지원한다. 따라서 각 RPC 호출에 별도 연결을 할당하지 않아도 요청 간 독립성을 유지하면서 높은 동시성을 확보할 수 있다.
* Keep-Alive는 일정 시간 동안 RPC 호출이 없더라도 연결의 유효성을 확인하기 위한 메커니즘이다. 클라이언트는 HTTP/2 PING 프레임을 전송하고 서버의 ACK 수신 여부를 통해 연결 상태를 판단한다.
  * 유휴 연결이 프록시나 로드밸런서에 의해 종료되는 현상 완화
  * 실제 RPC 호출 직전에 연결 장애 감지
  * 장시간 유지되는 연결의 상태를 지속해서 확인
* Keep-Alive 주기를 지나치게 짧게 설정하면 불필요한 네트워크 트래픽과 서버 부하가 발생할 수 있으니 idle timeout 정책을 고려해 설정해야 한다.
* 연결 장애가 발생하면 Channel은 일시적 장애 상태인 `TRANSIENT_FAILURE`로 전환될 수 있다. 이후 클라이언트는 지수 백오프를 적용해 재연결을 시도한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/027f8fc2-c387-4a2f-9a4a-7f8e0a32ea3b.png)

* gRPC 배치 처리는 여러 개의 작업을 하나의 요청 메시지로 묶어 한 번의 Unary RPC로 전송하는 애플리케이션 수준의 최적화 방식이다.
* gRPC가 개별 호출을 자동으로 배치하는 것은 아니며, 요청 수집기와 배치용 Protobuf 메시지 및 RPC 메서드를 별도로 설계해야한다.
* 배치 처리의 목적은 연결 수를 줄이는 것이 아니라 RPC 호출 횟수를 줄이는 데 있다. Channel과 HTTP/2 연결이 이미 재사용되더라도 각 호출에는 직렬화, 메타데이터, 인증, 인터셉터, 서버 스케줄링 및 관측 처리 비용이 발생한다. 여러 작업을 한 호출로 묶으면 이 고정 비용을 여러 항목에 분산할 수 있다.

```protobuf
service UserService {
  rpc GetUsersBatch(GetUsersBatchRequest)
      returns (GetUsersBatchResponse);
}

message GetUsersBatchRequest {
  repeated UserRequest requests = 1;
}

message UserRequest {
  string request_id = 1;
  string user_id = 2;
}

message GetUsersBatchResponse {
  repeated UserResult results = 1;
}

message UserResult {
  string request_id = 1;

  oneof outcome {
    User user = 2;
    ItemError error = 3;
  }
}
```
* 요청과 응답 순서에만 의존하지 않고 `request_id`로 결과를 연결해야 한다. 이를 통해 서버가 항목을 병렬 처리하거나 응답 순서를 변경하더라도 올바른 호출자에게 결과를 전달할 수 있다.
* `max_batch_size`가 크면 호출 수와 항목당 오버헤드는 감소하지만 메시지 크기, 메모리 사용량 및 처리 시간이 증가한다. `max_wait_time`이 길면 배치가 채워질 가능성은 높아지지만 개별 요청 지연시간이 늘어난다. 따라서 처리량과 지연시간 목표를 함께 측정해 값을 결정해야 한다.
* 배치 처리는 완료됐지만 일부 항목만 실패한 경우에는 RPC를 OK로 종료하고 응답 메시지에 항목별 오류를 포함하는 것이 일반적이다. 호출 전체를 재시도하면 이미 성공한 항목이 중복 처리될 수 있으므로 상태 변경 작업에는 요청 식별자 또는 멱등성 키를 적용해야 한다.

#### 실시간 통신에 필수로 사용되는 Streaming RPC 통신

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/399e3177-198f-49f3-b4a8-d5d95a6e81a7.png)

* 백프레셔는 수신 측의 처리 상태에 맞춰 송신 속도를 제어해 메시지 누적과 메모리 고갈을 방지한다.
* 클라이언트·서버에 용량이 제한된 큐와 동시성 상한을 적용하고, 스트림 준비 상태 및 HTTP/2 흐름 제어에 따라 전송을 중단하거나 재개한다.
* 버퍼 포화 정책과 deadline·취소·자원 해제를 명확히 정의하며, 재시도에 대비해 메시지 ID와 멱등성을 보장한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/98b2d763-064a-4952-9f88-62aaa1c2c275.png)

* 초기 동기화: 클라이언트는 현재 상태의 Snapshot과 Version을 받아 로컬 상태를 초기화한다.
* 실시간 반영: 해당 Version 이후의 이벤트를 gRPC 스트림으로 구독하고 순서대로 적용하며, 마지막 처리 Version을 Checkpoint로 저장한다.
* 누락 복구: Version Gap이 발견되면 Checkpoint 이후 이벤트를 재전송받고, 로그 보존 기간이 지난 경우 새로운 Snapshot으로 전체 상태를 재동기화한다.
* 설계 원칙: 이벤트 순서, 중복 처리 멱등성, 체크포인트 영속화, 재연결·재전송 정책과 백프레셔를 함께 고려한다.

#### Bidirectional Streaming RPC 주의사항 및 동시성

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fb7b41a0-236f-4c63-98c4-1ef30ba7752b.png)

* Full-Duplex 통신은 하나의 gRPC 양방향 스트림에서 요청 송신과 응답 수신을 동시에 수행하는 방식이다.
* 송신과 수신을 별도의 스레드 또는 실행기로 분리해, 한쪽 작업이 지연되더라도 반대 방향의 처리를 차단하지 않도록 한다.
* 각 방향에 독립적인 제한 버퍼와 백프레셔를 적용하고, 스트림 쓰기는 순서를 보장하도록 직렬화한다.
* 완료·오류·취소 발생 시 양쪽 작업을 조정하고 스레드, 큐, 스트림 자원을 안전하게 해제한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c423042a-422a-4985-8fc5-961e42497409.png)

* 동시 처리량은 Permit과 제한된 Worker Pool로 통제하고, 작업이 끝나면 Permit을 반환한다.
* 처리 상한에 도달하면 제한 큐에서 대기시키며, 큐까지 포화되면 `RESOURCE_EXHAUSTED`로 빠르게 실패시킨다.
* 운영 전환 시 기존 서버를 `DRAINING` 상태로 변경하고 readiness를 내려 신규 요청을 차단한다.
* 신규 요청은 새 서버로 전달하고, 기존 서버는 진행 중인 RPC만 Grace Period 안에서 완료한다.
* 모든 요청이 끝나거나 Grace Period가 만료되면 서버를 종료하고 채널·스레드·큐를 해제한다.
