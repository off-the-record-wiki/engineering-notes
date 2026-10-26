# 컴포지트 패턴 (Composite Pattern)

> 부분과 전체를 **같은 타입으로 다뤄서**, 트리 구조를 재귀로 처리하게 만드는 패턴.

세트 상품(장바구니)을 예제로 구현했다.

![컴포지트 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/composite-pattern.png)

---

## 1. 왜 필요한가

장바구니에 단품도 들어가고 세트도 들어간다. 세트 안에 또 세트가 들어갈 수도 있다.

컴포지트 없이 쓰면 이렇게 된다.

```java
// 컴포지트를 쓰지 않았을 때
public int totalPrice(List<Object> cart) {
    int sum = 0;
    for (Object item : cart) {
        if (item instanceof Product p) {
            sum += p.getPrice();
        } else if (item instanceof Bundle b) {
            sum += totalPrice(b.getItems());          // 재귀를 클라이언트가 짠다
            sum = applyDiscount(sum, b.getDiscount()); // 할인 규칙도 클라이언트가 안다
        }
    }
    return sum;
}
```

문제는 `instanceof`가 아니다. **트리를 순회하는 책임이 클라이언트로 넘어온 것**이다.

- 합계를 구하는 곳, 개수를 세는 곳, 화면에 그리는 곳이 **각자 같은 재귀를 짠다**
- 새 종류(예: 정기구독 묶음)가 생기면 그 `if`들을 전부 찾아 고쳐야 한다
- 묶음의 할인 규칙이 묶음 밖으로 새어 나온다

컴포지트는 **재귀를 객체 안으로 밀어 넣는다.**

```java
// 컴포지트를 썼을 때
int total = cart.stream().mapToInt(CartItem::price).sum();
```

`price()`를 부르면 단품이면 자기 값을, 묶음이면 자식들을 재귀로 합산한 값을 돌려준다. **클라이언트는 어느 쪽인지 묻지 않는다.**

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Component** | 단품과 묶음이 공유하는 규격 | `CartItem` |
| **Leaf** | 더 쪼갤 수 없는 것 — 재귀의 끝 | `Product` |
| **Composite** | 자식을 담고 재귀로 처리 | `Bundle` |
| **Client** | 부분과 전체를 구분하지 않는다 | `Main` |

### 핵심은 한 줄이다

```java
public class Bundle implements CartItem {
    private final List<CartItem> items = new ArrayList<>();   // ← 이 줄
}
```

담는 것이 **`Product`가 아니라 `CartItem`**이다. 이것 하나로 묶음 안에 묶음이 들어가고, 트리 깊이에 제한이 없어진다.

`Bundle`은 `CartItem`을 **구현하면서 `CartItem`을 담는다.** [데코레이터](decorator.md)의 "is-a이면서 has-a"와 모양이 같은데, 차이는 개수다. 데코레이터는 하나를 감싸고 컴포지트는 **여럿을 담는다.**

### 재귀는 Leaf에서 멈춘다

```java
// Leaf — 종료 조건
public class Product implements CartItem {
    @Override public int price() { return price; }   // 자기 값
    @Override public int count() { return 1; }       // 자기 하나
}

// Composite — 재귀 단계
public class Bundle implements CartItem {
    @Override
    public int price() {
        int sum = items.stream().mapToInt(CartItem::price).sum();   // 자식에게 같은 질문
        return sum * (100 - discountPercent) / 100;                 // 자기 로직
    }

    @Override
    public int count() {
        return items.stream().mapToInt(CartItem::count).sum();
    }
}
```

`Bundle.price()`는 자식이 단품인지 묶음인지 **묻지 않는다.** 같은 질문을 던지고 돌아온 답을 합칠 뿐이다.

**Composite가 자기 로직을 가져도 된다.** 여기서는 묶음 할인이 그것이다. 중첩되면 안쪽 할인이 먼저 걸리고 그 결과에 바깥 할인이 걸린다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out composite.code.Main
```

```
--- 1. 트리 ---
▣ 선물 세트 (10% 할인) → 62,370원
   ▣ 원두 세트 (5% 할인) → 32,300원
      · 에티오피아 예가체프 18,000원
      · 콜롬비아 수프리모 16,000원
   · 머그컵 12,000원
   · 드리퍼 25,000원

--- 2. 단품과 묶음을 구분하지 않는다 ---
  텀블러            22,000원  (상품 1개)
  선물 세트          62,370원  (상품 4개)

