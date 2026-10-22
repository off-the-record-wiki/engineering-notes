# Spring ‐ Spring AOP

#### AOP(Aspect Oriented Programming, 관점 지향 프로그래밍) 개념

* 애플리케이션 로직은 크게 핵심 기능과 부가 기능으로 나뉜다.
* AOP의 A(Aspect)는 부가 기능과 해당 부가 기능을 어디에 적용할지를 정의한 것이다.
* [AspectJ 프레임워크](https://eclipse.dev/aspectj/)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/39e4bc4d-6e05-4741-a007-c0ac0959a49e.png)

#### AOP 적용 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3ddadafa-62e7-432e-8116-de6b56bcbc7e.png)

* 런타임 시점 : 실제 대상 코드는 그대로 유지된다. 대신에 프록시를 통해 부가 기능이 적용되며 스프링 컨테이너에 실제 클래스가 아닌 프록시가 빈으로 등록된다. 스프링 AOP는 이 방식을 사용한다.
* 프록시 방식을 사용할 경우 스프링 AOP는 메서드 실행 지점에만 적용할 수 있다.
