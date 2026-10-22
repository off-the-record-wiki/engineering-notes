# (Effective Java Item 3) Java - private 생성자나 열거 타입으로 싱글턴임을 보증하라

* 싱글턴(Singleton)이란 인스턴스를 오직 하나만 생성할 수 있는 클래스를 말한다.
* 클래스를 싱글턴으로 만들면 사용하는 클라이언트를 테스트하기가 어려워질 수 있다.

#### public static final 필드 방식의 싱글톤 1

```java
public class Singleton {

    public static final Signleton INSTANCE = new Singleton(); // static으로 선언해서 클래스 영역에 하나만 존재함을 보장
 
    private Singleton() { // 생성자를 private으로 막아 외부에서 생성할 수 없게끔

    }

    // ...
}
```
* `private` 생성자는 `public static final` 필드인 `Singleton.INSTANCE`를 초기화할 때 딱 한 번만 호출됨을 보장한다.
* `public`이나 `protected` 생성자가 없으므로 Signleton 클래스가 초기화될 때 만들어진 인스턴스가 전체 시스템에서 딱 하나임이 보장된다.
* 예외는 단 한 가지, 권한이 있는 클라이언트는 리플렉션 API인 `AccessibleObject.setAccessible()`을 사용해 `private` 생성자를 호출할 수 있다. 이런 공격을 방어하려면 생성자를 수정해 두 번째 객체가 생성되려고 할 때 예외를 던지게 하면 된다.
* `public` 필드 방식의 큰 장점은 해당 클래스가 싱글턴임이 API에 명백히 드러난다는 것이다. `public static` 필드가 `final`이니 절대로 다른 객체를 참조할 수 없다.
* 덤으로 간결함이라는 장점도 있다.

#### 정적 팩토리 방식의 싱글톤 2

```java
public class Singleton {

    private static final Signleton INSTANCE = new Singleton();  // static으로 선언해서 클래스 영역에 인스턴스를 하나만 생성하되 private 접근 제어자로 제어

    private Singleton() {  // 동일하게 생성자를 private으로 만들어 외부에서 생성자를 호출할 수 없도록
    
    }

    public static Singleton getInstance() { // static으로 선언해 전체 시스템에서 하나임을 보장할 수 있으므로 항상 같은 객체를 반환하게 된다.
        return INSTANCE;
    }
}
```

* 여기서 `getInstance()`는 항상 같은 객체 참조를 반환하므로 제 2의 인스턴스가 결코 만들어지지 않는다.
* API를 바꾸지 않고도 싱글턴이 아니게 변경할 수 있다는 점이 장점이다. 유일한 인스턴스를 반환하던 팩토리 메서드가 호출하는 쓰레드별로 다른 인스턴스를 넘겨주게 할 수 있다.
* 추가로 원한다면 정적 팩토리 메서드를 제네릭 싱글턴 팩토리로 만들 수 있다.
* 추가로 정적 팩토리 메서드 참조를 공급자로 사용할 수 있다. 이런 장점들을 굳이 필요로 하지 않는다면 `public` 필드 방식이 좋다.

#### 열거 타입 방식의 싱글톤 3

```java
public enum Singleton {

    INSTANCE;

}
```

* 대부분 상황에서는 원소가 하나뿐인 열거 타입이 싱글턴을 만드는 가장 좋은 방법이다.
* 단, 만들려는 싱글턴이 ENUM 외의 클래스를 상속해야 한다면 이 방법은 불가능하다. 그러나 열거 타입이 다른 인터페이스를 구현하도록 선언할 수는 있다.
