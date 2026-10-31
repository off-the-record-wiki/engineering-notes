# Spring Batch - SYSTEM EXECUTE

#### JobParameters란?

* JobParameters는 배치 작업에 전달되는 입력 값이다. 이 값은 배치가 어떤 조건에서 어떤 데이터를 다룰지를 결정한다.

#### 프로퍼티와 JobParameters의 결정적 차이

* 입력값 동적 변경
* 메타데이터 : JobParameters의 모든 값을 메타데이터 저장소에 기록하며 이를 통해 Job 인스턴스 식별 및 재시작 처리, Job 실행 이력 추적 등 중요한 기능을 제공한다.

#### JobParameters 전달하기 - 커맨드라인에서 잡 파라미터 전달하기

```
./gradlew bootRun --args='--spring.batch.job.name=dataProcessingJob inputFilePath=/data/input/users.csv,java.lang.String'
```

* `parameterName` : 배치 Job에서 파라미터를 찾을 때 사용할 Key 값이다. 이 이름으로 Job 내에서 파라미터에 접근할 수 있다.
* `parameterValue` : 파라미터 실제 값
* `parameterType` : 파라미터의 타입(`java.lang.String`)과 같은 fully qualified name 사용. 이 파라미터 타입을 명시하지 않을 경우 Spring Batch는 해당 파라미터를 `String` 타입으로 가정한다.
* `identificationFlag` : Spring Batch에게 해당 파라미터가 JobInstance 식별(identification)에 사용될 파라미터인지 여부를 전달하는 값으로 `true`이면 식별에 사용된다는 의미다. 이 플래그는 생략 가능하며 생략할 경우 `true`로 설정된다.

#### 다양한 타입의 Job 파라미터 - 기본 데이터 타입 파라미터 전달

* 
