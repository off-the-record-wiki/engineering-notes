# Java ‐ Object

#### Object 클래스

```java
public class Parent {

	public void parentMethod() {
		System.out.println("Parent.parentMethod");
	}
}
```
```java
public class Child extends Parent {

	public void childMethod() {
		System.out.println("Child.childMethod");
	}
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e16af0be-1864-4560-b8a0-0ca3c9466a2e.png)

* 자바에서 모든 객체의 최종 부모는 Object이다.
* 공통 기능 제공
  * 객체의 정보를 제공하는 `toString()`
  * 객체의 같음을 비교하는 `equals()`
  * 객체의 클래스 정보를 제공하는 `getClass()`
  * 그 외 기타 여러가지 기능
* 다형성의 기본 구현
  * Object는 모든 클래스의 부모 클래스로 모든 객체를 참조할 수 있다.

#### Object 다형성

```java
public class ObjectMain {

	public static void main(String[] args) {

		Car car = new Car();
		Dog dog = new Dog();

		action(car);
		action(dog);
	}

	private static void action(Object object) {
		if (object instanceof Car car) {
			car.sound();
		} else if (object instanceof Dog dog) {
			dog.sound();
		}
	}
}
```

* Object 타입에는 `sound()` 메서드가 존재하지 않는다.
* 따라서 아래로 내려가야하는데 이 과정을 다운 캐스팅이라고 한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/71662bbc-011f-442f-bc3f-91cfe788f72c.png)

* Object를 활용한 다형성의 한계
  * Object는 모든 객체를 대상으로 다형적 참조를 할 수 있다.
  * Object를 통해 전달 받은 객체를 호출하려면 각 객체에 맞는 다운캐스팅 과정이 필요하다.

#### Object 배열

* Object는 모든 클래스의 부모이다. 따라서 모든 타입을 받을 수 있다.

```java
public class ObjectMain2 {

	public static void main(String[] args) {

		Car car = new Car();
		Dog dog = new Dog();

		Object[] objects = {car, dog};

		action(objects[0]);
		action(objects[1]);
	}

	private static void action(Object object) {
		if (object instanceof Car car) {
			car.sound();
		} else if (object instanceof Dog dog) {
			dog.sound();
		}
	}
}
```

#### toString()

* `toString()` 메서드는 객체 정보를 문자열 형태로 제공한다. 
* 이 메서드는 Object 클래스에 정의되므로 모든 클래스에서 상속받아 사용할 수 있다.
* 기본적으로 `toString()` 메서드로 제공되는 정보는 패키지를 포함한 객체의 이름과 객체의 참조값(해시코드)를 16진수로 제공한다.
* 하지만 기본적으로 제공되는 정보는 한 눈에 파악하기 어려워 메서드를 재정의해서 유용한 정보를 제공하도록 하는 것이 좋다.

```java
public class Car {

	private String carName;

	public Car(String carName) {
		this.carName = carName;
	}

	// toString() 메서드 재정의(오버라이딩)
	@Override
	public String toString() {
		return "Car{" +
				"carName='" + carName + '\'' +
				'}';
	}
}
```

#### Object와 OCP

* 객체 정보를 사용할 때, **구체적인 것에 의존한다면 메서드의 개수가 비례해서 늘어나기에 추상적인 것에 의존하는 것이 좋다.**
* 상위 타입으로 올라갈수록 추상적인 것이고, 하위 타입으로 내려갈수록 구체적인 것이다.

```java
public class ObjectPrinter {

        // Object 파라미터 사용 - 추상적인 것에 의존
	public static void print(Object object) {
		System.out.println(object);
	}
}
```

> 정적 의존관계 vs 동적 의존관계
>
> * 정적 의존관계 : 컴파일 시기에 결정되며, 주로 클래스 간의 관계를 말한다.<br>
> * 동적 의존관계 : 프로그램 실행(런타임) 도중에 결정된다.<br>

#### equals()

* 자바는 두 객체가 같다라는 표현을 2가지로 분리해서 제공한다.
  * 동일성(Identity) : 객체 참조 주소가 같은지(두 객체의 참조가 동일한 객체를 가리키고 있는지 확인)
  * 동등성(Equality) : 객체의 값이 같은지(두 객체가 논리적으로 동등한지 확인)

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

* 기본적으로 Object 클래스에서 제공해주는 `equals()` 메서드는 참조 값 주소를 기반으로 비교를 하게 되어있다.
* 따라서 이 부분을 도메인 측에서 별도로 `equals()`와 `hashCode()` 메서드를 오버라이딩 해야 한다.

```java
public class User {

	private String name;
	private int age;

	public User(String name, int age) {
		this.name = name;
		this.age = age;
	}

        // equals() 메서드 오버라이딩
	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		User user = (User) object;
		return age == user.age && Objects.equals(name, user.name);
	}

        // hashCode() 메서드 오버리이딩
	@Override
	public int hashCode() {
		return Objects.hash(name, age);
	}
}
```
