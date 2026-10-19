# gRPC - Writing .proto Files with Protocol Buffers

#### 왜 다양한 프로토콜이 존재하는지?

* 1980년대에는 원시적인 프로그래밍으로 인해서 성능은 뛰어났지만 관리하기가 너무 어려웠다.
* 그 이후에 HTTP가 웹 기반으로 등장하게 되었고, 일반적으로 알고 있는 프로토콜로써 사용이 되었다.
  * 3-way handshake 문제로 인한 오버헤드가 발생한다는 단점도 존재
  * 비슷한 헤더 형태를 중복해서 전송해야한다는 사이즈 문제가 존재
  * 텍스트라는 형태가 사람에게는 유용하지만, 컴퓨터 입장에서는 바이너리보다 사이즈가 더 크다는 문제 발생
* 그래서 HTTP 1.1이 등장하게 되었다.
  * Keep-Alive 설정을 통해서 연결을 재사용한다.
  * Head-of-Line Blocking 문제는 여전히 존재했다. 이 문제는 작업이 밀리면서 지연이 되는 현상을 말한다.
* 그래서 HTTP/2가 등장하게 되었다.
  * 바이너리 프로토콜을 사용해 파싱 속도 향상
  * 멀티 플렉싱 기능을 도입해 여러 스트림 처리 가능
  * HPack을 사용하여 헤더를 압축
  * 이런 장점을 토대로 REST API가 등장
* MSA 시대가 오면서 오버페칭, 언더페칭이라는 문제도 발생하고 N+1이라는 문제도 다분하게 발생하게 되었다.
* MSA 상황에서의 문제를 해결하기 위해 최종적으로 RPC 통신이 도입되었고 대표적으로 gRPC가 사용되기 시작했다.

#### 분산 시스템에서의 대표적인 문제 & RPC 패러다임

* JSON-RPC : 대중적으로 많이 사용되는 RPC로, 매우 단순하고 웹 개발자에게 친숙한 RPC이다.
* 분산 시스템 관점에서 여러 가지 문제가 존재한다.
  * 대표적으로 네트워크 파티션 장애 처리를 보장해야 한다. 재시도 로직을 포함시키거나 멱등성을 보장하는 형태의 클라이언트 구현. 서킷 브레이커, 사가 패턴 등을 고려해야 한다.
  * 동기와 비동기의 차이를 이해하고 활용할 줄 안다면 처리량 향상에 도움이 된다.

#### Protocol Buffers & 설계 철학

* Protocol Buffers : 구조화된 데이터를 직렬화하는 방식
* JSON 대비 데이터의 크기를 줄일 수 있고 파싱 속도도 더 빠르다. 이를 통해 성능적인 효율성을 챙길 수 있다.
  * 이름 대신 숫자로 데이터를 표현하기 때문에 빠르게 처리가 가능하다. `Varint` 인코딩 방식을 활용한다. 
* 스키마 기반의 설계를 지원한다.
  * 근본적으로 구조가 명확해야 시스템이 안정된다는 철학이 존재한다. 즉, 데이터의 형태와 규칙이 명확해야 시스템이 안정적이라는 의미이다.
  * 컴파일 기반의 안정성을 지원한다.
  * 명시적인 데이터 구조의 문서를 지원한다.
  * 하위 호환성을 보장하는 설계가 가능하다.
* 하지만 기본적으로 사용이 어렵고 디버깅 및 모니터링하기가 어렵다는 단점이 존재한다.
  * 명확하게 개발자가 이 데이터가 어떤 데이터인지 눈으로 보고 확인하기 어렵다. 일종의 해시값처럼 데이터를 관리하기 때문이다.

#### 기본 .proto 문법 작성과 메시지 간에 연관 관계

```proto
syntax = "proto3"; // 문법에 대한 버전 (필수)

package tutorial;  // 패키지 선언 (선택사항)

message Persion {  // 메시지를 정의 (필요에 따라서)
    // 필드 정의
}
```

```proto
syntax = "proto3";

message User {
    int32 id = 1;
    string name = 2;
    string email = 3;
}

// 32, 64, 32, 64

message Numbers {
    int32 age = 1;
    int64 po = 2;
    uint32 count = 3;
    uint64 c = 4;
}

message BasicTyyes {
    float tem = 1;      // 32비트
    double t = 2;       // 64비트
    bool is_active = 3; // 참/거짓
}

message TextData {
    string name = 1; // UTF-8 문자열
    bytes data = 2;  // 바이너리
}

message UserProfile {
    string name = 1;
    repeated string hobbies = 2;
}

name: "김개발"
hobbies: ["독서", "영화감상", "코딩"]

enum Status {
    UNKNOWN = 0;
    ACTIVE = 1;
}

message Account {
    string name = 1;
    Status status = 2;
}

message Address {
    string street = 1;
    string city = 2;
}

message Person {
    string name = 1;
    int32 age = 2;
    Address home_address = 3;
    Address work_address = 4;
}

message Company {
    string name = 1;

    message Emplo {
        string name = 1;
    }

    repeated Emplo employees = 2;
}

message UserSettings {
    string theme = 1;        // 빈 문자열
    bool noti = 2;           // false
    int32 time = 3;          // 0
    repeated string lan = 4; // []
}

enum Status2 {
    UNKNOWN = 0;
    STARTED = 1;
    FINISHED = 2;
}

message ABC {
    Status2 test = 1; // UNKNOWN
}

message User {
    int32 id = 1;
    string name = 2;
    string email = 3;
    bool is_deleted = 4; // Time 관련된 필드가 있으면 좋을 것 같다.
}

enum PostStatus {
    DRAFT = 0;
    PUBLISHED = 1;
    ARCHIVED = 2;
}

message Post {
    int32 id = 1;
    int32 user_id = 6;
    string title = 2;
    string content = 3;
    repeated string tags = 4;
    PostStatus status = 5;
}

message Comment {
    int32 id = 1;
    int32 post_id = 2;
    int32 user_id = 3;
    string content = 4;
}
```

```proto
message SearchReqeust {
    string query = 1;
    string keyword = 2;
    int32 user_id = 3;
    DataRange data_range = 4;
}

message SearchRequestOneOf {
    string query = 1; // 우리가 검색하고자 하는 문구

    // oneof : oneof는 Protobuf에서 여러 필드 중 동시에 하나만 설정될 수 있게 하는 선택지 묶음
    oneof search_type {
        string keyworkd = 2;
        int32 user_id = 3;
        DataRange data_range = 4;
    }
}

message APIResponse {
    string request_id = 1;

    oneof result {
        SuccessData success = 2;
        ErrorInfo error = 3;
    }
}

message SuccessData {
    string message= 1;

    // repeated : Protobuf의 배열/리스트 필드로 값이 0개 이상 들어갈 수 있다.
    repeated string data = 2;
}

message ErrorInfo {
    int32 error_code = 1;
    string error_message= 2;
}

message UserLoginEvent {
    string user_id = 1;
    string ip_address = 2;
}

message OrderCreatedEvent {
    string order_id = 1;
    string customer_id = 2;
}
```
