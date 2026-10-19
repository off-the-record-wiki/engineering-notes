# Java ‐ OOP

#### final 키워드

* 변수, 메서드, 클래스에 사용되어 변경을 제한하는 역할
* final 변수 : 값을 변경할 수 없음
* final 메서드 : 오버라이딩 불가능
* final 클래스 : 상속 불가능

> * final 키워드는 필수는 아니지만, 코드의 안정성과 명확성을 높여주는 매우 유용한 도구이다.
> * 기본적으로 final을 붙이는 것을 강력히 권장한다. 클래스의 멤버 변수는 객체의 상태를 나타내므로, 불필요하게 변경될 여지를 남기지 않는 것이 좋다.
>   * 변경될 이유가 없는 변수 : 객체가 생성될 때 초기화된 후 그 값이 계속 유지되어야 하는 변수에는 반드시 final을 붙여준다.
>   * 의도적으로 변경해야 하는 변수: 객체의 상태가 변해야 하는 경우 (예: 사용자의 나이, 계좌 잔액 등)에만 final을 생략한다.
> * 메서드 내에서 선언되는 지역 변수의 경우, 의견이 다소 나뉘지만, 값을 재할당할 필요가 없다면 final을 붙이는 것이 좋은 습관이다.
> * 한 번만 값을 할당하는 변수: 메서드 내에서 선언과 동시에, 또는 한 번만 값을 할당하고 그 이후로는 읽기만 하는 변수에는 final을 붙여준다.
> * 반복문 등에서 값이 계속 바뀌는 변수: 반복문의 카운터 변수처럼 값이 계속 변경되어야 하는 경우에는 당연히 final을 사용할 수 없다.
> * 메서드에 전달되는 매개변수에 final을 붙이면, 메서드 내부에서 해당 매개변수의 값을 재할당하는 것을 방지할 수 있다.

#### 다형성(Polymorphism)

* 같은 인터페이스나 부모 클래스를 공유하는 객체가 서로 다른 동작을 수행할 수 있는 성질
* 메서드 호출 시, 실행될 구체적인 구현이 실행 시점에 결정된다.

```java
// 메서드 오버라이딩을 통한 다형성 - 부모 클래스 메서드를 자식 클래스에서 재정의해 다른 동작을 수행하도록 구현
class Animal {
    void makeSound() {
        System.out.println("Some sound");
    }
}

class Dog extends Animal {
    @Override
    void makeSound() {
        System.out.println("Bark!");
    }
}
Animal myDog = new Dog();
myDog.makeSound(); // 실행 결과: Bark! (Dog의 메서드 실행)
```

```java
// 인터페이스를 통한 다형성 - 인터페이스를 구현한 여러 클래스가 같은 메서드를 다르게 구현할 수 있다.
interface Shape {
    void draw();
}

class Circle implements Shape {
    public void draw() {
        System.out.println("Drawing Circle");
    }
}

class Square implements Shape {
    public void draw() {
        System.out.println("Drawing Square");
    }
}

Shape s1 = new Circle();
Shape s2 = new Square();
s1.draw();
s2.draw();
```

#### 인터페이스 vs 추상 클래스

|항목|인터페이스|추상 클래스|
|:----:|:----:|:----:|
|상속 방식|다중 구현 가능|단일 상속만 가능(댜이아몬드 문제)|
|구현 여부|모든 메서드가 추상|일부만 구현 가능|
|목적|기능 명세(API)|공통 기능 제공 및 강제 구현 유도|
|키워드|interface|abstract class|

```java
// 인터페이스
interface AnimalActions {
    void eat(); // public abstract 생략됨
}

class Dog extends Animal implements AnimalActions {
    @Override
    void makeSound() {
        System.out.println("Bark!");
    }
 
    @Override
    public void eat() {
        System.out.println("Eating...");
    }
}

// 추상 클래스
abstract class Animal {
    abstract void makeSound();
    void sleep() {
        System.out.println("Sleeping...");
    }
}
```
