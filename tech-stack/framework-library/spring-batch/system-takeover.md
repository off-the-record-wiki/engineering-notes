# Spring Batch - SYSTEM TAKEOVER

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
