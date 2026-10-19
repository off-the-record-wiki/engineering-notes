# Spring ‐ @Aspect AOP

#### @Aspect 어노테이션 기반 프록시 적용

* 스프링 애플리케이션에 프록시를 적용하려면 포인트컷과 어드바이스로 구성된 어드바이저를 만들어서 스프링 빈으로 등록하면 된다.
* 스프링에서는 `@Aspect` 어노테이션으로 매우 편리하게 포인트컷과 어드바이스로 구성된 어드바이저 생성 기능을 지원한다.
* `@Aspect` 어노테이션은 관점 지향 프로그래밍을 가능하게 하는 AspectJ 프로젝트에서 제공하는 어노테이션으로 스프링은 이것을 사용해서 프록시를 통한 AOP를 가능하게 한다.

#### @Aspect 프록시

* 자동 프록시 생성기는 `@Aspect`를 찾아서 이것을 Advisor로 만들어준다.
  * `@Aspect`를 보고 어드바이저로 변환해서 저장한다.
  * 어드바이저를 기반으로 프록시를 생성한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3fe3cebd-7a36-4c0a-88e6-9a6141b95ce0.png)

#### @Aspect를 어드바이저로 변환해서 저장하는 과정

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ba4e7581-c894-4f78-ae9b-09278f22a135.png)

1. 스프링 애플리케이션 실행 시점에 자동 프록시 생성기를 호출한다.
2. 모든 `@Aspect` 빈을 조회한다.
3. `@Aspect` 어드바이저 빌더를 통해 @Aspect 정보를 기반으로 어드바이저를 생성한다.
4. `@Aspect` 기반 어드바이저를 저장한다.
 
