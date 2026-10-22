# (Effective Java Item 37) Java ‐ ordinal 인덱싱 대신 EnumMap을 사용하라

#### ordinal 인덱싱

```java
public class Snack {
    
    enum Type { BOX, BAG, BARREL }

    final String name;
    final Type type;

    Snack(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Snack{" + "name='" + name + '\'' + '}';
    }
}
```
```java
// Bad
public class ShoppingBasket {
  
    public static void main(String[] args) {
        // 과자의 타입마다 분리하기 위해 Set 배열을 만들고, 초기 사이즈를 과자의 총 타입의 갯수로 지정한다.
        Set<Snack>[] snacksByType = (Set<Snack>[]) new Set[Type.values().length];

        // 초기화를 해준다.
        for (int i = 0 ; i < snacksByType.length ; i++) {
            snacksByType[i] = new HashSet<>();
        }

        // 장바구니에 과자를 담는다.
        Snack[] shoppingBasket = {new Snack("오레오오즈", Type.BOX), new Snack("포카칩", Type.BAG), new Snack("바나나킥", Type.BAG), new Snack("닭다리스낵", Type.BOX), new Snack("콘칲", Type.BAG)};

        // 장바구니에 담은 과자들을 타입별로 분리한다.
        for (Snack snack : shoppingBasket) {
            snacksByType[snack.type.ordinal()].add(snack); // 이 때, ordinal()을 사용한다. (BOX = 0, BAG = 1)
        }

        // 결과를 출력한다.
        for (int i = 0; i < snacksByType.length; i++) {
            System.out.printf("%s: %s%n", Type.values()[i], snacksByType[i]);
        }
    }
}
```
* 배열은 제네릭과 호환이 되지 않기 때문에 비검사 형변환이 수행되고 컴파일이 안된다.
* 초기화를 할 때 정수 값을 잘못 입력하면 `ArrayIndexOutOfBoundException`이 발생한다.
* `ordinal()`은 상수 선언 순서에 따라 바뀌기 때문에 안전하지 못하다.

#### EnumMap

* Map 인터페이스에서 키를 특정 열거형 타입만을 사용하도록 하는 구현체이다.
* Enum을 key로 쓰는 Map이 필요할 때 적합하다.

```java
// Good
public class ShoppingBasket {
  
    public static void main(String[] args) {
        // 과자의 타입마다 분리하기 위해 EnumMa을 사용한다.
        Map<Snack.Type, Set<Snack>> snacksByType = new EnumMap<>(Snack.Type.class);

        // 초기화를 해준다.
        for (Snack.Type type : Snack.Type.values()) {
            snacksByType.put(type, new HashSet<>());
        }

        // 장바구니에 과자를 담는다.
        Snack[] shoppingBasket = {
            new Snack("오레오오즈", Type.BOX),
            new Snack("포카칩", Type.BAG),
            new Snack("바나나킥", Type.BAG),
            new Snack("닭다리스낵", Type.BOX),
            new Snack("콘칲", Type.BAG)
        };

        // 장바구니에 담은 과자들을 타입별로 분리한다.
        for (Snack snack : shoppingBasket) {
            snacksByType.get(snack.type).add(snack);
        }

        // 결과를 출력한다.
        System.out.println(snacksByType);
    }
}
```
* 배열 인덱스를 계산하는 과정에서 오류가 발생할 일이 없고 타입 안전하게 사용할 수 있다.

#### Stream(EnumMap X)

```java
public class ShoppingBasket {
    
    public static void main(String[] args) {
        // 장바구니에 과자를 담는다.
        Snack[] shoppingBasket = {
            new Snack("오레오오즈", Type.BOX),
            new Snack("포카칩", Type.BAG),
            new Snack("바나나킥", Type.BAG),
            new Snack("닭다리스낵", Type.BOX),
            new Snack("콘칲", Type.BAG)
        };

        // 장바구니에 담은 과자들을 타입별로 분리한 결과를 출력한다.
        System.out.println(Arrays.stream(shoppingBasket)
            .collect(groupingBy(s -> s.type)));
    }
}
```

#### Stream(EnumMap O)

```java
public class ShoppingBasket {

    public static void main(String[] args) {
        // 장바구니에 과자를 담는다.
        Snack[] shoppingBasket = {
            new Snack("오레오오즈", Type.BOX),
            new Snack("포카칩", Type.BAG),
            new Snack("바나나킥", Type.BAG),
            new Snack("닭다리스낵", Type.BOX),
            new Snack("콘칲", Type.BAG)
        };

        // 장바구니에 담은 과자들을 타입별로 분리한 결과를 출력한다.
        System.out.println(Arrays.stream(shoppingBasket)
            .collect(groupingBy(s -> s.type, () -> new EnumMap<>(Type.class), toSet())));
    }
}
```

* 배열의 인덱스를 얻기 위해 ordinal을 쓰는 것은 일반적으로 권장하지 않으니, 대신 `EnumMap`을 사용하라.
* 다차원 관계의 경우라면 `EnumMap<..., EnumMap<...>>`으로 표현하라.
