# 책임 연쇄 패턴 (Chain of Responsibility Pattern)

> 요청을 처리할 후보를 **사슬로 늘어세우고**, 각자 "내가 처리할까, 다음에게 넘길까"를 스스로 판단하게 하는 패턴.

경비 승인을 예제로 구현했다.

![책임 연쇄 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/chain-of-responsibility-pattern.png)

---

#### 1. 왜 필요한가

경비 신청을 승인하는 서비스가 있다. 금액에 따라 승인자가 다르다 — 30만 원 이하는 팀장, 100만 원 이하는 매니저, 500만 원 이하는 이사, 그 이상은 대표이사.

가장 직관적인 구현은 이 규칙을 한 메서드에 다 적는 것이다.

```java
// 책임 연쇄를 쓰지 않았을 때
public class ApprovalService {

    public ApprovalResult approve(ExpenseRequest request) {
        long amount = request.amount();
        if (amount <= 300_000) {
            return ApprovalResult.approvedBy("팀장", request);
        } else if (amount <= 1_000_000) {
            return ApprovalResult.approvedBy("매니저", request);
        } else if (amount <= 5_000_000) {
            return ApprovalResult.approvedBy("이사", request);
        } else {
            return ApprovalResult.approvedBy("대표이사", request);
        }
    }
}
```

**`ApprovalService` 하나가 모든 승인자의 한도를 다 알아야 한다.** 승인자가 늘거나(감사 담당 추가), 한도가 바뀌거나, 승인자별로 다른 부가 동작(승인 시 알림 발송, 특정 사유는 감사 로그 남기기)이 붙을 때마다 이 메서드가 계속 자란다. 승인 단계를 재배치하는 것도 이 메서드를 다시 쓰는 것과 같다.

책임 연쇄는 각 승인자를 **자기 판단만 하는 독립된 객체**로 뽑아내고, 처리 못 하면 다음 객체에게 넘긴다.

```java
// 책임 연쇄를 썼을 때 — 각 핸들러는 자기 한도만 안다
public final ApprovalResult approve(ExpenseRequest request) {
    if (canApprove(request)) {
        return ApprovalResult.approvedBy(role(), request);
    }
    return next.approve(request);   // ← 모르면 넘긴다. 누구에게 넘기는지도 상관 안 한다
}
```

`ApprovalService`라는 중앙 판단 클래스는 사라진다. 팀장은 팀장의 한도만, 매니저는 매니저의 한도만 안다.

---

#### 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Handler** | 처리 여부를 판단하고, 아니면 다음으로 넘기는 공통 규격 | `ApprovalHandler` |
| **ConcreteHandler** | 실제 처리 조건 | `TeamLeadHandler` · `ManagerHandler` · `DirectorHandler` · `CeoHandler` |
| **Client** | 체인을 조립하고 요청을 첫 핸들러에 보낸다 | `Main` |

#### Handler가 있어야 성립한다

```java
public abstract class ApprovalHandler {

    private ApprovalHandler next;

    public ApprovalHandler linkWith(ApprovalHandler next) {
        this.next = next;
        return next;
    }

    public final ApprovalResult approve(ExpenseRequest request) {
        if (canApprove(request)) {
            return ApprovalResult.approvedBy(role(), request);
        }
        if (next == null) {
            return ApprovalResult.rejected(request);
        }
        return next.approve(request);
    }

    protected abstract boolean canApprove(ExpenseRequest request);
    protected abstract String role();
}
```

`approve()`는 `final`이다 — **체이닝 로직은 템플릿 메서드로 한 곳에 고정해 뒀다.** 하위 클래스가 결정하는 건 "내가 처리할 수 있는가"(`canApprove`) 하나뿐이다. 다음 핸들러를 부를지, 언제 멈출지를 하위 클래스마다 다시 구현하게 두면 누군가는 체이닝을 까먹거나 틀리게 짤 수 있다. [팩토리 메서드](factory-method.md)에서 본 것과 같은 이유로 템플릿 메서드를 섞어 썼다.

#### 체인은 누가 조립하는가 — Client의 책임

```java
ApprovalHandler teamLead = new TeamLeadHandler();
teamLead.linkWith(new ManagerHandler()).linkWith(new DirectorHandler()).linkWith(new CeoHandler());
```

