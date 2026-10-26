# 커맨드 패턴 (Command Pattern)

> 요청을 **객체로 캡슐화**해서, 호출자가 요청의 내용을 몰라도 실행·취소·지연·큐잉할 수 있게 하는 패턴.

문서 편집기의 삽입·삭제·실행 취소를 예제로 구현했다.

![커맨드 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/command-pattern.png)

---

#### 1. 왜 필요한가

텍스트 편집기가 있다. 버튼을 누르면 글자가 들어가거나 지워지고, "실행 취소" 버튼을 누르면 방금 한 일이 되돌아가야 한다.

가장 직관적인 구현은 버튼 핸들러가 `Document`를 직접 부르고, 되돌리는 방법을 자기가 기억해 두는 것이다.

```java
// 커맨드 패턴을 쓰지 않았을 때
public class Editor {

    private final Document document;
    private final Deque<Object[]> history = new ArrayDeque<>();   // ← [타입, 좌표, 텍스트] 뒤죽박죽

    public void onInsert(int position, String text) {
        document.insert(position, text);
        history.push(new Object[]{"INSERT", position, text});     // ← 되돌리는 법을 호출부가 기억해야 한다
    }

    public void onDelete(int position, int length) {
        String deleted = document.text().substring(position, position + length);
        document.delete(position, length);
        history.push(new Object[]{"DELETE", position, deleted});
    }

    public void undo() {
        Object[] last = history.pop();
        switch ((String) last[0]) {                                // ← 액션이 늘 때마다 이 switch가 자란다
            case "INSERT" -> document.delete((int) last[1], ((String) last[2]).length());
            case "DELETE" -> document.insert((int) last[1], (String) last[2]);
        }
    }
}
```

**되돌리는 방법을 아는 쪽이 `Editor`다.** 액션이 하나 늘 때마다 `onXxx` 메서드와 `undo()`의 분기를 같이 고쳐야 한다. 여러 동작을 묶어 매크로로 만들거나, 재실행(redo)을 지원하거나, 실행을 큐에 쌓아 나중에 처리하는 건 이 구조 위에서는 손대기가 괴롭다 — 전부 `Editor` 한 클래스에 몰려 있기 때문이다.

커맨드 패턴은 **"무엇을, 어떻게 되돌리는지"를 액션 하나하나가 스스로 짊어지게 만든다.**

```java
// 커맨드 패턴을 썼을 때 — Editor(Invoker)는 Command 하나만 안다
public class EditorInvoker {

    private final Deque<Command> history = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        history.push(command);
    }

    public void undo() {
        history.pop().undo();      // ← 무슨 명령인지 몰라도 되돌릴 수 있다
    }
}
```

`EditorInvoker`는 커맨드가 몇 종류인지 모른다. 새 액션이 추가돼도 이 클래스는 한 줄도 바뀌지 않는다.

---

#### 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Command** | 요청을 캡슐화하는 규격 | `Command` |
| **ConcreteCommand** | 실제 요청 내용 + 되돌리는 법 | `InsertTextCommand` · `DeleteTextCommand` · `MacroCommand` |
| **Receiver** | 진짜 일을 하는 쪽 | `Document` |
| **Invoker** | 커맨드를 실행시키는 쪽 | `EditorInvoker` |
| **Client** | 커맨드를 만들고 Receiver를 연결한다 | `Main` |

#### Command가 있어야 성립한다

```java
public interface Command {
    void execute();
    void undo();
}
```

`EditorInvoker`가 아는 건 이 인터페이스뿐이다. **무엇을, 누구에게** 시키는지 모르기 때문에 실행을 나중으로 미룰 수도, 스택에 쌓아 되돌릴 수도, 여러 개를 묶을 수도 있다. 이 인터페이스가 없으면 `EditorInvoker`가 `Document`를 직접 알아야 하고, 그 순간 액션의 가짓수만큼 분기가 생긴다.

#### undo는 커맨드마다 계산 방식이 다르다

**삽입의 undo — 계산으로 끝난다**

```java
@Override
public void undo() {
    document.delete(position, text.length());   // 넣은 자리에서, 넣은 길이만큼
}
```

삽입은 자기가 넣은 텍스트를 알고 있으니 길이만 알면 지울 수 있다. 별도로 기억해 둘 게 없다.

**삭제의 undo — 실행 전에 미리 기억해야 한다**

