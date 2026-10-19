# Java - Stream API

#### 스트림 API

* 스트림(Stream)은 자바 8부터 추가된 기능으로 데이터 흐름을 추상화해서 다루는 도구이다.
* 컬렉션 또는 배열 등의 요소들을 연산 파이프라인을 통해 연속적인 형태로 다루는 것이다.

> 파이프라인
>
> * 스트림이 여러 단계를 거쳐 변환되고 처리되는 모습이 마치 물이 여러 파이프를 타고 이동하면서 정수 시설이나 필터를 거치는 과정과 유사하기 때문에 파이프라인이라는 용어를 사용한다.

#### 스트림의 특징

* 데이터 소스를 변경 X : 스트림에서 제공하는 연산들은 원본 컬렉션을 변경하지 않고 결과만 새로 생성한다.
* 일회성(1회 소비) : 한 번 사용된 스트림은 다시 사용할 수 없으며 필요하다면 새로 스트림을 생성해야 한다.
* 파이프라인 구성 : 중간 연산들이 이어지다가 최종 연산을 만나면 연산이 수행되고 종료된다.
* 지연 연산 : 중간 연산은 실제로 필요할 때까지 동작하지 않고 최종 연산이 실행될 때 한 번에 처리된다.
* 병렬 : 스트림으로부터 병렬 스트림을 쉽게 만들 수 있어 멀티코어 환경에서 병렬 연산을 비교적 단순한 코드로 작성할 수 있다.

#### 지연 연산

* 스트림 API에서 지연 연산이란, `filter`, `map`과 같은 중간 연산들은 `toList`와 같은 최종 연산이 호출되기 전까지 실제로 실행되지 않는다는 의미이다.
* 즉, 중간 연산들은 결과를 바로 계산하지 않고, "무엇을 해야할지"에 대한 설정만을 저장해둔다.
* 그리고 최종 연산이 실행되는 그 순간, 그때서야 중간 연산이 순차적으로 한 번에 수행된다.
* 불필요한 연산 생략 가능
  * `findFirst()`, `limit()`와 같은 단축 연산을 사용하면 결과를 찾은 시점에서 더 이상 나머지 요소들을 처리할 필요가 없다.
* 메모리 사용 효율
  * 중간 연산 결과를 매 단계마다 별도 자료구조에 저장하지 않고 최종 연산 때까지 필요할 때만 가져와서 처리한다.
* 파이프라인 최적화
  * 스트림은 요소를 하나씩 꺼내면서 연산을 묶어서 실행할 수 있다.
  * 이렇게 하면 메모리를 절약할 수 있고 짜잘짜잘하게 중간 단계마다 저장하지 않아도 되므로 내부적으로 효율적으로 동작하게 된다.

#### 스트림 생성

* 스트림은 자바 8부터 추가된 기능으로 데이터 처리에 있어서 간결하고 효율적인 코드 작성을 가능하게 해준다.
* 스트림을 이용하면 반복문(외부 반복)없이도 간단하게 필터링, 정렬, 변환 등의 작업을 적용할 수 있다.
* 특히 스트림은 중간 연산과 최종 연산을 구분하며, 지연 연산을 통해 불필요한 연산을 최소화한다.

|생성 방법|코드 예시|특징|
|:-----:|:-----:|:-----:|
|컬렉션|`list.stream()`|`List`,`Set`등 컬렉션에서 스트림을 생성|
|배열|`Arrays.stream(arr)`|배열에서 스트림을 생성|
|Stream.of()|`Stream.of("a", "b", "c")`|직접 요소를 입력해 스트림을 생성|
|무한 스트림(iterate)|`Stream.iterate(0, n -> n + 2)`|무한 스트림 생성(초기값 + 함수)|
|무한 스트림(generate)|`Stream.generate(Math::random)`|무한 스트림 생성(함수형 인터페이스 사용)|

#### 중간 연산

|연산|설명|
|:----:|:----:|
|`filter`|조건에 맞는 요소만을 남김|
|`map`|요소를 다른 형태로 변환|
|`flatMap`|중첩 구조 스트림을 일차원으로 평탄화|
|`distinct`|중복 요소 제거|
|`sorted`|요소 정렬|
|`peek`|중간 처리(로그, 디버깅)|
|`limit`|앞에서 N개의 요소만 추출|
|`skip`|앞에서 N개의 요소를 건너뛰고 이후 요소만 추출|
|`takeWhile`|조건을 만족하는 동안 요소 추출|
|`dropWhile`|조건을 만족하는 동안 요소를 버리고 이후 요소를 추출|

