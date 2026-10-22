# (Effective Java Item 39) Java ‐ 명명 패턴보다 애너테이션을 사용하라[Effective Java Item 39]

#### 명명 패턴의 문제점

* 오타 - 이름으로 구분하기 때문에 오타가 발생하면 무시된다.
* 올바른 프로그램 요소에서만 사용되리라 보증할 수 없다 - JUnit3에서는 테스트 메서드 이름을 test로 시작해야할뿐, 클래스 이름은 이를 따를 필요가 없다. 하지만 사용자는 테스트 클래스에 이 규칙을 적용하고 테스트가 되길 기대할 수 있다. 이 때, 사용자는 어떠한 경고 메시지고 받지 못할 뿐더라 당연히 의도한 테스트를 수행할 수 없다.
* 프로그램 요소를 매개변수로 전달할 방법이 없다 - 특정 예외를 던질 때 성공하는 테스트를 작성한다고 가정하면 기대하는 예외 타입을 테스트에 전달해야 한다.

#### 어노테이션 타입 선언 작성

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
```

* `@Retention` : 어노테이션의 스코프를 결정한다.
* `@Target` : 어노테이션이 적용될 대상을 결정한다.

#### 어노테이션을 사용한 코드 작성

```java
public class Sample {
    @Test public static void m1(){}

    public static void m2(){}

    @Test public static void m3(){
        throw new RuntimeException("fail");
    }
}
```

#### 어노테이션을 처리하는 코드 작성

* 어노테이션을 사용하는 것 자체로는 해당 클래스에 직접적인 영향을 미치지 않는다. 마커 어노테이션은 표시일 뿐이다. 이 어노테이션은 이를 처리하는 프로그램에 추가적인 정보를 제공하기 위해 사용된다.

```java
public class RunTests {
    public static void main(String[] args) throws Exception{
        int tests = 0;
        int passed = 0;
        Class<?> testClass = Class.forName(args[0]);
        for (Method m : testClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {	//Test 애너테이션이 선언된 메서드만을 호출한다.
                tests++;
                try {
                    m.invoke(null);
                    passed++;
                } catch (InvocationTargetException wrappedExc) {
                    Throwable exc = wrappedExc.getCause();
                    System.out.println(m + " 실패 : " + exc);
                } catch (Exception e) {
                    System.out.println("잘못 사용한 테스트 @Test : " + m);
                }
            }
        }
        System.out.printf("suc :: %d, fail : %d%n", passed, tests - passed);
    }
}
```

#### 매개변수를 받는 어노테이션 선언

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
    Class<? extends Throwable> value();		//매개변수 선언
}
```

#### 매개변수가 있는 어노테이션을 사용한 코드 작성

```java
public class Sample2 {
    @ExceptionTest(ArithmeticException.class)
    public static void m1() {
        int i = 1 / 0;
    }

    @ExceptionTest(ArithmeticException.class)
    public static void m2() {
        int[] arr = new int[0];
        arr[1] = 1;     // outOfIndex 예외 발생
    }

    @ExceptionTest(ArithmeticException.class)
    public static void m3() {}
}
```

#### 매개변수를 받는 어노테이션을 처리하는 코드 작성

```java
public class RunExceptionTests {
    public static void main(String[] args) throws Exception{
        int tests = 0;
        int passed = 0;
        Class<?> testClass = Class.forName("chapter6.Sample2");
        for (Method m : testClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(ExceptionTest.class)) {	//ExceptionTest 애너테이션을 사용한 메서드 선별
                tests++;
                try {
                    m.invoke(null);
                } catch (InvocationTargetException wrappedExc) {
                    Throwable exc = wrappedExc.getCause();
                    Class<? extends Throwable> excType = m.getAnnotation(ExceptionTest.class).value();	// 애너테이션의 매개변수 타입 확인
                    if (excType.isInstance(exc)) {	// 애너테이션의 매개변수 타입과 같을 경우 통과
                        passed++;
                    }
                } catch (Exception e) {
                    System.out.println("잘못 사용한 테스트 @Test : " + m);
                }
            }
        }
        System.out.printf("suc :: %d, fail : %d%n", passed, tests - passed);
    }

}
```