--- 3. 재귀로 계산된다 ---
  원두 세트   32,300원 = (18,000 + 16,000) × 95%
  선물 세트   62,370원 = (32,300 + 12,000 + 25,000) × 90%
  장바구니 합계 84,370원

--- 4. add() 는 Bundle 에만 있다 ---
  CartItem 으로 받으면 자식을 넣을 수 없다 — 안전성 방식의 대가이자 이득이다.
  대신 계산은 그대로 된다: 선물 세트 62,370원
```

**2번이 이 패턴의 목적이다.**

```java
List<CartItem> cart = List.of(new Product("텀블러", 22_000), giftSet);

for (CartItem item : cart) {
    System.out.printf("%s %,d원 (상품 %d개)%n", item.name(), item.price(), item.count());
}
```

이 반복문에 `if`도 `instanceof`도 없다. 텀블러는 `count()`가 1이고 선물 세트는 4인데, **세는 코드는 클라이언트에 없다.**

**3번은 재귀가 안쪽부터 바깥으로 올라오는 것**을 보여 준다. 원두 세트의 5% 할인이 먼저 적용되고, 그 결과가 선물 세트의 10% 할인 계산에 들어간다.

`print()`도 재귀다. 들여쓰기를 부모가 자식에게 넘겨 주면 트리 모양이 저절로 나온다.

```java
public void print(String indent) {
    System.out.printf("%s▣ %s …%n", indent, name);
    for (CartItem item : items) {
        item.print(indent + "   ");    // 자식은 한 칸 더 들여쓴다
    }
}
```

---

## 4. 새 종류를 추가하려면

정기구독 묶음이 생겼다고 하자. 12개월 치를 담고 매달 하나씩 나가는 것이다.

```java
public class Subscription implements CartItem {

    private final List<CartItem> months = new ArrayList<>();

    @Override public int price() { return months.stream().mapToInt(CartItem::price).sum(); }
    @Override public int count() { return months.stream().mapToInt(CartItem::count).sum(); }
    @Override public void print(String indent) { ... }
}
```

**클래스 하나 추가가 전부다.** 합계를 구하는 곳도, 개수를 세는 곳도, 출력하는 곳도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

`instanceof`로 짰다면 그 분기를 전부 찾아다녀야 했다.

---

## 5. 알아둘 점

### `add()`를 어디에 둘 것인가 — GoF의 유명한 트레이드오프

이 패턴에서 유일하게 고민할 지점이고, GoF도 정답을 정해 두지 않았다.

**투명성(transparency) — Component에 둔다**

```java
public interface CartItem {
    int price();
    void add(CartItem child);        // Leaf도 갖게 된다
}

public class Product implements CartItem {
    @Override
    public void add(CartItem child) {
        throw new UnsupportedOperationException("단품에는 담을 수 없다");
    }
}
```

- **장점** — 클라이언트가 Leaf와 Composite를 **완전히** 동일하게 다룬다. 캐스팅이 없다
- **단점** — `Product.add()`가 생기고, 그건 던질 예외 말고 할 일이 없다. **런타임에야 터진다**

이건 리스코프 치환 원칙을 흔든다. `CartItem`을 받은 코드가 `add()`를 부를 수 있다고 믿는데, 절반은 거짓이다.

**안전성(safety) — Composite에만 둔다 (이 예제)**

```java
public interface CartItem {
    int price();                     // add() 가 없다
}

