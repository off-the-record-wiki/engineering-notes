# (Clean Architecture 8) - OCP: 개방-폐쇄 원칙

#### OCP: 개방-폐쇄 원칙

![](https://github.com/user-attachments/assets/3a61d0ae-2494-4c5c-ac3e-0acd3c842f5f)

* 기준: "새 종류가 하나 더 생기면 이 코드를 고쳐야 하나?" → 예라면 OCP 신호이다.

1. 타입 코드로 분기

```java
double fee(Payment p) {
    if (p.getType() == CARD)  return p.amount() * 0.025;
    if (p.getType() == KAKAO) return p.amount() * 0.02;
    if (p.getType() == BANK)  return 300;
}
```
* 볼 곳: `type ==`, `switch (type)`처럼 종류를 나타내는 값으로 동작을 고르는 부분이다.
* 다른 종류의 페이가 추가되면 이 메서드를 열어 `if`를 하나 더 넣어야 한다.

2. `instanceof` 체인

```java
void draw(Shape s) {
    if (s instanceof Circle c)      drawCircle(c);
    else if (s instanceof Rect r)   drawRect(r);
    else if (s instanceof Triangle t) drawTriangle(t);
}
```
* 볼 곳: 객체의 실제 클래스를 검사해 분기하는 부분이다.
* 새로 도형 타입이 늘어날 때마다 `else if`가 늘어난다. 각 도형이 `draw()`를 직접 갖게 하면 다형성으로 이 분기가 사라진다.

3. 같은 분기가 여러 메서드에 반복

```java
void pay(Type t)    { switch (t) { case CARD: ... case KAKAO: ... } }
void refund(Type t) { switch (t) { case CARD: ... case KAKAO: ... } }
String label(Type t){ switch (t) { case CARD: ... case KAKAO: ... } }
```
* 볼 곳: `case` 목록이 여러 곳에 복사돼 있는 부분이다.
* 하나를 추가할 때 모든 곳을 찾아 고쳐야 하고, 한 곳이라도 빠뜨리면 버그가 발생한다.

4. 형식이나 채널을 문자열 파라미터로 고르기

```java
void export(Report r, String format) {
    if (format.equals("csv"))   { ... }
    else if (format.equals("pdf")) { ... }
    else if (format.equals("excel")) { ... }
}

void notify(User u, String channel) {
    if (channel.equals("email")) { ... }
    else if (channel.equals("sms")) { ... }
}
```
* 볼 곳: "csv", "sms" 같은 문자열이나 enum으로 출력 형식, 알림 채널, 저장소를 고르는 부분이다.
* 형식이나 채널은 계속 늘어나는 대표적인 영역이다. `Exporter`, `Notifier` 인터페이스와 구현 클래스로 나누면 추가적으로 확장할 수 있다.

5. 기능을 켜고 끄는 boolean 플래그가 쌓임 

```java
Price calc(Order o, boolean isVip, boolean isEvent, boolean hasCoupon) {
    if (isVip) ...
    if (isEvent) ...
    if (hasCoupon) ...
}
```
* 볼 곳: 정책이 추가될 때마다 파라미터와 `if`가 늘어나는 메서드이다.
* 정책이 추가될 때마다 시그니처와 본문을 모두 고쳐야 한다. 인터페이스를 받아 차례로 적용하게 바꾸면 새로운 정책은 클래스만 추가하면 된다.
