# Java ‐ Wrapper Class

#### 래퍼 클래스 - 기본형의 한계

* 기본형의 한계
  * 객체가 아니다.
  * null 값을 가질 수 없다.
* 자바는 기본형에 대응하는 래퍼 클래스를 가지고 있다.
* 자바가 제공하는 기본 래퍼 클래스는 불변이며, 이 래퍼 클래스는 객체이기 때문에 `equals()`로 비교해야 한다.

```
- byte -> Byte
- short -> Short
- int -> Integer
- long -> Long
- float -> Float
- double -> Double
- char -> Character
- boolean -> Boolean
```

#### 래퍼 클래스 - 자바 래퍼 클래스

* 기본형을 래퍼 클래스로 변경하는 것을 박싱(Boxing)이라고 한다. Boxing은 `valueOf`를 통해서 이루어진다.
* 래퍼 클래스에 들어있는 값을 기본형으로 꺼내는 것을 언박싱(Unboxing)이라고 한다. Unb# oxing은 `intValue`, `longValue` 등을 통해서 이루어진다.

#### 래퍼 클래스 - 오토 박싱

* 자바에서 int를 Integer, Integer를 int로 변환하는 부분에 대한 예시 코드

```java
public class AutoBoxing {

	public static void main(String[] args) {

		int value = 7;
		Integer i1 = Integer.valueOf(value);	// 박싱(boxing)

		int i2 = i1.intValue(); // 언박싱(unboxing)

		System.out.println(i1);
		System.out.println(i2);
	}
}
```

* 자바는 자바 1.5부터 오토 박싱, 오토 언박싱을 지원한다. 

#### Class 클래스

* 자바에서 Class 클래스는 클래스의 정보(메타 데이터)를 다루는데 사용된다.
* Class 클래스를 통해 개발자는 실행 중인 자바 애플리케이션 내에서 필요한 클래스의 속성과 메서드에 대한 정보를 조회하고 조작할 수 있다.
* Class 클래스의 주요 기능
  * 타입 정보 얻기 : 클래스의 이름, 슈퍼 클래스, 인터페이스, 접근 제한자 등과 같은 정보를 조회할 수 있다.
  * 리플렉션 : 클래스에 정의된 메서드, 필드, 생성자 등을 조회하고 이들을 통해 객체 인스턴스를 생성하거나 메서드를 호출하는 등의 작업을 할 수 있다.
  * 동적 로딩과 생성 : `Class.forName()` 메서드를 사용해 클래스를 동적으로 로드하고, `newInstance()` 메서드를 통해 새로운 인스턴스를 생성할 수 있다.
  * 어노테이션 처리 : 클래스에 적용된 어노테이션을 조회하고 처리하는 기능을 제공한다.

```java
public class ClassMetaMain {
	public static void main(String[] args) throws ClassNotFoundException {

		Class<String> clazz = String.class;				// 클래스 조회
		Class<? extends String> clazz1 = new String().getClass();       // 인스턴스 조회
		Class<?> clazz2 = Class.forName("java.lang.String");            // 문자열 조회

		// 모든 메서드 출력
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			System.out.println("method = " + method);
		}

		String superClass = clazz.getSuperclass().getName();
		System.out.println("상위 클래스 정보 : " + superClass);

		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> anInterface : interfaces) {
			System.out.println("인터페이스 정보 : " + anInterface);
		}
	}
}
```

```java
public class ClassCreateMain {
	public static void main(String[] args) throws Exception {

		Class<Hello> helloClass = Hello.class;
		// Class<?> helloClass1 = Class.forName("java.clazz.Hello");

		// 생성자를 선택하고 선택된 생성자를 기반으로 인스턴스를 생성한다.
		Hello hello = helloClass.getDeclaredConstructor().newInstance();
		String result = hello.hello();
		System.out.println("result = " + result);
	}
}
```

#### System 클래스

* `System` 클래스는 시스템과 관련된 기능을 제공한다.

```java
public class SystemMain {
	public static void main(String[] args) {

		// 현재 시간(밀리초)
		long currentTimeMillis = System.currentTimeMillis();
		System.out.println("currentTimeMillis = " + currentTimeMillis);

		// 현재 시간(나노초)
		long currentTimeNano = System.nanoTime();
		System.out.println("currentTimeNano = " + currentTimeNano);

		// 환경 변수
		Map<String, String> getenv = System.getenv();
		for (String env : getenv.keySet()) {
			System.out.println("env = " + env);
		}

		// 시스템 속성
		Properties properties = System.getProperties();
		System.out.println("properties = " + properties);

		// 배열 고속 복사
		char[] originalArray = {'h', 'e', 'l', 'l', 'o'};
		char[] copiedArray = new char[originalArray.length];
		System.arraycopy(originalArray, 0, copiedArray, 0, originalArray.length);

		System.out.println("copiedArray: " + Arrays.toString(copiedArray));
	}
}
```

#### Math, Random 클래스

* [Java - Math](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html)
* [Java - Random](https://docs.oracle.com/javase/8/docs/api/java/util/Random.html)
