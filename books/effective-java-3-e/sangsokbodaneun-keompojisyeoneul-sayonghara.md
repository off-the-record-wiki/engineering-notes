# (Effective Java Item 18) Java ‐ 상속보다는 컴포지션을 사용하라

#### 상속보다는 컴포지션을 사용하라

* 상속은 캡슐화를 깨뜨린다.
* 상위 클래스가 어떻게 구현되느냐에 따라 하위 클래스 동작에 이상이 생길 수 있다.
* 상위 클래스 설계자가 확장을 충분히 고려하고 문서화도 제대로 해두지 않으면 하위 클래스는 상위 클래스 변화에 맞춰 수정이 되어야만 한다.
* 또한 상위 클래스에 새로운 메서드가 추가되면 해당 클래스를 상속받는 모든 하위 클래스가 메서드를 재정의해 불필요하게 상속받아 구현해야하는 문제가 발생한다.

```java
// Bad
public class InstrumentedHashSet<E> extends HashSet<E> {
    
    private int addCount = 0;

    public InstrumentedHashSet(int initCap, float loadFactor) {
        super(initCap, loadFactor);
    }

    @Override 
    public boolean add(E e) {
	addCount++;
	return super.add(e);
    }

    @Override 
    public boolean addAll(Collection<? extends E> c) {
	addCount += c.size();
	return super.addAll(c);
    }

    public int getAddCount() {
	return addCount;
    }
}
```

#### 컴포지션(Composition)

* 상속은 캡슐화를 깨뜨리고 확장하기 어려운 구조를 가질 수 있게 된다.
* 컴포지션이란 기존 클래스를 확장하는 대신, 새로운 클래스를 만들고 private 필드로 기존 클래스의 인스턴스를 참조하게 하는 방법이다.

```java
public class Engine {
    public void start() {
        System.out.println("엔진이 시동을 겁니다.");
    }

    public void stop() {
        System.out.println("엔진이 멈춥니다.");
    }
}
```
```java
public class Wheel {
    private String brand;

    public Wheel(String brand) {
        this.brand = brand;
    }

    public void roll() {
        System.out.println(brand + " 바퀴가 굴러갑니다.");
    }
}
```
```java
// Good
public class Car {
    private final Engine engine;    // 클래스 인스턴스를 멤버변수로
    private final Wheel[] wheels;   // 클래스 인스턴스를 멤버변수로

    public Car() {
        this.engine = new Engine();
        this.wheels = new Wheel[]{
            new Wheel("미쉐린"),
            new Wheel("미쉐린"),
            new Wheel("미쉐린"),
            new Wheel("미쉐린")
        };
    }

    public void drive() {
        System.out.println("--- 자동차 운행 시작 ---");
        engine.start();
        for (Wheel wheel : wheels) {
            wheel.roll();
        }
        System.out.println("자동차를 운전합니다.");
    }

    public void stop() {
        System.out.println("--- 자동차 운행 종료 ---");
        engine.stop();
        System.out.println("자동차를 멈춥니다.");
    }
}
```