* 중간 연산은 파이프라인 형태로 연결할 수 있으며, 스트림을 변경하지만 원본 데이터 자체를 변경하지 않는다.
* 중간 연산은 lazy하게 동작하므로 최종 연산이 실행될 때까지는 실제 처리가 일어나지 않는다.
* peek은 디버깅을 목적으로 자주 사용하며 실제 스트림 요소값을 변경하거나 연산 결과를 반환하지 않는다.
* takeWhile, dropWhile은 정렬된 스트림에서 사용할 때 유용하다.

#### FlatMap

* 각 요소를 스트림으로 변환한 뒤, 그 결과를 하나의 스트림으로 평탄화해준다.

```json
# Before
[
  [1, 2],
  [3, 4],
  [5, 6],
]
```

```json
# After
[1, 2, 3, 4, 5, 6]
```

#### 최종 연산

|연산|설명|특징|
|:-------:|:-------:|:-------:|
|`collect`|Collector를 사용하여 결과 수집(다양한 형태로 변환 가능)|`stream.collect(Collectors.toList())`|
|`toList()`|스트림을 불변 데이터로 수집|`stream.toList()`|
|`toArray`|스트림을 배열로 변환|`stream.toArray(Integer[]::new)`|
|`forEach`|각 요소에 대해 동작 수행|`stream.forEach()`|
|`count`|요소 개수 반환|`stream.count()`|
|`reduce`|누적 함수를 사용해 모든 요소를 단일 값으로 합침, 초기값이 없다면 Optional로 반환|`stream.reduce`|
|`min/max`|최소값, 최대값을 Optional로 반환|`stream.min()`, `stream.max()`|
|`findFirst`|조건에 맞는 첫 번째 요소(Optional 반환)|`stream.findFirst()`|
|`findAny`|조건에 맞는 아무 요소(Optional 반환)|`stream.findAny()`|
|`anyMatch`|하나라도 조건을 만족하는지|`stream.anyMatch(n -> n > 5)`|
|`allMatch`|모든 조건을 만족하는지|`stream.allMatch(n -> n > 0)`|
|`noneMatch`|하나도 조건을 만족하지 않는지|`stream.noneMatch(n -> n < 0)`|

#### 기본형 특화 스트림

* 스트림 API에는 기본형 특화 스트림이 존재한다.
* 오토박싱/언박싱 비용을 줄여 성능도 향상시킬 수 있다.
* 기본형 특화 스트림은 합계, 평균 등 자주 사용하는 연산들을 편리한 메서드로 제공하고 타입 변환과 박싱/언박싱 메서드도 제공해 다른 스트림과 연계해 작업하기가 수월하다.

|스트림 타입|대상 원시 타입|
|:----:|:----:|
|IntStream|int|
|LongStream|long|
|DoubleStream|double|

* 성능 차이 정리 - 전통적 for문 vs 스트림 vs 기본형 특화 스트림
  * 전통적 for문이 보통 가장 빠르다.
  * 스트림보다 전통적 for문이 약 1.5배 ~ 2배 정도 빠르다.
  * 박싱/언박싱 오버헤드가 발생한다.
  * 기본형 특화 스트림은 전통적 for문에 가까운 성능을 보인다.
  * 전통적 for문과 거의 비슷하거나 전통적 for문이 10% ~ 30% 정도 빠르다.
  * 박싱/언박싱 오버헤드를 피할 수 있다.
  * 내부적으로 최적화된 연산을 수행한다.
* 성능 차이는 대부분의 일반적인 애플리케이션에서 거의 차이가 없다. 이런 차이를 체감하려면 한 번에 사용하는 루프가 최소한 수천만 건 이상이어야 한다.
* 박싱/언박싱을 많이 유발하지 않는 상황이라면 일반 스트림과 기본형 특화 스트림 간 성능 차이가 그리 크지 않을 수 있다.
* 대규모 데이터 처리나 반복 횟수가 많은 경우라면 기본형 특화 스트림이 효과적일 수 있으며, 성능 극대화가 필요한 상황에서는 여전히 for문 루프가 더 빠른 경우가 있다. 실제 프로젝트에서는 극단적 성능이 필요한 경우가 아니라면 코드 가독성과 유지보수성을 위해 스트림 API를 사용하는 것이 더 나은 선택이 될 수 있다.

#### 컬렉터와 다운 스트림 컬렉터

