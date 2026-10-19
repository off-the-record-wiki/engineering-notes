# Java - Method Reference

#### 메서드 참조(Method Reference)

* 이미 정의된 메서드를 람다처럼 간결하게 표현하기 위해 등장한 개념
  * 메서드 참조를 사용하면 코드가 더욱 간결해지고 가독성이 향상된다.
  * 매개변수를 명시적으로 작성할 필요가 없어진다.
  * 별도의 로직 분리와 함께 재사용성이 높아진다.

#### 메서드 참조의 4가지 유형

* 정적 메서드 참조
* 특정 객체 인스턴스 메서드 참조
* 생성자 참조
* 임의 객체의 인스턴스 메서드 참조

```java
public class MethodRefMain {
	public static void main(String[] args) {

		// 정적 메서드
		Supplier<String> staticMethods1 = () -> Person.greeting();
		Supplier<String> staticMethods2 = Person::greeting;

		// 특정 객체 인스턴스 메서드 참조
		Person person = new Person("Me!");
		Supplier<String> instanceMethod1 = () -> person.introduce();
		Supplier<String> instanceMethod2 = person::introduce;

		// 생성자 참조
		Supplier<Person> newPerson1 = () -> new Person();
		Supplier<Person> newPerson2 = Person::new;

		// 정적 메서드 - 매개변수 O
		Supplier<String> staticMethodsWithParam1 = () -> Person.greetingWithName("Me!");
		Function<String, String> staticMethodsWithParam2 = Person::greetingWithName;
		System.out.println(staticMethodsWithParam2.apply("Me!"));

		// 특정 객체 인스턴스 메서드 참조 - 매개변수 O
		Supplier<String> instanceMethodWithParam1 = () -> person.introduceWithName("Me!");
		Function<String, String> instanceMethodWithParam2 = person::introduceWithName;
		System.out.println(instanceMethodWithParam2.apply("Me!"));

		// 생성자 참조 - 매개변수 O
		Supplier<Person> newPersonWithParam1 = () -> new Person("Me!");
		Function<String, Person> newPersonWithParam2 = Person::new;
		Person newPerson = newPersonWithParam2.apply("Me!");
		System.out.println(newPerson.getName());
	}
}
```

* 메서드 참조에서 매개변수를 생략하는 이유
  * 함수형 인터페이스 시그니처가 이미 정해져 있고, 컴파일러가 그 시그니처를 바탕으로 메서드 참조와 연결해주기 때문에 명시적으로 매개변수를 작성하지 않아도 자동으로 추론되어 호출된다.
* 메서드 참조가 발생하는 시점에 대한 명확한 정리
  * `클래스이름::인스턴스메서드이름` 형태의 메서드 참조에서 실제 인스턴스 메서드는 람다/함수형 인터페이스의 첫 매개변수를 호출할 대상 인스턴스로 사용하고 나머지 매개변수를 메서드 인자로 전달한다.
