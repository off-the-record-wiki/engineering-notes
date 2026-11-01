# Spring Batch - SYSTEM EXECUTE

#### 청크 지향 처리

* 데이터 읽기(`ItemReader`): 데이터를 하나씩 읽어들인다. Chunk 크기만큼 반복 실행된다.
* 데이터 가공(`ItemProcessor`): 읽어들인 데이터를 원하는대로 가공한다. 필요없는 놈들은 여기서 걸러진다.
* 데이터 저장(`ItemWriter`): Chunk 단위로 모아진 데이터를 한 번에 처리한다.

#### 파일 기반 배치 처리

![](https://github.com/user-attachments/assets/6ea87e06-beeb-49a0-b645-68fead2fca4c)

* 각 라인이 하나의 데이터다. 보통 파일의 한 줄은 독립적인 하나의 레코드로 취급된다. 이 경우 줄 바꿈(\n)이 레코드의 끝을 의미한다 구분자나 고정 길이로 각 필드를 구분할 수 있다.
* 다양한 구분자를 지원한다. 
  * CSV: 쉼표(,)로 각 필드를 구분한다. 가장 널리 쓰이는 형식이다. 
  * TSV: 탭(\t)으로 구분하며, 쉼표가 데이터에 포함될 때 유용하다 `고정 길이: 각 필드`가 정해진 길이를 가진다. 레거시 시스템에서 주로 사용된다.
* 강력한 호환성과 범용성 : 거의 모든 시스템에서 읽고 쓸 수 있는 표준 형식이다 엑셀, 데이터베이스 등 다양한 도구와 호환된다 사람도 쉽게 읽을 수 있어 디버깅이 용이하다 대용량 데이터 처리에도 적합한 단순한 구조다.

#### FlatFileItemReader

* `FlatFileItemReader`의 `read()` 메서드의 핵심 동작은 크게 두 단계로 이루어진다.
  * 파일에서 한 줄을 읽어온다.
  * 읽어온 한 줄의 문자열을 우리가 사용할 객체로 변환해 리턴한다.
* 파일에서 읽어온 한 줄의 데이터는 `LineMapper`가 변환한다. `LineMapper`는 파일에서 읽어온 한 줄의 텍스트를 도메인 객체로 매핑한다.

#### DefaultLineMapper

* `DefaultLineMapper`의 동작은 크게 두 단계로 이뤄진다.
  * 토큰화(Tokenization) : 하나의 문자열 라인을 토큰 단위로 분리한다.
  * 객체 매핑 작전 : 분리된 토큰들을 도메인 객체의 프로퍼티에 매핑한다.

1. 토큰화 : 텍스트 한 줄을 의미있는 필드(token)으로 나누기

![](https://github.com/user-attachments/assets/b1392aa0-4d83-448e-869c-47ba0d8bdf5f)

* Spring Batch에는 대표적으로 두 가지 종류의 `LineTokenizer` 구현체가 있다. 하나는 구분자로 구분된 형식의 라인을 토큰화하는 `DelimitedLineTokenizer`이고, 다른 하나는 고정 길이로 구분된 데이터를 토큰화하는 `FixedLengthTokenizer`이다.
* `DelimitedLineTokenizer` : 쉼표(,)와 같은 구분자로 구분된 데이터를 토큰화한다.
* `FixedLengthTokenizer` : 고정 길이로 구분된 데이터를 토큰화한다.

2. 객체 매핑 : 토큰화된 필드들(`FieldSet`)을 객체에 매핑하기

![](https://github.com/user-attachments/assets/4f979542-425a-4a2c-9816-f46f76a7df61)

* `FieldSet`을 객체로 매핑하는 일은 `DefaultLineMapper`의 또 다른 부하, `FieldSetMapper`가 담당한다.
* `FieldSetMapper`의 `mapFieldSet()` 메서드는 `LineTokenizer`가 만든 `FieldSet` 객체를 입력 받아 우리가 원하는 자바 객체(`SystemFailure`)로 최종 변환한다.
* 별도 설정이 없다면 `BeanWrapperFieldSetMapper`가 기본 `FieldSetMapper`로 사용된다.

```java
public interface FieldSetMapper<T> {
    T mapFieldSet(FieldSet fieldSet) throws BindException;
}
```

#### [FlatFileItemReader 구성](https://docs.spring.io/spring-batch/reference/readers-and-writers/flat-files/file-item-reader.html)

* `name()` : `ItemReader`의 식별자 지정. 
* `resource()` : 타겟 설정. 읽어들일 `Resource`를 지정한다.
* `delimited()` : 파일 형식 지정.
* `.delimiter()` : 구분자 지정. 읽어들일 파일이 구분자로 분리된 형식임을 설정했으니 이제 어떤 문자를 구분자로 사용할지 지정하는 역할을 한다.
* `.names()` : 프로퍼티 매핑. `FieldSet`의 `names` 필드에 사용할 객체의 프로퍼티 이름을 전달한다. 
* `.targetType()` : 매핑 대상 클래스 지정. 기본으로 사용되는 `FieldSetMapper` 구현체인 `BeanWrapperFieldSetMapper`에서 `FieldSet`을 객체로 매핑할 대상 도메인 클래스를 지정한다.
* `.linesToSkip()` : 헤더 처리.
* `.strict()` : 파일 검증 강도 설정. 파일과 데이터 검증의 강도를 설정하는 메서드로 기본값은 `true`이다. 이 경우 파일 누락 시 예외를 발생시켜 배치를 중단하고 `false`면 파일이 존재하지 않아도 경고만 남기고 진행한다. 이 설정은 `LineTokenizer`의 검증 강도에도 영향을 미친다. `LineTokenizer`가 토큰화한 `tokens`의 길이가 `names()`에 전달된 객체 프로퍼티 이름(names)의 길이와 다를 경우 `strict=true`면 예외가 발생하고 `strict=false`면 토큰 수를 자동으로 보정한다.

#### 고정 길이 형식의 파일 읽기

* 고정 길이 형식의 파일은 각 필드가 고정된 길이로 딱 맞춰진 텍스트 파일이다. 여기서는 구분자란 존재하지 않는다.
* `.fixedLength()` : `FlatFileItemReader`에게 읽어들일 파일이 고정 길이 형식임을 알리는 설정이다. 이 메서드를 호출하면 `DefaultLineMapper`가 사용할 `LineTokenizer` 구현체로 `FixedLengthTokenizer`가 지정된다. `FixedLengthTokenizer`는 각 필드의 길이가 고정된 데이터를 토큰화하여 읽어들이는 역할을 한다.
* `.columns()` : 고정 길이 파일을 읽기 위해 각 필드의 정확한 위치를 `FixedLengthTokenizer`에 전달한다. `Range`배열은 각 필드의 시작과 끝 위치를 정한다.
* `.strict()` : `strict` 설정은 앞서 설명한 파일 존재 여부와 토큰 개수 불일치에 대한 처리 외에도, 고정 길이 파일에서 추가적인 의미를 갖는다. `strict` 모드에서 `FixedLengthTokenizer`는 파일에서 읽은 라인의 길이를 엄격하게 검증한다. 파일에서 읽은 라인의 길이가 `Range`에 지정된 최대 길이(마지막 필드의 끝 위치)와 다를 경우 예외를 발생시킨다. 이는 고정 길이 파일의 데이터 정확성을 보장하는 핵심 메커니즘이다.
* `.names()`와 `.targetType()` : 구분자로 구분된 형식의 파일을 처리할 때와 동일하게 동작한다. `BeanWrapperFieldSetMapper`에서 `FieldSet`을 도메인 객체로 변환하는 과정은 파일 형식과 무관하다.

#### RegexLineTokenizer

* 구조가 복잡하고 라인의 길이도 가변적인 경우, 정규식을 활용한 `RegexLineTokenizer`가 유용한 도구가 된다.

```java
tokenizer.setRegex("\\[\\w+\\]\\[Thread-(\\d+)\\]\\[CPU: \\d+%\\] (.+)");
```
* `\\[\\\\w+\\]` : 대괄호 안의 로그 레벨(예: WARNING, ERROR)을 패턴으로 매칭한다. 이 부분은 분석 대상에서 제외된다.
* `\\[Thread-(\\\\d+)\\]` : 스레드 번호에 해당하는 두 번째 대괄호 안에서 Thread- 뒤에 나오는 숫자가 첫 번째 그룹으로 캡처된다.
* `\\[CPU: \\\\d+%\\]` : CPU 사용량을 나타내는 부분이다. 이건 로그 메시지 파싱엔 필요 없으니 건너뛴다.
* `(.+)` : 마지막으로 로그 메시지를 전부 가져오는 부분이다. 이게 두 번째 그룹으로 캡처된다.

#### PatternMatchingCompositeLineMapper

![](https://github.com/user-attachments/assets/5501ed18-df41-4f9b-bdc6-b8a739526094)

* 각 라인은 서로 다른 구조와 형식을 가지고 있다. 기본으로 사용되는 `LineMapper` 구현체인 `DefaultLineMapper`는 하나의 `LineTokenizer`와 하나의 `FieldSetMapper`만을 가지고 있어 이런 다양한 형식의 파일을 처리할 수 없다.
* 이런 상황에서 `PatternMatchingCompositeLineMapper`를 사용할 수 있다.

#### RecordFieldSetMapper

* 파일의 데이터를 record로 매핑하는 역할을 한다.

#### MultiResourceItemReader

![](https://github.com/user-attachments/assets/499e8cbf-0916-4cf7-85ca-410823200f62)

* `MultiResourceItemReader`는 여러 파일을 순차적으로 읽을 수 있게 해주는 `ItemReader` 구현체이다.
* `resources()` : `resources()` 메서드에 읽을 대상 `Resource`의 목록을 배열 형태로 구성한다.
* `delegate()` : 실제 데이터 읽기를 수행할 위임 대상 `ItemReader`를 `delegate()` 메서드에 구성한다.

#### FlatFileItemWriter

![](https://github.com/user-attachments/assets/5233bbba-8d23-434d-a34b-57e5fc1854c6)

1. `FieldExtractor`(필드 추출)

* `FieldExtractor`는 도메인 객체에서 필드를 추출하는 역할을 한다.

```java
public interface FieldExtractor<T> {
    Object[] extract(T item);
}
```

![](https://github.com/user-attachments/assets/45a88e03-c445-498c-8dcf-394a49ca56c2)

2. `LineAggregator`(문자열 결합)

* `LineAggregator`는 `FieldExtractor`에서 추출한 데이터들을 하나의 문자열로 결합하는 역할을 맡는다.
* 구분자 기반 형식의 파일을 읽을 때 `DelimitedLineTokenizer`가 사용되었던 것처럼 구분자 기반 형식으로 파일을 쓸 때(문자열로 결합할 때)는 `DelimitedLineAggregator`가 사용된다. 또한 고정 길이 형식을 포함한 다른 형식으로 파일을 쓸 때(문자열로 결합할 때)는 `FormatterLineAggregator`를 사용할 수 있다.

```java
public interface LineAggregator<T> {
    String aggregate(T item);
}
```

![](https://github.com/user-attachments/assets/bf23f9d9-6d61-4c9b-ac83-3ac41dbb08ee)

#### 커스텀 포맷 형식으로 파일 쓰기

* 커스텀 포맷으로 데이터를 파일에 쓰기 위해서는 `FormatterLineAggregator`를 사용해야 한다. 
* `FormatterLineAggregator`는 객체의 필드를 지정된 포맷에 맞춰 문자열로 변환한다. 고정 길이 형식의 파일이나 특별한 형식의 보고서를 만들 때 사용하기 좋다.
* `formatted()` : `FlatFileItemWriterBuilder`의 `formatted()` 메서드는 `FlatFileItemWriter`가 사용할 `LineAggregator` 구현체로 `FormatterLineAggregator`를 지정한다. `FormatterLineAggregator`는 객체의 각 필드를 지정된 포맷 문자열에 맞춰 하나의 문자열로 변환한다.
* `format()` : `FormatterLineAggregator`가 사용할 포맷 문자열을 지정한다.

#### 파일 처리 옵션

* `shouldDeleteIfExists` : 기존 파일의 처리 여부
* `append` : 기존 파일에 데이터 덧붙이기 여부
* `shouldDeleteIfEmpty` : 빈 결과 파일 처리 여부

#### FlatFileItemWriter의 롤백 전략

![](https://github.com/user-attachments/assets/39f3071f-a1ad-4488-bba4-055fead5b85b)

* DB와 달리 파일은 이미 쓰여진 데이터를 롤백할 수 없다는 문제가 있다. 이 문제를 해결하기 위해 `FlatFileItemWriter`는 데이터를 즉시 파일에 쓰지 않고 내부 버퍼에 일시적으로 저장해둔다.
* 이후 청크 처리가 정상적으로 완료되어 트랜잭션이 커밋되려고 할 때, 즉 `beforeCommit()` 콜백이 호출될 때 비로소 버퍼의 데이터를 파일에 쓴다. 덕분에 중간에 롤백이 발생하더라도 실제 파일에는 아무 데이터도 쓰이지 않아 마치 DB 롤백처럼 동작하게 된다.

#### 파일 쓰기와 OS 캐시: forceSync 옵션

* OS는 성능을 위해 매번 디스크에 직접 쓰지 않고 메모리 캐시에 데이터를 먼저 저장한다. 디스크 I/O는 비용이 크기 때문이다. 그러나 이로 인해 OS가 갑자기 중단되면 캐시의 데이터가 디스크에 쓰이지 못하고 유실될 수 있다.
* `FlatFileItemWriterBuilder`는 이러한 동작을 개발자가 손쉽게 제어할 수 있도록 `forceSync()` 메서드를 제공한다. 이 값을 `true`로 설정하면 캐시가 아닌 디스크에 즉시 동기화되어 OS 중단이나 파일 시스템 문제가 발생해도 데이터 손실 위험이 줄어든다. 다만 잦은 동기화로 인해 성능 저하가 발생할 수 있다는 점을 기억하자. 기본값은 `false`이다.

#### 대용량 파일의 분할 처리: MultiResourceItemWriter

![](https://github.com/user-attachments/assets/7c9b28ea-e73c-4fc5-bcec-bad72e5d4381)

* `name()` : M`ultiResourceItemWriter` 식별을 위한 고유 이름 지정
* `resource()` : `MultiResourceItemWriter`를 사용할 땐 위임 대상 `ItemWriter` 대신 `MultiResourceItemWriter` 구성 코드에 `Resource`를 지정해줘야 한다. 위임 대상 `ItemWriter`에 `Resource`를 지정해주는 역할은 `MultiResourceItemWriter`가 내부적으로 수행
* `itemCountLimitPerResource` : 한 파일당 최대 라인 수를 지정
* `delegate` : 실제 파일 쓰기를 수행할 위임 대상 `ItemWriter`를 설정
* `resourceSuffixCreator` : 파일 기본 이름 뒤에 붙을 접미사를 정의

#### JSON 파일 읽고 쓰기

![](https://github.com/user-attachments/assets/831f9824-7a9c-4d9b-ad6c-6086fe0e0699)

* 중첩된 객체도 표현할 수 있고, 배열도 다룰 수 있어서 현대 시스템에서 자주 사용하는 패턴이다.

#### JsonItemReader

* `JsonItemReader`는 JSON 데이터를 처리하기 위한 리더이다.
* `resource` : JSON 데이터의 소스를 지정한다. 파일뿐만 아니라 URL 등 다양한 소스에서 JSON을 읽어들일 수 있다.
* `JsonObjectReader` : JSON 파싱의 핵심 엔진이다. Jackson, Gson을 모두 제공한다.

#### JsonFileItemWriter

* `JsonFileItemWriter`는 객체를 JSON 배열 형식으로 변환해 파일로 저장하는 역할을 한다. 내부적으로 `JsonObjectMarshaller`를 사용해 객체를 JSON 문자열로 변환한다.
* `WritableResource` : JSON 데이터를 저장할 대상 파일을 나타내는 Spring의 `WritableResource`이다.
* `JsonObjectMarshaller` : 객체를 JSON 형식으로 마샬링하는 JSON 객체 변환기로 Spring Batch에서는 Jackson, Gson 두 가지 구현체를 지원한다.