public class Bundle implements CartItem {
    public Bundle add(CartItem child) { ... }    // Bundle 타입에만 있다
}
```

- **장점** — **컴파일러가 막아 준다.** 있을 수 없는 호출이 애초에 안 써진다
- **단점** — 자식을 넣으려면 `Bundle` 타입을 알아야 한다

**어느 쪽을 고를 것인가.** 판단 기준은 하나다 — **트리를 만드는 코드와 쓰는 코드가 나뉘어 있는가?**

나뉘어 있다면 안전성이 낫다. 조립할 때는 `Bundle` 타입을 알고 있고, 쓸 때는 `add()`가 필요 없기 때문이다. 실무에서는 대개 그렇다 (조립은 설정·팩토리에서, 사용은 서비스에서).

트리를 실행 중에 자유롭게 바꿔야 한다면 투명성이 편하다. 대신 `UnsupportedOperationException`을 감수한다.

> `java.util.List`의 `Collections.unmodifiableList()`가 정확히 이 딜레마의 사례다. `add()`가 인터페이스에 있어서 부를 수는 있지만 예외가 난다.

### `java.io.File`은 컴포지트가 아니다

파일과 디렉터리를 한 타입으로 다루니 컴포지트처럼 보이지만, 쓰는 쪽은 이렇게 된다.

```java
public long size(File file) {
    if (file.isDirectory()) {                    // ← 분기가 남아 있다
        long sum = 0;
        for (File child : file.listFiles()) {    // ← 파일이면 null 이다
            sum += size(child);                  // ← 재귀를 호출자가 짠다
        }
        return sum;
    }
    return file.length();
}
```

**재귀가 객체 안이 아니라 클라이언트에 있다.** 컴포지트였다면 `file.size()` 한 줄이었을 것이다. 한 타입으로 묶는 것만으로는 이 패턴이 되지 않는다는 좋은 반례다.

진짜 예는 AWT의 `Container extends Component`다. `Container`가 `Component` 목록을 담고, `paint()`가 재귀로 내려간다.

### 순환 참조를 막을 방법이 없다

```java
Bundle a = new Bundle("A", 0);
Bundle b = new Bundle("B", 0);
a.add(b);
b.add(a);        // 컴파일도 되고 실행도 된다

a.price();       // StackOverflowError
```

타입 시스템은 이걸 막지 못한다. 트리를 실행 중에 조립한다면 `add()`에서 검사가 필요하다.

```java
public Bundle add(CartItem item) {
    if (contains(item, this)) throw new IllegalArgumentException("순환 참조");
    ...
}
```

이 예제는 조립이 코드에 고정되어 있어 뺐다.

### 타입이 느슨해진다 — 이 패턴의 진짜 대가

```java
giftSet.add(new Product("머그컵", 12_000));
giftSet.add(anotherGiftSet);          // 선물 세트 안에 선물 세트? 막을 수 없다
```

"세트 안에는 단품만 온다", "깊이는 2단계까지" — **이런 제약을 타입으로 표현할 수 없다.** 전부 `CartItem`이기 때문이다.

부분과 전체를 같게 만든 대가로 **둘의 차이를 표현할 언어를 잃는다.** 그 차이가 도메인에서 중요하다면 이 패턴이 맞지 않는다는 뜻이다.

### 깊은 트리는 재귀 깊이를 조심한다

`price()`가 재귀 호출이므로 트리가 깊으면 `StackOverflowError`가 난다. 카테고리 트리나 조직도처럼 깊이가 수십 단계까지 갈 수 있다면 스택 대신 명시적 큐로 순회하는 편이 안전하다.

일반적인 상품 세트 정도(깊이 2~3)에서는 신경 쓸 일이 아니다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 금액 값 객체 (`int` 대신 `Money`) — 할인 계산의 원 단위 절사가 그냥 버려진다
- 순환 참조 검사와 깊이 제한
- 자식 목록의 방어적 복사 (`items`를 꺼내 주는 메서드가 없어서 지금은 안전하다)
- 부모 참조 — 자식에서 부모로 올라가야 한다면 필요하다. 대신 순환 구조가 되어 관리가 늘어난다
- 방문자 패턴과의 조합 — 트리에 연산을 계속 추가해야 한다면 그쪽이 낫다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 데이터가 **트리 구조**이고, 부분과 전체에 **같은 연산**을 하고 싶을 때
- 클라이언트 코드에서 `instanceof`로 부분/전체를 가르는 분기가 반복될 때
- 깊이가 정해져 있지 않을 때 (중첩 세트, 카테고리, 조직도, 메뉴, UI 컨테이너)

**안 쓴다**
- 구조가 트리가 아닐 때 — 억지로 만들면 관계가 왜곡된다
- **부분과 전체를 다르게 다뤄야** 할 때 — 같게 만드는 게 목적인 패턴이라 정반대다
- 타입으로 제약을 걸어야 할 때 — "여기엔 단품만" 같은 규칙을 표현할 수 없다
- 연산이 계속 늘어날 때 — 연산마다 Component에 메서드를 추가해야 한다. 방문자 패턴을 본다

판단은 한 줄이다. **"하나짜리와 여러 개짜리에 같은 질문을 던지고 싶은가?"** 그렇다면 컴포지트다.