각 핸들러는 자기 다음이 누군지 **생성 시점에는 모른다.** `linkWith`로 나중에 연결될 뿐이다. 체인의 순서와 구성을 결정하는 건 어디까지나 `Main`(Client)이다 — 핸들러 자신은 자기가 몇 번째인지도 모른다.

---

#### 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out chain_of_responsibility.code.Main
```

```
--- 1. 금액에 따라 처리자가 달라진다 ---
    [팀장] 150,000원 — 내 권한 안, 승인
  → 이서준의 150,000원 요청 — 팀장 승인

    [팀장] 700,000원 — 내 권한 밖, 다음으로 넘긴다
    [매니저] 700,000원 — 내 권한 안, 승인
  → 김하나의 700,000원 요청 — 매니저 승인

    [팀장] 3,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [매니저] 3,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [이사] 3,000,000원 — 내 권한 안, 승인
  → 박도윤의 3,000,000원 요청 — 이사 승인

    [팀장] 10,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [매니저] 10,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [이사] 10,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [대표이사] 10,000,000원 — 내 권한 안, 승인
  → 최지우의 10,000,000원 요청 — 대표이사 승인

--- 2. 체인이 짧으면 아무도 처리하지 못할 수 있다 ---
    [팀장] 3,000,000원 — 내 권한 밖, 다음으로 넘긴다
    [매니저] 3,000,000원 — 내 권한 밖인데 다음 핸들러가 없다
  → 반려 — 이 체인엔 처리할 수 있는 핸들러가 없다

--- 3. 순서가 곧 정책이다 ---
    [매니저] 150,000원 — 내 권한 안, 승인
  → 매니저를 앞에 두니, 팀장 권한 안에서도 매니저가 승인했다
```

**증거는 로그에 찍힌 핸들러의 개수와 순서다.**

- 1번 — 금액이 클수록 더 많은 핸들러를 거친다. 150,000원은 팀장에서 한 줄로 끝나고, 10,000,000원은 네 핸들러를 다 거친다. 각 핸들러는 자기 앞뒤에 누가 있는지 모른 채로 딱 자기 몫만 판단했다
- 2번 — 체인을 팀장→매니저로 짧게 만들면 300만 원 요청은 아무도 처리하지 못한다. **`next == null`에서 멈추는 것도 이 패턴의 정상적인 종료 조건이다** — 처리 실패가 예외적인 상황이 아니라 설계에 포함돼 있다
- 3번 — 매니저를 팀장보다 앞에 두면, 원래 팀장이 처리했을 15만 원짜리 요청도 매니저가 가로챈다. **체인의 순서가 곧 정책이다.** [프록시](proxy.md)에서 본 것과 같은 원리가 여기도 적용된다

---

#### 4. 새 핸들러를 추가하려면

```java
public class AuditLogHandler extends ApprovalHandler {

    @Override
    protected boolean canApprove(ExpenseRequest request) {
        System.out.printf("    [감사 로그] %s의 %,d원 요청 기록%n", request.requester(), request.amount());
        return false;   // 승인 권한은 없다 — 기록만 남기고 항상 다음으로 넘긴다
    }