```java
private String deletedText;   // 아직 없다. execute()가 채운다

@Override
public void execute() {
    deletedText = document.text().substring(position, position + length);   // 지우기 전에 챙긴다
    document.delete(position, length);
}

@Override
public void undo() {
    document.insert(position, deletedText);
}
```

`Document.delete(position, length)`는 지운 내용을 돌려주지 않는다. **되돌리려면 지우기 직전에 커맨드가 스스로 챙겨 둬야 한다.** `Document`는 이 사정을 전혀 모른다 — 지우라면 지울 뿐이다.

**매크로 — 여러 커맨드를 하나로, undo는 역순으로**

```java
@Override
public void execute() {
    for (Command command : commands) {
        command.execute();
    }
}

@Override
public void undo() {
    for (int i = commands.size() - 1; i >= 0; i--) {   // ← 실행의 역순
        commands.get(i).undo();
    }
}
```

두 번째 커맨드가 첫 번째 커맨드의 결과 위에서 실행됐다면, 되돌릴 때도 두 번째부터 걷어내야 첫 번째가 가정한 상태로 돌아간다. 순서를 거꾸로 하면 좌표가 어긋난다.

---

#### 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out command.code.Main
```

```
--- 1. 명령 실행 — Invoker는 Command만 안다 ---
  → Hello
  → Hello World

--- 2. 실행 취소(undo) — Document는 undo를 모른다 ---
  → Hello
  → ''

--- 3. 다시 실행(redo) ---
  → Hello

--- 4. 새 명령을 실행하면 redo 기록은 사라진다 ---
  → Hello!
    [다시 실행] 다시 실행할 명령이 없다

--- 5. 삭제 명령 — undo하려면 지운 값을 스스로 기억해야 한다 ---
  → Hello! Command
  → Command
  → Hello! Command

--- 6. 매크로 명령 — 여러 Command를 하나로 실행·취소 ---
  → Hello! **Command**
  → Hello! Command
→ Invoker는 이 매크로가 명령 두 개로 이루어졌다는 걸 몰랐다.
```

**증거는 `EditorInvoker`와 `Document`가 서로에 대해 아무것도 모른다는 것이다.**

- 1·2번 — 삽입 두 번, undo 두 번으로 빈 문서까지 정확히 돌아간다. `Document`에는 undo 코드가 한 줄도 없다
- 3·4번 — redo는 실행 취소를 다시 실행하는 것뿐이다. 그런데 되돌린 뒤 **새 명령을 실행하면 redo 기록이 사라진다** — 갈림길에서 버려진 미래는 되살릴 수 없다는, 대부분의 에디터가 따르는 규칙이다
- 5번 — 삭제했다가 되돌리니 지워졌던 `"Hello! "`가 정확히 복원된다. `DeleteTextCommand`가 실행 시점에 값을 챙겨 둔 덕분이다
- 6번 — `MacroCommand` 하나를 실행·취소했을 뿐인데 별표 두 쌍이 정확히 나타나고 사라진다. `EditorInvoker`는 이게 커맨드 두 개로 이루어졌다는 사실 자체를 모른다

---

#### 4. 새 명령을 추가하려면

```java
public class UppercaseCommand implements Command {

    private final Document document;
    private final int position;
    private final int length;
    private String original;

    public UppercaseCommand(Document document, int position, int length) {
        this.document = document;
        this.position = position;
        this.length = length;
    }

    @Override
    public void execute() {
        original = document.text().substring(position, position + length);
        document.delete(position, length);
        document.insert(position, original.toUpperCase());
    }

