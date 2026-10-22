# (Clean Code 6) Clean Code - 객체와 자료구조

#### 자료 추상화

```java
// ❌ Bad(구현을 외부로 노출한다)
public class Point {
    public double x;
    public double y;
}
```
```java
// ⭕ Good(구현을 완전히 숨긴다)
public interface Point {
    double getX();
    double getY();
    void setCartesian(double x, double y);
    double getR();
    double getTheta();
    void setPolar(double r, double theta);
}
```

* 변수를 `private`으로 선언하더라도 각 값마다 Getter/Setter를 제공한다면 구현을 외부에 노출하는 셈이다.
* 변수 사이에 함수라는 계층을 넣는다고 구현이 저절로 감춰지진 않는다. 구현을 감추려면 '추상화'가 필요하다.
* 추상 인터페이스를 제공해 사용자가 구현을 모른 채 자료의 핵심을 조작할 수 있어야 진정한 의미의 클래스이다.

```java
// ❌ Bad(자료를 세세하게 공개한다)
public interface Vehicle {
    double getFuelTankCapacityInGallons();
    double getGallonsOfGasoline();
}
```
```java
// ⭕ Good(추상적인 개념으로 표현한다)
public interface Vehicle {
    double getPercentFuelRemaining();
}
```

* 인터페이스 조회/설정만으로 추상화가 이뤄지지 않는다.
* 객체가 포함하는 자료를 표현할 가장 좋은 방법을 고민해야 한다. 아무 생각없이 조회/설정 함수만으로 추상화가 이뤄지지 않는다.

#### 자료/객체 비대칭

* 객체는 추상화 뒤로 자료를 숨긴 채 자료를 다루는 함수만 공개한다.
```java
// ❌ Bad(절차적 도형 에제)
public class Geometry {
    private final double PI = 3.141592653589793;

    public double area(Object shape) {
        if (shape instanceof Square) {
            Square s = (Square) shape;
            return s.side * s.side;
        }

        if (shape instanceof Circle) {
            Circle c = (Circle) shape;
            return PI * c.radius * c.radius;
        }

        throw new IllegalArgumentException("알 수 없는 도형입니다.");
    }
}

public class Circle {
    public Point center;
    public double radius;
}

public class Point {
    public double x;
    public double y;
}

public class Square {
    public Point topLeft;
    public double side;
}
```
```java
// ⭕ Good
public class Rectangle implements Shape{

    private Point topLeft;
    private double height;
    private double width;

    @Override
    public double getArea() {
        return height * width;
    }
}
```

#### 디미터 법칙

* 디미터 법칙은 잘 알려진 휴리스틱으로 모듈은 자신이 조작하는 객체의 속사정을 몰라야 한다는 법칙이다.
* 객체는 자료를 숨기고 함수를 공개한다. 즉, 객체는 조회 함수로 내부 구조를 공개하면 안 된다.

```java
// ❌ Bad(get 조회 함수로 체이닝 방식으로 사용하는 방식을 자제)
final String outputDir = ctxt.getOptions().getScratchDir().getAbsolutePath();
```

* 위와 같은 코드를 기차 충돌이라고 부른다. 일반적으로 조잡하다 여기는 방식이므로 피하는 편이 좋다.
* 위 코드는 다음과 같이 나누는 편이 좋다.

```java
// ❌ Good
Options opts = ctxt.getOptions();
File scratchDir = opts.getScratchDir();
final String outputDir = scratchDir.getAbsolutePath();
```

#### 자료 전달 객체

* 자료 구조체의 전형적인 형태는 공개 변수만 있고 함수가 없는 클래스이다.
* 이런 자료 구조체를 때로는 자료 전달 객체(Data Transfer Object, DTO)라 한다.
* DTO는 데이터베이스와 통신하거나 소켓에서 받은 메시지의 구문을 분석할 때 유용하다.
* 흔히 DTO는 데이터베이스에 저장된 가공되지 않은 정보를 애플리케이션 코드에서 사용할 객체로 변환하는 일련의 단계에서 가장 처음으로 사용하는 구조체이다.

```java
public class Address {

    private String street;
    private String streetExtra;
    private String city;
    private String state;
    private String zip;

    public Address(String street, String streetExtra, String city, String state, String zip) {
        this.street = street;
        this.streetExtra = streetExtra;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public String getStreet() {
        return street;
    }

    public String getStreetExtra() {
        return streetExtra;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }
}
```

#### 활성 레코드

* 활성 레코드는 DTO의 특수한 형태이다.
* 공개 변수가 있거나 비공개 변수에 조회/설정 함수가 있는 자료 구조지만, 대개 `save`나 `find`와 같은 탐색 함수도 제공한다.
* 활성 레코드는 데이터베이스 테이블이나 다른 소스에서 자료를 직접 반환한 결과다.
