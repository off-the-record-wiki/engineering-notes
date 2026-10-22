# (Effective Java Item 53) Java ‐ 가변인수는 신중히 사용하라

* 가변인수(varargs) 메서드는 명시한 타입의 인수를 0개 이상 받을 수 있다.
* 가변인수 메서드를 호출하면 가장 먼저 인수 개수와 길이가 같은 배열을 만들고 이 인수들을 배열에 저장하여 가변인수 메서드에 건네준다.
* 만약 다음과 같이 인수가 1개 이상이어야 할 때도 있다. 예컨대 최솟값을 찾는 메서드인데, 인수를 0개만 받을 수 있도록 설계하는 것은 좋지 않다. 인수 개수는 런타임에 자동으로 생성된 배열 길이로 알 수 있다.

```java
// Bad
static int min(int ... args) {
    if (args.length == 0) {
        throw new IllegalArgumentException("인수가 1개 이상 필요합니다.");
    }

    int min = args[0];
    for (int i = 1; i < args.length; i++) {
        if (args[i] < min) {
            min = args[i];
        }
    }
    return min;
}
```
* 만약 인수를 0개 넣어 호출하게 된다면 런타임에 실패하게 된다.
* args 유효성 검사를 명시적으로 해야 하고 min의 초기값을 `Integer.MAX_VALUE`로 설정하지 않고는 for-each문도 사용할 수 없다.
* 다음과 같이 매개변수를 2개 받도록 하면 된다.
```java
static int min(int firstArg, int ... args) {
    int min = firstArg;
    for (int arg: args) {
        if (arg < min) {
            min = arg;
        }
    }
    return min;
}
```
* 그러나 성능에 민감한 상황이라면 가변인수가 문제가 될 수 있다.
* **가변인수 메서드의 경우 호출될 때마다 배열을 새로 하나 할당하고 초기화를 한다.**
* 만약 가변인수를 필요로 한다면 메서드를 정의할 때, 필수 매개변수는 가변 인수 앞에 두고, 가변인수를 사용할 때는 성능 문제를 같이 고려하자.