```java
public class CollectorMain {
	public static void main(String[] args) {

		// 기본 기능
		List<String> list = Stream.of("Java", "Spring", "JPA")
				.collect(Collectors.toList());
		System.out.println("list = " + list);

		// 수정 불가능 리스트
		List<Integer> unmodifiableList = Stream.of(1, 2, 3)
				.collect(Collectors.toUnmodifiableList());
		//unmodifiableList.add(4); // 런타임 예외
		System.out.println("unmodifiableList = " + unmodifiableList);

		Set<Integer> set = Stream.of(1, 2, 2, 3, 3, 3)
				.collect(Collectors.toSet());
		System.out.println("set = " + set);

		// 타입 지정
		Set<Integer> treeSet = Stream.of(3, 4, 5, 2, 1)
				.collect(Collectors.toCollection(TreeSet::new));
		System.out.println("treeSet = " + treeSet);

		Map<String, Integer> map1 = Stream.of("Apple", "Banana", "Tomato")
				.collect(Collectors.toMap(
					name -> name, // keyMapper
					name -> name.length() // valueMapper
				));
		System.out.println("map1 = " + map1);

		Map<String, Integer> map3 = Stream.of("Apple", "Apple", "Banana")
				.collect(Collectors.toMap(
					name -> name,
					name -> name.length(),
					(oldVal, newVal) -> oldVal + newVal // 중복될 경우 기존 값 + 새 값
				));
		System.out.println("map3 = " + map3);

		// Map의 타입 지정
		LinkedHashMap<String, Integer> map4 = Stream.of("Apple", "Apple", "Banana")
				.collect(Collectors.toMap(
					name -> name,
					String::length,
					(oldVal, newVal) -> oldVal + newVal, // key 중복 발생 시
					LinkedHashMap::new // 결과 Map 타입 지정
				));
		System.out.println("map4 = " + map4.getClass());
	}
}
```

```java
public class CollectorMain {
	public static void main(String[] args) {

		// 첫 글자 알파벳을 기준으로 그룹화
		List<String> names = List.of("Apple", "Avocado", "Banana",
				"Blueberry", "Cherry");
		Map<String, List<String>> grouped = names.stream()
				.collect(Collectors.groupingBy(name -> name.substring(0, 1)));
		System.out.println("grouped = " + grouped);

		// 짝수(even)인지 여부로 분할(파티셔닝)
		List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);
		Map<Boolean, List<Integer>> partitioned = numbers.stream()
				.collect(Collectors.partitioningBy(n -> n % 2 == 0));
		System.out.println("partitioned = " + partitioned);

		// 다운스트림 컬렉터에서 유용하게 사용
		Integer max1 = Stream.of(1, 2, 3)
				.collect(Collectors.maxBy(
						((i1, i2) -> i1.compareTo(i2)))
				).get();
		System.out.println("max1 = " + max1);

		Integer max2 = Stream.of(1, 2, 3)
				.max((i1, i2) -> i1.compareTo(i2)).get();
		System.out.println("max2 = " + max2);

		Integer max3 = Stream.of(1, 2, 3)
				.max((Integer::compareTo)).get();
		System.out.println("max3 = " + max3);

		// 기본형 특화 스트림 사용
		int max4 = IntStream.of(1, 2, 3)
				.max().getAsInt();
		System.out.println("max4 = " + max4);

		// 다운스트림 컬렉터에서 유용하게 사용
		long count1 = Stream.of(1, 2, 3)
				.collect(Collectors.counting());
		System.out.println("count1 = " + count1);

		long count2 = Stream.of(1, 2, 3)
				.count();
		System.out.println("count2 = " + count2);

		// 다운스트림 컬렉터에서 유용하게 사용
		double average1 = Stream.of(1, 2, 3)
				.collect(Collectors.averagingInt(i -> i));
		System.out.println("average1 = " + average1);

		// 기본형 특화 스트림으로 변환
		double average2 = Stream.of(1, 2, 3)
				.mapToInt(i -> i)
				.average().getAsDouble();
		System.out.println("average2 = " + average2);

		// 기본형 특화 스트림 사용
		double average3 = IntStream.of(1, 2, 3)
				.average().getAsDouble();
		System.out.println("average3 = " + average3);

		// 통계
		IntSummaryStatistics stats = Stream.of("Apple", "Banana", "Tomato")
				.collect(Collectors.summarizingInt(String::length));
		System.out.println(stats.getCount()); // 3
		System.out.println(stats.getSum()); // 17 (5+6+6)
		System.out.println(stats.getMin()); // 5
		System.out.println(stats.getMax()); // 6
		System.out.println(stats.getAverage()); // 5.66...

		List<String> alphabets = List.of("a", "b", "c", "d");

		// 모든 이름을 하나의 문자열로 이어 붙이기
		String joined1 = names.stream()
				.collect(Collectors.reducing(
						(s1, s2) -> s1 + "," + s2
				)).get();
		System.out.println("joined1 = " + joined1);

		String joined2 = names.stream()
				.reduce((s1, s2) -> s1 + "," + s2).get();
		System.out.println("joined2 = " + joined2);

		// 문자열 전용 기능
		String joined3 = names.stream()
				.collect(Collectors.joining(","));
		System.out.println("joined3: " + joined3);

		String joined4 = String.join(",", "a", "b", "c", "d");
		System.out.println("joined4: " + joined4);
	}
}
```

