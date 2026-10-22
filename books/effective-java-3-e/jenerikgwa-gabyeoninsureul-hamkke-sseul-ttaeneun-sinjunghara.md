# (Effective Java Item 32) Java - 제네릭과 가변인수를 함께 쓸 때는 신중하라

#### 제네릭과 가변인수를 함께 쓸 때는 신중하라

* 가변인수는 메서드에 넘기는 인수의 개수를 가변적으로 조절할 수 있게 해주는 것을 말한다.
* 가변인수 메서드를 호출하면 가변인수를 담기 위한 배열이 자동으로 하나 만들어진다.
* 그런데 내부로 감춰야할 이 배열을 그만 클라이언트에 노출하는 문제가 생긴다. 그 결과 varargs 매개변수에 제네릭이나 매개변수화 타입이 포함되면 알기 어려운 컴파일 경고가 발생한다.
```java
public static void display(String ...infos) {
    for (String info: infos) {
        System.out.println(s);
    }
}
```

#### 제네릭과 varargs를 혼용하면 타입 안전성이 깨진다.

```java
import java.util.List;

public class Main {

    public static void test(List<String>... stringList) {
        // 컴파일러 경고: Possible heap pollution from parameterized vararg type.
        
        List<Integer> integerList = List.of(42);
        
        // 인텔리제이 경고: Local variable 'objects' is redundant.
        Object[] objects = stringList; 
        
        // 힙 오염 발생 시점: List<String> 배열인 곳에 List<Integer>를 강제로 주입
        objects[0] = integerList;
        
        // 인텔리제이 경고: Variable 's' is never used. Remove this unused "s" local variable. ...
        // 런타임 에러 발생 시점: String을 기대했으나 내부적으로 Integer(42)가 들어있어 ClassCastException 발생
        String s = stringList[0].get(0);
    }

    public static void main(String[] args) {
        // 인텔리제이 경고: Unchecked generics array creation for varargs parameter.
        test(List.of("Dangerous!"));
    }
}
```

* `List<String>...`은 내부적으로 `List<String>[]` 배열로 처리된다.
* 제네릭 배열은 런타임에 타입 정보가 소거된다.
* `get(0)` 메서드를 호출하는 부분에서 ClassCastException이 발생하게 된다.
* 제네릭 가변인자는 배열 공변성과 타입 소거로 인해 타입 안전성이 깨질 수 있다.

#### @SafeVarags 어노테이션

* @SafeVarargs 어노테이션은 메서드 작성자가 그 메서드가 타입 안전함을 보장하는 장치이다.
* 컴파일러는 이 약속을 믿고 그 메서드가 안전하지 않을 수 있다는 경고를 더 이상 하지 않는다.
* 따라서, **메서드가 안전한 게 확실하지 않다면 절대 @SafeVarargs 애너테이션을 달아서는 안 된다.**

#### 제네릭 varargs 매개변수를 안전하게 사용하는 방법

```java
public class TimeCapsule {
    
    @SafeVarargs
    static <T> List<T> saveCapsule(List<? extends T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<? extends T> list : lists)
            result.addAll(list);
        return result;
    }

    public static void main(String[] args) {
        List<String> timeCapsule = saveCapsule(
            List.of("편지", "인형"), List.of("USB", "편지"), List.of("반지","사진"));
        System.out.println(timeCapsule);
    }
}
```

>**❗가변인자의 내부 동작**

```java
saveCapsule(List.of("a"), List.of("b"))
```
* 위와 같이 호출하게 되면 내부적으로 아래와 같이 배열이 자동으로 생성된다.
```java
List<? extends T>[] lists = new List[]{List.of("a"), List.of("b")};
```

>**❗배열의 공변성 문제**
```java
List<String>[] arr = new List[2];
Object[] objects = arr;     // 배열은 공변이라 가능
objects[0] = List.of(123);  // 컴파일 OK, 런타임에 문제 없음
String s = arr[0].get(0);   // ClassCastException(Integer를 String으로)
```
