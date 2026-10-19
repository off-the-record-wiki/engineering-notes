# Clean Spring - Domain-Driven Development with Design Patterns

#### [도메인 모델 패턴](https://en.wikipedia.org/wiki/Domain_model)

* 도메인/비즈니스 로직을 구성하는 아키텍처 패턴의 한 가지
* 도메인 모델의 속성과 행위를 모두 포함하는 도메인의 오브젝트 모델이다.
* 오브젝트 모델이기 때문에 복잡한 연관관계, 커스텀 속성, 상속 등을 사용할 수 있다.
* 트랜잭션 스크립트는 하나의 업무절차(TX)를 처리하기 위한 스크립트(메서드)를 만들고 비즈니스 로직을 순서대로 코드로 작성하는 방법이다.

#### 널 안전성과 도메인 불변식 — Objects.requireNonNull을 쓰는 이유

* Java 언어에는 언어 차원의 널 안전성이 없다.
* Kotlin은 `String`과 `String?`을 타입 시스템으로 구분해 컴파일 시점에 널 접근을 차단한다.
* Java에는 이런 장치가 없어서 어떤 참조든 `null`일 수 있고, 컴파일러는 아무 말도 하지 않는다.
* `Objects.requireNonNull`은 이 공백을 런타임 검사로 보완하는 도구다. `null`이면 그 자리에서 즉시 `NullPointerException`을 던지는 한 줄짜리 유틸 메서드일 뿐, 컴파일 타임 안전성을 제공하는 것이 아니다.

> **이유 1 : Fail-fast — 터지는 위치를 원인 지점으로 당겨온다**
>
> * 검사 없이 null을 필드에 저장하면, NPE는 한참 뒤 전혀 다른 곳에서 터진다.
> * 원인(null 대입)과 증상(NPE)의 거리가 멀수록 디버깅이 어렵다.
> * `requireNonNull`은 잘못된 값이 들어오는 바로 그 순간 터뜨려서 원인과 증상의 거리를 0으로 만든다.
> * 두 번째 인자로 메시지를 주면 원인이 예외 메시지에 바로 드러난다.

> **이유 2 : 불변식(invariant)을 생성 시점에 강제한다**
> * 관문에서 `requireNonNull`을 걸면 "닉네임 없는 Member는 아예 존재할 수 없다"가 보장된다.
> * Member를 사용하는 모든 코드에서 `nickname == null` 방어 검사가 필요 없어진다.
> * 유효하지 않은 상태의 도메인 객체는 만들어질 수 없게 하라의 가장 소박한 구현이다.

> **참고 : Bean Validation(@NotNull)과의 차이**
> * 도메인 모델이 프레임워크에 기대지 않고 스스로를 지킨다는 점에서 헥사고날 아키텍처의 도메인 계층에는 `requireNotNull` 방식이 적합하다.
> * Bean Validation은 요청 DTO 등 어댑터 경계에서 입력을 거르는 용도로 역할이 나뉜다.

```java
@Entity
@Getter
@ToString(callSuper = true, exclude = "detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends AbstractEntity {

    @Embedded
    @NaturalId  // 기술적 PK(id)와 별개로, 비즈니스 관점에서 이 엔티티를 유일하게 식별하는 키
    @AttributeOverride(name = "address", column = @Column(name = "email", unique = true))
    private Email email;

    private String nickname;

    private String passwordHash;

    private MemberStatus status;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private MemberDetail detail;

    public static Member register(MemberRegisterRequest createRequest, PasswordEncoder passwordEncoder) {
        Member member = new Member();

        member.email = new Email(createRequest.email());
        member.nickname = Objects.requireNonNull(createRequest.nickname());
        member.passwordHash = Objects.requireNonNull(passwordEncoder.encode(createRequest.password()));

        member.status = MemberStatus.PENDING;

        member.detail = MemberDetail.create();

        return member;
    }

    public void activate() {
        Assert.state(status == MemberStatus.PENDING, "PENDING 상태가 아닙니다");
        this.status = MemberStatus.ACTIVE;
        this.detail.activate();
    }

    public void deactivate() {
        Assert.state(status == MemberStatus.ACTIVE, "ACTIVE 상태가 아닙니다");
        this.status = MemberStatus.DEACTIVATED;
        this.detail.deactivate();
    }

    public boolean verifyPassword(String password, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(password, this.passwordHash);
    }

    public void updateInfo(MemberInfoUpdateRequest updateRequest) {
        Assert.state(getStatus() == MemberStatus.ACTIVE, "등록 완료 상태가 아니면 정보를 수정할 수 없습니다");
        this.nickname = Objects.requireNonNull(updateRequest.nickname());
        this.detail.updateInfo(updateRequest);
    }

    public void changePassword(String password, PasswordEncoder passwordEncoder) {
        this.passwordHash = passwordEncoder.encode(Objects.requireNonNull(password));
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
```

#### [값 객체(VO)](https://en.wikipedia.org/wiki/Value_object)

* 도메인 모델에서 식별자가 필요하지 않고 속성/값으로만 구별되는 오브젝트
* 엔티티가 너무 많은 책임을 가지는 것을 방지하고 특정 속성 관련 행위를 분리해서 엔티티를 더 집중된 상태로 유지하게 한다.
* 원시 타입보다는 도메인 개념을 더 명시적으로 나타내서 모델의 명확성을 높인다.
* 생성 이후에 상태가 변하지 않고 변경이 필요하면 새로운 객체로 교체한다.
* 풍부한 기능을 가지며 자체 유효성 검사도 가능해진다.