    @Override
    public void undo() {
        document.delete(position, original.toUpperCase().length());
        document.insert(position, original);
    }
}
```

**클래스 하나 추가가 전부다.** `EditorInvoker`도, `Document`도, 다른 커맨드도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

---

#### 5. 알아둘 점

#### 전략 패턴과의 차이 — 구조는 같고 시간 축이 다르다

클래스 다이어그램만 보면 [전략 패턴](strategy.md)과 거의 같다. 둘 다 인터페이스를 정의하고, 구현체를 주입받아 위임한다.

차이는 **캡슐화하는 대상과, 그것이 언제 실행되느냐**에 있다.

| | 전략 패턴 | 커맨드 패턴 |
|---|---|---|
| 캡슐화하는 것 | 알고리즘 (같은 일을 하는 여러 방법) | 요청 (무엇을, 누구에게, 언제) |
| 실행 시점 | 즉시, 그 자리에서 | 나중일 수 있다 (스택, 큐, 스케줄러) |
| 되돌리기 | 개념이 없다 | undo가 핵심 동기 중 하나 |
| 상태 | 대개 무상태 | 되돌릴 값을 들고 있는 경우가 흔하다 |

전략은 "정렬을 퀵정렬로 할까 병합정렬로 할까"처럼 **같은 문제의 다른 풀이**를 고르는 것이다. 커맨드는 "이 버튼을 누르면 무슨 일이 일어나야 하는가"라는 **요청 자체**를 대상으로 캡슐화한다. `DeleteTextCommand`가 `deletedText`를 들고 있는 것처럼, 커맨드는 실행 시점과 되돌리는 시점 사이의 **시간을 넘나드는 상태**를 가질 수 있다 — 전략에는 없는 특징이다.

#### 컴포지트 패턴과 겹치는 지점

`MacroCommand`는 사실상 **컴포지트 패턴**이다 — `Command` 자체가 `Command`의 목록을 담아 재귀적으로 구성될 수 있다. 매크로 안에 매크로를 넣는 것도 그대로 동작한다. GoF도 커맨드의 확장으로 매크로 커맨드를 별도로 다룬다.

#### 메멘토 패턴 — undo를 만드는 또 다른 방법

이 예제의 undo는 **반대 동작을 실행하는 방식**이다 (삽입 ↔ 삭제). undo를 만드는 다른 방법은 상태 자체를 통째로 저장했다가 그대로 복원하는 것 — **메멘토 패턴**이다.

| | 커맨드의 undo | 메멘토 |
|---|---|---|
| 되돌리는 방법 | 반대 동작을 실행 | 이전 상태를 통째로 복원 |
| 저장하는 것 | 명령당 필요한 값만 (`deletedText` 등) | 상태 스냅샷 전체 |
| 유리한 경우 | 반대 동작이 명확하고 저렴할 때 | 반대 동작을 정의하기 어렵거나, 상태가 복잡할 때 |

실무에서는 둘을 섞기도 한다 — 되돌리기 쉬운 동작은 커맨드의 반대 실행으로, 상태가 복잡해서 반대 동작을 정의하기 어려운 동작은 스냅샷 복원으로 처리한다.

#### 익숙한 곳에 이미 있다

```java
executorService.submit(() -> processOrder(orderId));   // Runnable = 커맨드
```

`Runnable`·`Callable`을 `ExecutorService`에 제출하는 것 자체가 커맨드 패턴이다. "지금, 여기서" 실행하는 대신 **요청을 객체로 감싸 다른 스레드가, 나중에** 실행하게 만든다. Swing의 `javax.swing.Action`도 마찬가지다 — 버튼과 메뉴가 같은 `Action` 객체를 공유해서, 하나만 비활성화해도 둘 다 비활성화된다. 이벤트 소싱에서 "일어난 일을 이벤트로 기록해 두었다가 재생한다"는 것도 같은 발상의 연장이다.

#### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 스레드 안전 — `EditorInvoker`의 `history`/`redoStack`은 동시 접근을 가정하지 않는다
- 커맨드 직렬화 — 지금은 프로세스가 죽으면 undo 이력도 같이 사라진다. 영속화하려면 커맨드를 데이터로 저장할 방법이 필요하다
- undo 실패 처리 — 되돌리려는데 이미 다른 곳에서 상태가 바뀌어 있는 경우 (동시 편집)
- 비동기 실행 — 지금은 `execute()`가 즉시, 동기로 끝난다. 큐에 쌓아 별도 스레드가 소비하게 만드는 것도 이 패턴이 자주 쓰이는 자리다

---

#### 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 실행 취소·다시 실행을 지원해야 할 때
- 요청을 **큐에 쌓거나, 로깅하거나, 나중에** 실행해야 할 때
- 버튼·메뉴·단축키처럼 **같은 동작을 여러 진입점이 공유**해야 할 때
- 여러 동작을 하나의 매크로로 묶어야 할 때

**안 쓴다**
- 그 자리에서 한 번 실행하고 끝이면 그냥 메서드 호출로 충분하다 — 커맨드로 감싸는 순간 클래스 수만 늘어난다
- 되돌릴 필요도, 지연 실행도 없다면 [전략 패턴](strategy.md)이나 단순 콜백이 낫다
- 이미 프레임워크가 해 주고 있을 때 — 커맨드 버스(Axon, MediatR류)나 잡 스케줄러가 있다면 직접 만들 이유가 없다

판단은 한 줄이다. **"이 요청을 지금, 한 번만, 같은 자리에서 실행하고 끝인가?"** 아니라면 커맨드다.
