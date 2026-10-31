# Spring Batch - SYSTEM BUILD

#### 태스크릿(Tasklet) 지향 처리

* 태스크릿(Tasklet) 지향 처리 모델은 Spring Batch에서 가장 기본적인 Step 구현 방식으로 비교적 복잡하지 않은 단순한 작업을 실행할 때 사용된다.
* 일반적인 Spring Batch의 Step은 대부분 대량 데이터 처리(읽고-처리하고-쓰기)하는 ETL 작업에 초점을 맞춘다.
* 하지만 때로는 단순한 시스템 작업이나 유틸성 작업이 필요할 때가 있다.
  * 매일 새벽 불필요한 로그 파일 삭제
  * 특정 디렉토리에서 오래된 파일을 아카이브
  * 사용자에게 단순한 알림 메시지 또는 이메일 발송
  * 외부 API 호출 후 결과를 단순히 저장하거나 로깅
* 이처럼 단일 비즈니스 로직 실행에 초점을 맞춘 작업들이 있다. 태스크릿 지향 처리는 바로 이러한 목적을 위해 설계된 처리 방식이다.

#### 태스크릿 지향 처리의 구현 방식

```java
@FunctionalInterface
public interface Tasklet {
    @Nullable
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception;
}
```

#### RepeatStatus의 두 얼굴: FINISHED vs CONTINUABLE

* `RepeatStatus.FINISHED` : Step의 처리가 성공이든 실패든 상관없이 해당 Step이 완료되었음을 의미한다. 더 이상 반복할 필요 없이 다음 스텝으로 넘어가며 배치 잡은 차근차근 진행된다.
* `RepeatStatus.CONTINUABLE` : Tasklet의 `execute()`메서드가 추가로 더 실행되어야 함을 Spring Batch Step에 알리는 신호이다. Step의 종료는 보류되고 필요한 만큼 `execute()`메서드가 반복 호출된다.

#### RepeatStatus가 필요한 이유: 짧은 트랜잭션을 활용한 안전한 배치 처리

![](https://github.com/user-attachments/assets/9b527891-f695-45b4-8373-893e8a1e06b3)

* `execute()` 내부에서 while문을 사용한다면 N만 건째 처리 중 예외가 발생했을 때, 이미 처리한 이전 데이터도 모두 롤백되어 하나도 정리되지 않은 상태로 돌아간다. 반면 `RepeatStatus.CONTINUABLE`로 반복한다면 매 만 건 처리마다 트랜잭션이 커밋되므로 예외가 발생하더라도 처리된 데이터는 이미 안전하게 정리된 상태로 남는다.
* 결국 `RepeatStatus`를 반환해 `execute()`를 반복 실행하도록 하는 이유는 거대한 하나의 트랜잭션 대신 작은 트랜잭션들로 나누어 안전하게 처리하기 위해서다.

#### ResourcelessTransactionManager

* `ResourcelessTransactionManager`는 `no-op(아무것도 하지 않는)` 방식으로 동작하는 `PlatformTransactionManager` 구현체로 이를 사용하면 불필요한 DB 트랜잭션 처리를 생략할 수 있다.
* `PlatformTransactionManager` 빈을 직접 정의할 땐 주의가 필요하다. Spring Batch는 내부적으로 Job, Step의 상태와 같은 메타데이터를 DB를 통해 관리한다. 이 때도 트랜잭션이 사용되는데 별도의 구성 변경 없이 `PlatformTransactionManager`를 빈으로 정의할 경우 Step의 비즈니스 로직 처리를 위한 트랜잭션과 메타데이터 관리를 위한 트랜잭션이 서로 다른 성격임에도 불구하고 같은 `PlatformTransactionManager` 빈을 사용하게 되어 의도치 않은 문제가 발생할 수 있다.

#### Chunk – 데이터를 작은 덩어리로 나누어 처리하는 방식

![](https://github.com/user-attachments/assets/b644ccab-3678-40ad-9ff2-ff47b8467906)

* 청크(Chunk)는 데이터를 일정 단위로 쪼갠 덩어리를 말한다. 읽고, 처리하고, 쓰는 작업을 일정 크기로 나눈 데이터 덩어리(청크)를 대상으로 하기 때문이다.
* 100만 건의 데이터를 한 번에 메모리로 불러오고 처리하고 저장한다면 전체 시스템이 다운되기에 청크라는 것이 존재한다.

1. 메모리를 지켜라 – 데이터 폭탄 방지

* 대용량 데이터를 한 번에 메모리에 올리는 것은 문제가 될 수 있다. DB에서 데이터를 불러오는 순간 메모리 과부하로 시스템이 뻗는다. 하지만 청크 방식은 나눠서 불러와서 메모리 사용량은 안정적이고 시스템은 무리 없이 데이터 처리를 이어갈 수 있다.

2. 가벼운 트랜잭션 – 작은 실패

* 트랜잭션은 작업 성공 또는 실패를 하나의 단위로 묶는 것이라고 볼 수 있다. 대용량 데이터 자체를 하나의 트랜잭션으로 처리할 경우 작업 중간에 오류가 발생하면 전체 데이터 모두가 롤백된다.

![](https://github.com/user-attachments/assets/6396ae01-92fc-47ce-8b59-390472ad1887)

* 청크 처리 과정에서 작업 중간에 에러가 발생하면 이전 청크들은 모두 커밋 완료가 되었고 에러가 발생한 청크만 롤백되고 해당 청크부터 재시작하면 된다.

#### 청크 지향 처리 - 읽고 깎고 쓴다

1. `ItemReader` - 데이터를 읽는다.

```java
public interface ItemReader<T> {
    T read() throws Exception, 
        UnexpectedInputException, 
        ParseException, 
        NonTransientResourceException;
}
```

* `read()` 메서드는 아이템을 하나씩 반환한다. 여기서 아이템이란 파일의 한 줄 또는 데이터베이스의 한 행(Row)에 해당하는 데이터 하나를 의미한다.
* `null`을 반환하면 Step의 종료 시점이라는 것을 의미한다. Spring Batch가 Step의 완료를 판단하는 조건이다.

2. `ItemProcessor` - 데이터를 가공한다.

```java
public interface ItemProcessor<I, O> {
    O process(I item) throws Exception;
}
```
* `process()` 메서드에서는 입력 데이터(I)를 원하는 형태(O)로 반환한다.
* `process()` 메서드가 `null`을 반환하면 입력 데이터가 처리 흐름에서 제외된다. 다시 말해 `ItemWriter`로 전달되지 않는다. 유효하지 않은 데이터나 처리할 필요가 없는 데이터를 걸러낼 때 사용한다.
* 입력 데이터의 유효성을 검사한다. 필터링과 달리 조건에 맞지 않는 데이터의 경우 예외를 발생시킨다. 예를 들어, 필수 필드 누락이나 잘못된 데이터 형식을 발견했을 때 예외를 던져 배치 잡을 중단시킨다.
* `ItemProcessor`는 생략 가능하다.

3. `ItemWriter` - 데이터를 반영한다.

```java
public interface ItemWriter<T> {
    void write(Chunk<? extends T> chunk) throws Exception;
}
```

* 
