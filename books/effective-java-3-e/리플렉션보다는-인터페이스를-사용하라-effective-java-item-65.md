# (Effective Java Item 65) Java ‐ 리플렉션보다는 인터페이스를 사용하라

* 리플렉션(java.lang.reflect)을 이용하면 프로그램에서 임의의 클래스에 접근할 수 있다.
* Class 객체가 주어지면 그 클래스의 생성자, 메서드, 필드에 해당하는 Constructor, Method, Field 인스턴스를 가져올 수 있고, 이어서 이 인스턴스들로 그 클래스 멤버 이름, 필드 타입, 메서드 시그니처 등을 가져올 수 있다.
* 리플렉션을 이용하면 컴파일 당시에 존재하지 않던 클래스도 이용할 수 있지만 단점이 존재한다.

#### 컴파일 타임에서 이루어지는 타입 검사가 주는 이점을 하나도 누릴 수 없다.
* 예외 검사도 마찬가지다.
* 프로그램이 리플렉션 기능을 써서 존재하지 않는 혹은 접근할 수 없는 메서드를 호출하려 시도하면 런타임 오류가 발생한다.

#### 리플렉션을 이용하면 코드가 지저분하고 장황해진다.
* 지루한 일이기도 하지만 읽기도 어렵다.

#### 성능이 떨어진다.
* 리플렉션을 통한 메서드 호출은 일반 메서드 호출보다 훨씬 느리다.

<br>

* 리플렉션은 아주 제한된 형태로만 사용해야 그 단점을 피하고 이점만 취할 수 있다. 
* 컴파일 타임에 이용할 수 없는 클래스를 사용해야만 하는 프로그램은 비록 컴파일 타임이라도 적절한 인터페이스나 상위 클래스를 이용할 수 있을 것이다.
* 리플렉션은 인스턴스 생성에만 쓰고 이렇게 만든 인스턴스는 인터페이스나 상위 클래스로 참조해 사용하자.

```java
public static void main(String[] args) {
    Class<? extends Set<String>> cl = null;
    // 클래스 이름을 클래스 객체로 변환
    try {
        cl = (Class<? extends Set<String>>) Class.forName(args[0]);
    } catch (ClassNotFoundException e) {
        fatalError("클래스를 찾을 수 없습니다.");
    }

    // 생성자를 얻어오기
    Constructor<? extends Set<String>> cons = null;
    try{
        cons = cl.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
        fatalError("매개변수 없는 생성자를 찾을 수 없습니다.");
    }

    // 위에서 만든 클래스를 통해 집합 인스턴스 생성
    Set<String> s = null;
    try{
        s = cons.newInstance();
    } catch (IllegalAccessException e) {
        fatalError("생성자에 접근할 수 없습니다.");
    } catch (InstantiationException e) {
        fatalError("클래스를 인스터화 할 수 없습니다.");
    } catch (InvocationTargetException e) {
        fatalError("생성자가 예외를 던졌습니다." + e.getCause());
    } catch (ClassCastException e){
        fatalError("Set을 구현하지 않은 클래스 입니다.");
    }

    // 생성한 인스턴스 사용
    s.addAll(Arrays.asList(args).subList(1, args.length));
    System.out.println(s);
}

private static void fatalError(String msg){
    System.out.println(msg);
    System.exit(1);
}
```

#### 테스트 코드 작성에서 리플렉션을 사용해도 되는가?

* 결론부터 말하자면 테스트 코드 작성 시 리플렉션을 사용하는 것은 합당한 선택으로 간주된다.
* 프로덕션 코드에서는 복잡성 증가 및 캡슐화 위반 가능성 때문에 지양될 수 있지만, 테스트 환경에서는 명확한 이점을 제공한다.

#### 비공개 멤버(private member) 접근
* 리플렉션은 클래스의 private 필드나 메서드에 접근할 수 있게 해준다.
* 이는 외부에서 직접 호출할 수 없는 내부 로직이나 상태를 테스트할 때 필수적이다.

#### 캡슐화 유지
* 프로덕션 코드에 테스트를 위한 공개 접근자를 추가하는 대신 리플렉션을 사용해 테스트 코드에서만 비공개 멤버에 접근함으로써 프로덕션 코드의 캡슐화는 그대로 유지할 수 있다.

#### 테스트 용이성 향상
* 상속 구조가 복잡하거나 서드파티 라이브러리를 테스트할 때, 리플렉션은 테스트 더블(Test Doubles)나 목 객체(Mock Objects)를 구성하는 과정을 단순화하거나 가능하게 만든다.
* 그러나, 리플렉션을 과도하게 사용하면 테스트 코드 자체 가독성이 떨어지고 이해하기 어려워질 수 있다.
* 비공개 멤버 이름이나 시그니처가 변경되면 리플렉션을 사용하는 테스트 코드가 컴파일 오류 없이 런타임 오류를 발생시킬 수 있다.
