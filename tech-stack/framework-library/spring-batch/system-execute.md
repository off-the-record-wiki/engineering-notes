# Spring Batch - SYSTEM EXECUTE

#### 청크 지향 처리

* 데이터 읽기(ItemReader): 데이터를 하나씩 읽어들인다. Chunk 크기만큼 반복 실행된다.
* 데이터 가공(ItemProcessor): 읽어들인 데이터를 원하는대로 가공한다. 필요없는 놈들은 여기서 걸러진다.
* 데이터 저장(ItemWriter): Chunk 단위로 모아진 데이터를 한 번에 처리한다.

#### 파일 기반 배치 처리

![](https://github.com/user-attachments/assets/6ea87e06-beeb-49a0-b645-68fead2fca4c)

* 각 라인이 하나의 데이터다. 보통 파일의 한 줄은 독립적인 하나의 레코드로 취급된다. 이 경우 줄 바꿈(\n)이 레코드의 끝을 의미한다 구분자나 고정 길이로 각 필드를 구분할 수 있다.
* 다양한 구분자를 지원한다. 
  * CSV: 쉼표(,)로 각 필드를 구분한다. 가장 널리 쓰이는 형식이다. 
  * TSV: 탭(\t)으로 구분하며, 쉼표가 데이터에 포함될 때 유용하다 `고정 길이: 각 필드`가 정해진 길이를 가진다. 레거시 시스템에서 주로 사용된다.
* 강력한 호환성과 범용성 : 거의 모든 시스템에서 읽고 쓸 수 있는 표준 형식이다 엑셀, 데이터베이스 등 다양한 도구와 호환된다 사람도 쉽게 읽을 수 있어 디버깅이 용이하다 대용량 데이터 처리에도 적합한 단순한 구조다.

#### FlatFileItemReader

* FlatFileItemReader의 `read()` 메서드의 핵심 동작은 크게 두 단계로 이루어진다.
  * 파일에서 한 줄을 읽어온다.
  * 읽어온 한 줄의 문자열을 우리가 사용할 객체로 변환해 리턴한다.
* 파일에서 읽어온 한 줄의 데이터는 `LineMapper`가 변환한다. `LineMapper`는 파일에서 읽어온 한 줄의 텍스트를 도메인 객체로 매핑한다.

#### DefaultLineMapper

* DefaultLineMapper의 동작은 크게 두 단계로 이뤄진다.
  * 토큰화(Tokenization) : 하나의 문자열 라인을 토큰 단위로 분리한다.
  * 객체 매핑 작전 : 분리된 토큰들을 도메인 객체의 프로퍼티에 매핑한다.

1. 토큰화 : 텍스트 한 줄을 의미있는 필드(token)으로 나누기

![](https://github.com/user-attachments/assets/b1392aa0-4d83-448e-869c-47ba0d8bdf5f)

* Spring Batch에는 대표적으로 두 가지 종류의 `LineTokenizer` 구현체가 있다. 하나는 구분자로 구분된 형식의 라인을 토큰화하는 `DelimitedLineTokenizer`이고, 다른 하나는 고정 길이로 구분된 데이터를 토큰화하는 `FixedLengthTokenizer`이다.
* `DelimitedLineTokenizer` : 쉼표(,)와 같은 구분자로 구분된 데이터를 토큰화한다.
* `FixedLengthTokenizer` : 고정 길이로 구분된 데이터를 토큰화한다.

2. 객체 매핑 : 토큰화된 필드들(FieldSet)을 객체에 매핑하기

![](https://github.com/user-attachments/assets/4f979542-425a-4a2c-9816-f46f76a7df61)

* `FieldSet`을 객체로 매핑하는 일은 `DefaultLineMapper`의 또 다른 부하, `FieldSetMapper`가 담당한다.
* `FieldSetMapper`의 `mapFieldSet()` 메서드는 `LineTokenizer`가 만든 `FieldSet` 객체를 입력 받아 우리가 원하는 자바 객체(SystemFailure)로 최종 변환한다.
* 별도 설정이 없다면 `BeanWrapperFieldSetMapper`가 기본 `FieldSetMapper`로 사용된다.

```java
public interface FieldSetMapper<T> {
    T mapFieldSet(FieldSet fieldSet) throws BindException;
}
```

#### [FlatFileItemReader 구성](https://docs.spring.io/spring-batch/reference/readers-and-writers/flat-files/file-item-reader.html)

* `name()` : ItemReader의 식별자 지정. 
* `resource()` : 타겟 설정. 읽어들일 Resource를 지정한다.
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

* 구조가 복잡하고 라인의 길이도 가변적인 경우, 정규식을 활용한 RegexLineTokenizer가 유용한 도구가 된다.

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
