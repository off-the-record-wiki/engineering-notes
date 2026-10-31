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