```java
public class DownStreamCollectorMain {
	public static void main(String[] args) {

		List<Student> students = List.of(
				new Student("Kim", 1, 85),
				new Student("Park", 1, 70),
				new Student("Lee", 2, 70),
				new Student("Han", 2, 90),
				new Student("Hoon", 3, 90),
				new Student("Ha", 3, 89)
		);

		// 1단계: 학년별로 학생들을 그룹화 해라.
		Map<Integer, List<Student>> collect1_1 = students.stream()
				.collect(Collectors.groupingBy(
						Student::getGrade, // 그룹화 기준: 학년
						Collectors.toList() // 다운스트림1: 학생을 리스트로 수집
				));
		System.out.println("collect1_1 = " + collect1_1);

		// 다운스트림에서 toList() 생략 가능
		Map<Integer, List<Student>> collect1_2 = students.stream()
				.collect(Collectors.groupingBy(Student::getGrade));
		System.out.println("collect1_2 = " + collect1_2);

		// 2단계: 학년별로 학생들의 이름을 출력해라.
		Map<Integer, List<String>> collect2 = students.stream()
				.collect(Collectors.groupingBy(
						Student::getGrade, // 그룹화 기준: 학년
						Collectors.mapping(Student::getName, // 다운스트림 1: 학생 -> 이름 변환
		Collectors.toList() // 다운스트림 2: 변환된 값(이름)을 List로 수집
		)));
		System.out.println("collect2 = " + collect2);

		// 3단계: 학년별로 학생들의 수를 출력해라.
		Map<Integer, Long> collect3 = students.stream()
				.collect(Collectors.groupingBy(
						Student::getGrade,
						Collectors.counting()
				));
		System.out.println("collect3 = " + collect3);

		// 4단계: 학년별로 학생들의 평균 성적 출력해라.
		Map<Integer, Double> collect4 = students.stream()
				.collect(Collectors.groupingBy(
						Student::getGrade,
						Collectors.averagingInt(Student::getScore)
				));
		System.out.println("collect4 = " + collect4);
	}
}
```

> [📚Java Streams: The Good, The Bad, and The Ugly](https://medium.com/@harshgajjar7110/java-streams-the-good-the-bad-and-the-ugly-6f6b54526619)
> 
> **장점**
> 
> **1. 간결하고 읽기 쉬운 코드**
> * 스트림은 데이터 모음을 조작하는 선언적 방법을 제공하여 코드를 더욱 간결하고 읽기 쉽게 만든다.
>
> **2. 병렬 처리**
> * 스트림은 쉽게 병렬화할 수 있으므로 대규모 데이터 세트를 효율적으로 처리할 수 있다.
>
> **3. 성능 향상**
> * 스트림은 컴파일러에 의해 최적화될 수 있으므로 코드 실행 속도가 빨라진다.
>
> **4. 함수형 프로그래밍**
> * 스트림은 불변성, 순수 함수, 관심사 분리를 장려하는 함수형 프로그래밍 원칙을 기반으로 한다.
>
> **5. 지연 평가**
> * 스트림은 지연 평가를 수행한다. 즉, 필요할 때만 데이터를 처리하므로 성능이 향상되고 메모리 사용량이 줄어든다.
>
> **단점**
>
> **1. 학습 곡선이 가파름**
> * 스트림은 함수형 프로그래밍 개념에 대한 확실한 이해를 요구하는데, 이는 명령형 프로그래밍에 익숙한 개발자에게는 어려울 수 있다.
>
> **2. 디버깅**
> * 스트림 디버깅은 까다로울 수 있는데, 코드가 추적하기 어려운 여러 개의 연결된 작업으로 구성되는 경우가 많기 때문이다.
>
> **3. 부작용**
> * 스트림은 상태를 유지하지 않고 변경되지 않도록 설계되었기 때문에 부작용에 의존하는 특정 작업을 수행하기 어려울 수 있다.
>
> **주의사항**
>
> * 스트림을 과도하게 사용하면 코드가 지나치게 복잡해지고 유지 관리가 어려워질 수 있다.
> * 스트림은 많은 경우 성능을 향상시킬 수 있지만, 제대로 사용하지 않으면 성능 병목 현상을 초래할 수 있다.
> * 스트림은 Java 8에서 도입되었으므로 이전 버전의 Java와는 호환되지 않는다.