    @Override
    protected String role() {
        return "감사 로그";
    }
}
```

```java
ApprovalHandler audit = new AuditLogHandler();
audit.linkWith(teamLead).linkWith(manager).linkWith(director).linkWith(ceo);
```

**클래스 하나 추가와 조립 순서 한 줄이 전부다.** 기존 네 핸들러는 이 핸들러의 존재를 모른다. `AuditLogHandler`는 `canApprove`가 절대 `true`를 반환하지 않는다는 점이 특이한데, 이 패턴에서 핸들러가 반드시 체인을 멈출 필요는 없다는 걸 보여준다 — **항상 통과시키되 지나가는 길목에서 뭔가를 하는 미들웨어**도 똑같은 구조로 만들 수 있다. → 개방-폐쇄 원칙(OCP)

---

#### 5. 알아둘 점

#### 데코레이터 패턴과의 차이 — 이번에도 구조는 같고 위임이 다르다

[데코레이터](decorator.md)·[프록시](proxy.md)에 이어 또 같은 질문이 나온다. 클래스가 자기 자신과 같은 타입을 들고 위임한다는 구조는 똑같다.

| | 데코레이터 | 책임 연쇄 |
|---|---|---|
| 감쌀 대상 | 생성자로 받는다 | 생성자로 받거나 `linkWith`로 나중에 조립 |
| 위임 | **항상 한다**, 자기 일도 같이 한다 | **조건에 따라 멈출 수 있다** |
| 목적 | 기능을 더한다 | 처리할 대상을 **찾는다** |
| 실행되는 개수 | 겹친 것 전부 | 보통 **하나**에서 멈춘다 |

이 예제의 `AuditLogHandler`는 예외적으로 항상 다음으로 넘긴다는 점에서 데코레이터에 가깝게 행동한다 — 패턴의 경계는 항상 이렇게 흐릿해질 수 있다. 판단 기준은 **"멈출 수 있는 경로가 있는가"** 다.

#### 옵저버 패턴과의 차이 — 하나가 반응하는가, 전부가 반응하는가

[옵저버](observer.md)는 이벤트 하나에 등록된 리스너 **전부**가 반응한다 (통지). 책임 연쇄는 체인에 등록된 핸들러 중 **보통 하나만** 처리하고 멈춘다 (에스컬레이션). `AuditLogHandler`처럼 항상 통과하는 핸들러를 여러 개 두면 옵저버와 결과가 비슷해 보이지만, 의도가 다르다 — 옵저버는 "이 일이 일어났다는 걸 알리는 것"이 목적이고, 책임 연쇄는 "이 일을 처리할 사람을 찾는 것"이 목적이다.

#### 실무에서는 이미 쓰고 있다

- 서블릿 `Filter`/`FilterChain` — `chain.doFilter(request, response)`를 부르는 자리가 이 예제의 `next.approve(request)`와 같다
- 스프링 시큐리티의 `FilterChainProxy` — 인증·인가·CSRF 검사를 각각 다른 필터가 맡고, 처리되면 그 자리에서 멈추거나 다음으로 넘긴다
- Express.js·Koa 같은 미들웨어의 `next()`
- `java.util.logging.Logger`의 핸들러 체인 — 로그 레벨에 따라 부모 로거로 계속 전파된다. 이름부터가 `Handler`다

#### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 순환 방지 — `linkWith`는 이미 등록된 핸들러를 다시 연결해도 막지 않는다. 실수로 순환 구조를 만들면 무한 재귀에 빠진다
- 체인 구성의 외부화 — 지금은 `Main`에 조립 순서가 하드코딩돼 있다. 설정 파일이나 스프링 빈 순서로 옮기는 게 실무에 가깝다
- 비동기·병렬 처리 — 지금은 동기·순차 호출뿐이다
- 부분 처리 후 계속 진행 — 지금은 "승인 아니면 다음으로" 이분법뿐이다. 필터 체인처럼 여러 핸들러가 조금씩 손을 대고 계속 넘기는 경우는 더 복잡한 신호(수정된 요청 객체 등)가 필요하다

---

#### 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 요청을 처리할 후보가 여럿이고, **누가 처리할지 실행 시점에** 정해질 때
- 처리 단계(에스컬레이션)가 있고, 그 순서나 구성을 유연하게 바꾸거나 늘려야 할 때
- 요청을 보내는 쪽이 **최종 처리자가 누군지 몰라도** 되게 만들고 싶을 때

**안 쓴다**
- 처리자가 하나로 고정돼 있다면 그냥 그 객체를 부르면 된다 — 체인으로 감쌀 이유가 없다
- 등록된 대상 **전부**가 반응해야 한다면 [옵저버 패턴](observer.md)이다
- 단계가 절대 안 바뀌고 두세 개뿐이라면 if-else로 충분할 수 있다 — 체인으로 만드는 순간 클래스 수가 늘고, 흐름을 한 단계 건너 추적해야 한다

판단은 한 줄이다. **"이 요청을 누가 처리할지, 지금 코드가 아니라 실행 시점의 조립이 정하는가?"** 그렇다면 책임 연쇄다.
