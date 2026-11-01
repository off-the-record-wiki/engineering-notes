# Spring Batch - SYSTEM TAKEOVER

#### ItemStream

```java
public interface ItemStream {
    default void open(ExecutionContext executionContext) throws ItemStreamException {}
	
    default void update(ExecutionContext executionContext) throws ItemStreamException {}
	
    default void close() throws ItemStreamException {}
}
```
* Spring Batch에서 자원 초기화 및 해제, 메타데이터 관리 및 상태 추적 등의 역할을 담당한다.

#### 자원 초기화 및 해제

![](https://github.com/user-attachments/assets/aec1c226-5b79-4684-85ff-dae5f1baa88f)

* 배치 작업은 다양한 시스템 자원을 다루게 된다. 파일 핸들, 데이터베이스 커넥션, 메모리 버퍼 등등 이런 자원들은 사용 전 적절한 준비가 필요하고 사용 후에는 반드시 정리되어야 한다.
* 파일을 읽기 전에는 파일을 열어야 하고, 커서 기반 `ItemReader`에서는 데이터베이스와의 커넥션을 맺어야 하며 데이터 처리를 위한 메모리 버퍼를 할당해야 한다.
* 작업 완료 후 이런 자원들을 제대로 해제하지 않으면 다음과 같은 치명적인 문제가 발생한다.
  * 파일 핸들 누수: 파일을 열었지만 닫지 않으면 시스템의 파일 핸들이 고갈된다.
  * 데이터베이스 커넥션 누수: DB 커넥션을 해제하지 않으면 커넥션 풀이 고갈되어 다른 작업들까지 영향을 받는다.
  * 메모리 누수: 사용하지 않는 자원이 가용 메모리를 좀먹어 결국 애플리케이션이 다운된다.
* 자원의 초기화와 해제는 `ItemStream`의 `open()`, `close()` 메서드를 통해 처리된다.

#### 메타데이터 관리 및 상태 추적

* 운영환경에서 배치 작업은 언제든 실패할 수 있다. 네트워크 장애가 발생할 수도 있고, 서버가 예기치 않게 종료될 수도 있다. 이런 상황에서 두 가지 선택지가 있다.
  * 처음부터 모든 데이터를 다시 처리한다.
  * 실패한 지점부터 정확하게 재시작한다.
* Spring Batch는 매 트랜잭션마다 현재 실행 상태를 메타데이터 저장소에 기록한다.

#### open() 메서드(저장된 실행 정보를 복원한다)

* `open()` 메서드는 스텝의 시작 시점에 호출되어 작업을 실패한 시점부터 이어서 실행할 수 있도록 상태를 복원하는 역할을 한다.
* `open()` 메서드는 `ExecutionContext`를 입력으로 받는다. 이 `ExecutionContext`에는 이전 스텝 실행의 실행 정보가 담겨있으며, `ItemStream` 구현체들은 이 `ExecutionContext`를 사용해 자신의 이전 상태를 복원한다.

```java
default void open(ExecutionContext executionContext) throws ItemStreamException {}
```

#### update() 메서드(상태 저장)

![](https://github.com/user-attachments/assets/0e3b8d67-424f-4384-bf97-c9656991eb2e)

* 현재 작업이 어디까지 진행되었는지를 저장하는 역할은 `update()` 메서드가 맡고 있다. 이렇게 저장된 정보는 작업이 실패했을 때 정확한 재시작 지점을 파악하는데 사용된다.
* `update()` 메서드 역시 `ExecutionContext`를 파라미터로 받는다. `ItemStream` 구현체들은 `update()` 메서드에서 이 `ExecutionContext`에 자신의 현재 실행 정보를 저장하는데, Spring Batch의 `Step`은 여기서 저장된 정보를 메타데이터 저장소에 안전하게 보관한다.
* `update()` 메서드는 매 트랜잭션의 커밋 직전에 호출된다. 단, 처리 도중 예외가 발생하여 트랜잭션이 롤백되는 경우에는 호출되지 않는다. 이는 실패한 처리 내용이 실행 정보에 반영되는 것을 방지한다.
* 하지만 모든 `ItemReader`가 재시작을 지원하는 것은 아니다. `RedisItemReader`는 SCAN 명령 순서 불일치때문에 재시작을 지원하지 않는다.
