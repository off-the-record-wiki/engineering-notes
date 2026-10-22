# (Effective Java Item 87) Java - 커스텀 직렬화 형태를 고려해보라

#### 객체의 물리적 표현과 논리적 내용이 같다면 기본 직렬화 형태라도 무방하다.

* **물리적 표현** : 객체가 실제로 저장하는 필드 구조
* **논리적 내용** : 그 객체가 표현하려는 데이터의 의미
* 이 둘이 일치하는 경우 기본 직렬화를 써도 된다는 뜻이다.
```java
class Person {
    private String name;
    private int age;
}
```
* 여기서 필드 자체는 곧 "사람"의 논리적 데이터가 되며 "구조 = 의미"가 된다.
```java
class LinkedList {
    private Node head;  // 내부 연결 노드들
}
```
* 물리적으로는 `Node` 체인이지만, 논리적으로는 순서 있는 값의 목록으로 기본 직렬화하면 내부 포인터 구조까지 그대로 직렬화가 된다.
* 이렇게 되면 구현이 API에 노출되고 나중에 내부 구조가 바뀌면 역직렬화 호환이 꺠지게 된다.

#### 합리적인 직렬화 형태

```java
public final class StringList implements Serializable {
    private transient int size = 0;// 직렬화 대상에서 제외한다.
    private transient Entry head = null;

    // 이번에는 직렬화 하지 않는다.
    private static class Entry {
        String data;
        Entry next;
        Entry previous;
    }

    // 문자열을 리스트에 추가한다.
    public final void add(String s) { ... }

    /**
     * StringList 인스턴스를 직렬화한다.
     */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(size);

        // 모든 원소를 순서대로 기록한다.
        for (Entry e = head; e != null; e = e.next) {
            s.writeObject(e.data);
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int numElements = stream.readInt();

        for (int i = 0; i < numElements; i++) {
            add((String) stream.readObject());
        }
    }
    // ... 생략
}
```
* **transient**
  * 클래스에서 `transient` 또는 `static` 키워드가 선언된 필드를 제외하고는 모두 직렬화 대상이 된다.
  * `transient` 키워드가 선언된 멤버 변수는 직렬화 대상에서 제외되었기 때문에 자바 객체로 변환되는 역직렬화 결과에서도 값을 확인할 수 있다.

#### writeObject 와 readObject 가 private 으로 기술되어 있다

* `private`으로 선언되었다는 것은 이 클래스를 상속한 서브 클래스에서 메서드를 재정의를 하지 못하게 한다는 것이다.
* 또한 다른 객체는 호출할 수 없기 때문에 클래스 무결성이 유지되며 슈퍼 클래스와 서브 클래스는 독립적으로 직렬화 방식을 유지하며 확장될 수 있다. 직렬화 과정에서는 "리플렉션(Reflection)"을 통해 메서드를 호출하기 때문에 접근 지정자는 문제가 되지 않는다.

```java
public class SomeClass implements Serializable {
    private String fld1;
    private int fld2;
    private transient String fld3; 
    private void readObject(java.io.ObjectInputStream stream)
         throws IOException, ClassNotFoundException {
         stream.defaultReadObject(); //fills fld1 and fld2;
         fld3 = Configuration.getFooConfigValue();
    }
}
```

#### SerialVersionUID

```java
private static final long serialVersionUID = 0204L;
```
* 직렬화를 할 때, `serialVersionUID`가 없으면 내부에서 자동으로 유니크한 번호를 생성해 관리하게 된다.
* `serialVersionUID`는 직렬화/역직렬화 과정에서 값이 서로 맞는지 확인한 후에 처리를 하기 때문에 이 값이 맞지 않다면 `InvalidClassException` 예외가 발생한다.
* 자바의 직렬화 스펙 정의를 살펴보면 `serialVersionUID` 값은 필수가 아니며 선언되어 있지 않으면 클래스의 기본 해시값을 사용한다.

> The getSerialVersionUID method returns the serialVersionUID of this class. Refer to [Section 4.6, "Stream Unique Identifiers"](https://docs.oracle.com/javase/10/docs/specs/serialization/class.html#stream-unique-identifiers). If not specified by the class, the value returned is a hash computed from the class's name, interfaces, methods, and fields using the Secure Hash Algorithm (SHA) as defined by the National Institute of Standards.

* 따라서 직접 `serialVersionUID`를 명시하지 않더라도 내부에서 자동으로 값이 추가되며 이 값들은 클래스의 이름, 생성자 등과 같이 클래스 구조를 이용해서 생성한다.
* 직렬화 가능한 클래스를 선언할 때, `serialVersionUID` 값을 생략해도 내부적으로 정보가 생성됨을 유추할 수 있게 된다.
