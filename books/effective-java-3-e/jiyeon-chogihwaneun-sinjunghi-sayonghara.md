# (Effective Java Item 83) Java - 지연 초기화는 신중히 사용하라

#### 지연 초기화(lazy initialization)

* 필드의 초기화 시점을 그 값이 처음 필요할 때까지 늦추는 기법이다.
* 주로 최적화 용도로 사용되며 대표적으로 Spring의 Proxy가 있다.
* 클래스와 인스턴스 초기화할 때 발생하는 위험한 순환문제를 해결하는 효과도 있다.

#### 지연 초기화는 양날의 검이다.

* 지연 초기화를 사용한다면 인스턴스 생성 시의 초기화 비용은 줄어들지만 대신 필드에 접근하는 비용이 커진다.
* 지연 초기화 필드 중 초기화가 이뤄지는 비율에 따라, 실제 초기화에 드는 비용에 따라, 초기화된 각 필드를 얼마나 빈번히 호출하느냐에 따라 지연 초기화가 실제로 성능을 느려지게 할 수도 있다.
* 해당 클래스의 인스턴스 중 그 필드를 사용하는 인스턴스 비율이 낮고 그 필드를 초기화하는 비용이 크다면 지연 초기화를 사용하도록 한다.

```java
public class FieldHolder {
    private volatile ExpensiveField field;

    public ExpensiveField getField() {
        ExpensiveField result = field;
        if (result == null) { // 첫 번째 검사 (락 없이)
            synchronized(this) {
                result = field;
                if (result == null) { // 두 번째 검사 (락 쥐고)
                    field = result = new ExpensiveField(); // 그제야 초기화
                }
            }
        }
        return result;
    }
}
```
* 해당 필드를 읽을 때마다 `volatile` 키워드의 변수를 읽는 쓰레드 메모리 배리어 동기화 비용을 계속 지불해야 한다.

#### 초기화 방법 1 - 일반적 초기화

```java
public class TestClass {
    private final Member member = createMember();

    private Member createMember() {
        return new Member("KJJ");
    }
}
```

* 특징 : 클래스가 인스턴스화되거나 필드에 접근하기 전, 선언과 동시에 객체를 생성해버리는 가장 정석적인 방법이다.
* 장점 : `Thread-Safe`, 불변성 확보, 성능 최적(아무런 부가 연산 없이 속도가 가장 빨라서), Fail-Fast
* 단점 : 무거운 객체인데 한 번 호출될까 말까한 기능이라면 쓰지도 않을 메모리를 미리 차지하게 되는 비효율적인 문제 발생

#### 초기화 방법 2 - 인스턴스 필드의 지연초기화

```java
public class TestClass {
    private Member member;

    public synchronized  Member getMember(){
        if(member==null){
            member = createMember();
        }
        return member;
    }
    private Member createMember() {
        return new Member("KJJ");
    }
}
```
* 지연 초기화가 초기화 순환성을 깨뜨릴 것 같다면 `synchronized`를 단 접근자를 사용한다.
* 특징 : 메서드 전체에 `synchronized` 키워드를 걸어 최초 호출 시점에 필드를 초기화하는 전통적인 지연 초기화 방식이다.
* 장점 : 코드가 직관적이고 이해하기 쉽다.
* 단점 : 심각한 성능 병목(값을 단순히 읽으려고 할 때마다 모든 쓰레드가 락을 확보하기 위해 줄을 서야 한다.)

#### 초기화 방법 3 - 정적 필드용 지연 초기화 홀더 클래스 관용구

```java
public class TestClass {

    static final private Member member = createMember();

    private static Member createMember() {
        System.out.println("init");
        return new Member("KJJ");
    }

    public static Member getMember(){
        return TestClass.member;
    }
}
```
* 특징 : `private static class`를 두어 JVM 클래스 로더 메커니즘을 이용한 지연 초기화 방식이다.
* 장점 : `synchronized`나 `volatile` 키워드를 사용하지 않아도 된다. 지연 초기화의 이점을 누리면서도 메모리 절약도 가능하다.
* 단점 : 오직 `static` 레벨 필드에만 적용할 수 있다. 일반 인스턴스 필드에는 사용할 수 없다.

#### 초기화 방법 4 - 인스턴스 필드 지연 초기화용 이중검사 관용구

```java
public class TestClass {

    private volatile Member member;

    private Member createMember() {
        return new Member("KJJ");
    }

    public Member getMember() {
        Member result = member;
        if(result!=null){
            return result;
        }
        synchronized (this){
            if(member == null){
                member = createMember();
            }
            return member;
        }
    }
}
```
* 특징 : 인스턴스 필드 지연 초기화시 성능 저하를 유발하는 2번 방식의 문제를 해결하기 위해 락이 걸리지 않는 조건문 분기를 앞에 먼저 두는 방식
* 장점 : 이미 객체가 생성된 이후라면 `synchronized` 블록을 호출하지 않으므로 읽기 성능이 비약적으로 향상된다.
* 단점 : 코드가 한 눈에 들어오지 않고 실수하기가 쉽다.
