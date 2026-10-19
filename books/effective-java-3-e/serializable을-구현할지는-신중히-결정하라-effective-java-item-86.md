# (Effective Java Item 86) Java - Serializable을 구현할지는 신중히 결정하라

* `Serializable`은 Java에서 직렬화를 사용하기 위한 시작점이다. `int`, `long`같은 Primitive Type은 Java에서 기본적으로 직렬화를 지원하지만 객체의 경우 직렬화를 사용하기 위해서는 `Serializable`을 구현해야한다.
* `Serializable`을 객체가 구현하면 해당 객체는 Java가 지원하는 직렬화 시스템의 지원을 받을 수 있다. 사용하기는 편하지만 길게 봤을 때 값비싼 일이 될 수 있기 때문이다.
* 참고로 `Serializable` 객체를 직렬화할 때는 `ObjectOutputStream`을 사용하며 `Serializable`을 구현하지 않은 객체를 직렬화하면 `java.io.NotSerializableException`가 발생한다.

#### Serializable을 구현하면 릴리즈 뒤에는 수정하기 어렵다.

* 클래스가 `Serializable` 인터페이스를 구현하게 되면 직렬화된 바이트 스트림 인코딩도 하나의 공개 API가 된다. 때문에 이 클래스가 널리 퍼지면 그 직렬화 형태로 영원히 지원해야한다.
* `Serializable`을 구현한 순간부터 해당 객체의 직렬화 형태는 Java 직렬화 형태에 묶이는 것이 된다. 기본 직렬화 형태에서는 `private`와 `package-private` 수준의 필드마저도 API로 공개가 된다. 즉, 정보 은닉이 깨지게 되는 셈이다.

```java
public class Person implements Serializable {

    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```
```java
@Test
void serialize() throws IOException {
    Person person = new Person("pkch", 28);

    try (FileOutputStream fileOutputStream = new FileOutputStream(SERIALIZE_OBJECT_FILE_PATH)) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(person);
        }
    }
}
```
* 객체를 `ObjectOutputStream`을 통해 직렬화를 한 뒤 이를 `FileOutputStream`을 통해 객체 내용을 저장하면 private 필드가 공개되는 것을 볼 수 있다.

```java
@Test
void deserialize() throws IOException {
    Person person = null;

    try (FileInputStream fileInputStream = new FileInputStream(SERIALIZE_OBJECT_FILE_PATH)) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            person = (Person) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    assertThat(person.getName()).isEqualTo("pkch");
    assertThat(person.getAge()).isEqualTo(28);
}
```
```java
public class Person implements Serializable {
    private final String name;
    private final int age;
    private final double height;
    private final double weight;

    public Person(String name, int age, double height, double weight) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
    }
}
```
* 위와 같이 `Person` 클래스에 height, weight라는 새로운 필드가 추가된 이후 실행하면 다음과 같은 오류가 발생한다.
```
java.io.InvalidClassException: edu.pkch.serialize.Person; local class incompatible: stream classdesc serialVersionUID = -6765962567694553436, local class serialVersionUID = -2416939271889238383
```
* 기본적으로 `serialVersionUID`는 정의하지 않으면 해당 객체의 hashCode를 기반으로 설정이 되는데 height, weight가 추가되면서 `serialVersionUID`가 바뀐 것이다.
* 그렇기 때문에 이런 문제를 방지하기 위해서 `serialVersionUID`도 같이 관리해야한다.
```java
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

#### serialVersionUID의 한계

* 앞서 말했듯이 새로운 필드가 추가/제거됨에 따라 변경사항이 발생하면 직렬화/역직렬화 과정에서 예외가 발생하게 된다.
* 그러나 기존에 존재하던 변수 이름을 변경했을 때에는 해당 데이터가 누락된다는 문제가 있다.
* 변수명을 변경하는 경우 값의 누락이 있을 뿐 에러는 발생하지 않는다. 그러나 기존에 존재하는 변수의 타입을 변경하면 이야기가 달라진다.
```java
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int na2;  // 기존 int, 필드명 na2

    public Person(String name, int na2) {
        this.name = name;
        this.na2 = na2;
    }
}
```
```java
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final long age;

    public Person(String name, long age) {
        this.name = name;
        this.age = age;
    }
}
```
```
java.io.InvalidClassException: edu.pkch.serialize.Person; incompatible types for field age
```

#### 버그와 보안 구멍이 생길 위험이 높아진다.

* 객체를 생성하는 가장 기본적인 방법은 생성자를 이용하는 것이다.
* `ObjectInputStream#readObject`는 객체를 만들어 낼 수 있는 메서드이다. 즉, 객체를 `Serializable` 인터페이스로 구현하면 생성자 이외에 객체를 생성할 수 있는 숨은 생성자를 만들게 되는 것이다.
* 기본 역직렬화를 통해 불변식이 깨질 수 있으며, 허가되지 않은 접근에 쉽게 노출될 우려가 있다.

