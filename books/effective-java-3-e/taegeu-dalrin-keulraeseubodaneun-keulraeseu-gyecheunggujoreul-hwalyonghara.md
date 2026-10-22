# (Effective Java Item 23) Java ‐ 태그 달린 클래스보다는 클래스 계층구조를 활용하라

#### 태그 달린 클래스보다는 클래스 계층구조를 활용하라

> 태그 달린 클래스란?
> * 태그 달린 클래스(tagged class)는 하나의 클래스 안에 여러 개의 객체 유형을 담고, 어떤 유형인지 구분하기 위해 '태그' 역할을 하는 특별한 필드를 두는 방식이다.<br>
```java
// Bad
public class Figure {
    enum Shape { RECTANGLE, CIRCLE }; // 태그 필드
    final Shape shape;

    // 사각형일 때 사용되는 필드
    double length;
    double width;

    // 원일 때 사용되는 필드
    double radius;

    // 원을 위한 생성자
    public Figure(double radius) {
        this.shape = Shape.CIRCLE;
        this.radius = radius;
    }

    // 사각형을 위한 생성자
    public Figure(double length, double width) {
        this.shape = Shape.RECTANGLE;
        this.length = length;
        this.width = width;
    }

    // 면적을 계산하는 메서드
    double area() {
        switch (shape) {
            case RECTANGLE:
                return length * width;
            case CIRCLE:
                return Math.PI * (radius * radius);
            default:
                throw new AssertionError(shape);
        }
    }
}
```
> ⚠️태그 달린 클래스의 문제점은 여러가지가 있다.
> 
> * 복잡한 코드(가독성 저하) : 여러 종류의 객체를 하나의 클래스에서 처리하므로 코드가 복잡하고 가독성이 떨어진다. 위와 같은 예시 코드에서 CIRCLE 인스턴스임에도 불구하고 length와 width 필드를 메모리에 항상 들고 있어야 한다.
> * 유지보수 어려움 : 새로운 유형을 추가할 때마다 클래스와 switch문 같은 조건문을 모두 수정해야한다.
> * 메모리 낭비 : 특정 유형에서만 사용되는 필드들이 다른 유형의 객체에도 모두 존재하므로 메모리가 낭비된다.
> * 안전성 문제(타입 안정성 부족) : 프로그래머의 실수로 잘못된 태그 값을 사용하거나, 올바르지 않은 필드에 접근할 위험이 있다.

#### 클래스 계층구조로 리팩토링

```java
// Good
public abstract class Figure { // 계층구조의 루트가 될 추상 클래스를 먼저 정의한다.
    abstract double area();    // 태그 값에 따라 동작이 달라지는 메서드들을 추상 메서드로 선언한다.
}

class Circle extends Figure {  // Figure 추상 클래스를 확장한 구현체 클래스를 정의한다.
    final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    @Override
    double area() {            // 추상 메서드를 각자의 의미에 맞게 구현한다.
        return Math.PI * (radius * radius);
    }
}

class Rectangle extends Figure {
    final double length;
    final double width;

    Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    @Override
    double area() {
        return length * width;
    }
}
```
* 이런 클래스 계층 구조로 리팩토링을 하면서 실수로 빼먹은 case문으로 인해 런타임 오류가 발생할 일도 없다.
* 루트 클래스 코드를 건드리지 않고도 독립적으로 계층을 확장하고 함께 사용할 수 있게 된다.
* 타입이 의미별로 따로 존재하니 변수 의미를 명시하거나 제한할 수 있고 또 특정 의미만 매개변수로 받을 수 있다.
