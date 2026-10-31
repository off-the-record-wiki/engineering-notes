# spring-batch

Spring Batch 실습용 프로젝트 골격이다. Spring Boot 4.1.2, Java 25, MySQL 8.4 기준.

## 실행

Docker 가 켜져 있어야 한다. `compose.yaml` 의 MySQL 을 자동으로 띄운다.

```bash
./gradlew bootRun
```

Job 을 하나만 등록했을 때는 그 Job 이 실행된다. 여러 개를 등록했으면 `--args='--spring.batch.job.name=<Job 이름>'` 으로 고른다.

### parameterJob

문자열 파라미터 `name` 과 정수형 파라미터 `count` 를 받아 `Hello, <name>` 을 `count` 번 출력한다. 둘 다 필수다.
파라미터는 `이름=값,타입` 으로 넘기고, 타입을 생략하면 `String` 이다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=parameterJob name=batch count=3,java.lang.Integer'
```

같은 파라미터로 다시 실행하면 이미 완료된 JobInstance 라서 실행되지 않는다. 값을 바꿔서 실행한다.

필수 파라미터가 빠지면 Job 은 실행되지 않는다. 다만 Spring Batch 6 은 파라미터 검증 전에 JobInstance 를 만들기 때문에, 실행 기록이 없는 `BATCH_JOB_INSTANCE` 행이 남는다.

### dateTimeJob

날짜 파라미터 `targetDate`(`LocalDate`)와 날짜·시간 파라미터 `requestedAt`(`LocalDateTime`)을 받아 요일과 시각을 출력한다.
값은 `LocalDate` 는 `2026-10-31`, `LocalDateTime` 은 `2026-10-31T09:30:15` 형식(ISO)으로 넘긴다. 파라미터는 지정한 타입 그대로 `BATCH_JOB_EXECUTION_PARAMS` 에 저장된다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=dateTimeJob targetDate=2026-10-31,java.time.LocalDate requestedAt=2026-10-31T09:30:15,java.time.LocalDateTime'
```

Job 이 여러 개라서 `--spring.batch.job.name` 으로 실행할 Job 을 지정해야 한다.

## 검사

Testcontainers 로 MySQL 을 띄워 배치 메타 테이블이 생성되는지 확인한다.

```bash
./gradlew build
```