#### 해당 클래스의 신버전 릴리즈 시 테스트할 것이 늘어난다.

* `Serializable`의 문제점과 같이 구버전의 직렬화 형태가 신버전에서 역직렬화가 가능한지, 그 역도 가능한지 테스트해야한다.
* 즉, 테스트의 양이 직렬화 가능 클래스 수와 릴리즈 횟수에 비례한다.
* 릴리즈 할 때마다 반드시 양방향 직렬화/역직렬화가 가능한지 확인하고 원래의 객체를 충실히 복제가능한지를 반드시 확인해야한다.

#### Serializable 구현 여부에 신중할 것을 당부한다.

* 객체를 전송할 때나 저장할 때 Java 직렬화를 사용하는 프레임워크용으로 만든 클래스라면 선택의 여지없이 `Serializable` 인터페이스를 구현해야할 것이다. 참고로 이 경우에 `Serializable` 구현 클래스에 사용되는 컴포넌트 클래스들도 모두 `Serializable`을 구현해야 한다.
* 이 경우 `Serializable` 인터페이스 구현에 따른 이점과 비용을 생각해서 구현하는 것이 좋다. 참고로 `BigInteger`, `Integer`과 같은 값 객체나 컬렉션 객체는 `Serializable` 인터페이스를 구현했고 쓰레드 풀과 같이 동작을 표현하는 `Serializable` 인터페이스를 구현하지 않았다.

#### 상속용으로 설계된 클래스는 Serializable을 구현하면 안되며, 인터페이스도 Serializable을 확장해선 안 된다.

* 만약 `Serializable`을 확장, 구현하면 하위 클래스들이 고스란히 그 문제를 같이 가져가게 된다.
* 상속용으로 설계된 클래스 중 `Serializable` 인터페이스를 구현한 대표적인 사례로 `Throwable`과 `Component`가 있다. `Throwable`은 RMI를 통해 클라이언트로 예외를 보내기 위해, `Component`는 GUI를 전송하고 저장, 복원하기 위해 구현했다.

```java
public class Throwable implements Serializable {
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -3042686055658047285L;

    // ...
}
```

#### 직렬화와 확장이 모두 가능할 때

* 인스턴스 필드의 값 중에 불변식을 보장해야할 것이 있다면 반드시 하위 클래스에서 `finalize()` 메서드를 재정의하지 못하게 해야한다.
* `finalize()`를 재정의하면서 `final` 키워드를 붙여서 선언하는 것이다. 이렇게 하지 않으면 `finalizer` 공격에 취약해질 수 있다.
* 인스턴스 필드 중 기본값 `int = 0`, `Object = null`과 같이 설정되면 위배되는 불변식이 있다면 `readObjectNoData()` 메서드를 반드시 추가해야 한다.
```java
private void readObjectNoData() throws InvalidObjectException {
    throw new InvalidObjectException("스트림 데이터가 필요합니다");
}
```

#### 상위 클래스에서 직렬화를 지원하지 않을 때

* 상위 클래스에서 `Serializable`을 구현하지 않는다면 하나만 생각하면 된다. 상속용 클래스가 `Serializable` 인터페이스를 지원하지 않는 경우 하위 구현 클래스가 `Serializable`을 구현할 때 부담이 늘어난다.
* 이 때, 상위 클래스에서 인자가 없는 기본 생성자를 지원하면 하위 클래스에서 간단히 `Serializable` 인터페이스로 직렬화를 구현할 수 있다.  만약 지원하지 않는다면 하위 클래스에서 직렬화 프록시 패턴을 사용해야 한다.

#### 내부 클래스는 Serializable을 구현하면 안된다.

* 내부 클래스는 바깥 인스턴스 참조와 유효 범위 안의 지역변수들을 저장하기 위해 컴파일러가 자동으로 생성한 필드가 추가된다. 익명 클래스와 지역 클래스의 이름 짓는 규칙이 언어 명세에 없기 때문에 이 필드들이 클래스 정의에 어떻게 추가되는지도 정의되지 않았다.
* 따라서 내부 클래스 직렬화 형태는 불분명하기에 `Serializable` 인터페이스를 구현하면 안 된다. 정적 멤버 클래스는 `Serializable` 인터페이스를 구현하므로 Java 직렬화가 가능하다.
