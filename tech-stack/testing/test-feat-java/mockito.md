# Test - Mockito

#### [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/module-summary.html)

* `Mock` : 진짜 객체와 비슷하게 동작하나 프로그래머가 직접 그 객체 행동을 관리하는 객체
* `Mockito` : Mock 객체를 쉽게 만들고 관리하고 검증할 수 있는 방법을 제공한다.

#### Mock 객체 만들기

* 인터페이스를 구현한 구현체 클래스가 없다면 테스트가 어렵다.
* 이와 같은 상황에서 Mock 객체를 만들 수 있다.

```java
@ExtendWith(MockitoExtension.class)            // (1). Mock 어노테이션을 처리해줄 Extension을 추가해야함
class StudyServiceTest {

    @Mock
    protected StudyRepository studyRepository; // (2). StudyRepository 타입의 가짜 객체

    @Mock
    protected MemberService memberService;     // (3). MemberService 타입의 가짜 객체 

    @Test
    void createStudyService() {

    StudyService studyService = new StudyService(memberService, studyRepository);
    assertNotNull(studyService);
    }
}
```

#### Mock 객체 Stubbing

* 모든 Mock 객체는 `null` 혹은 `Optional.empty`를 리턴한다.
* `Mock` 객체를 조작해서 특정한 매개변수를 받은 경우 특정 값을 리턴하거나 예외를 던지도록 만들 수 있다.
* `Mock` 객체를 조작해서 Void 메서드 특정 매개변수를 받거나 호출된 경우 예외를 발생시킬 수 있다.
* `Mock` 객체를 조작해서 메서드가 동일한 매개변수로 여러번 호출될 때 각기 다르게 행동하도록 조작할 수 있다.

```java
@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    protected StudyRepository studyRepository;
    
    @Mock
    protected MemberService memberService;

    @Test
    void createStudyService() {

	Member member = new Member();
	member.setId(1L);
	member.setEmail("dnwls4659@gmail.com");

        // Stubbing : Mockito를 사용해 특정 메서드 호출에 대해 미리 정의된 값을 반환하도록 설정한 작업
	when(memberService.findById(1L)).thenReturn(Optional.of(member));

	StudyService studyService = new StudyService(memberService, studyRepository);
	Study study = new Study(10, "Java");
	studyService.createNewStudy(1L, study);

	Optional<Member> findById = memberService.findById(1L);
	assertEquals("dnwls4659@gmail.com", findById.get().getEmail());
    }

    @Test
    void practice() {

    // given
    Member member = new Member();
    member.setId(1L);
    member.setEmail("dnwls4659@gmail.com");

    Study study = new Study(10, "테스트");

    // when
    // TODO) memberService 객체에 findById 메서드를 1L 값으로 호출하면 Optional.of(member) 객체를 리턴하도록 Stubbing
    // TODO) studyRepository 객체에 save 메서드를 호출하면 study 객체를 리턴하도록 Stubbing
    when(memberService.findById(1L)).thenReturn(Optional.of(member));
    when(studyRepository.save(study)).thenReturn(study);

    StudyService studyService = new StudyService(memberService, studyRepository);

    studyService.createNewStudy(1L, study);

    // then
    assertNotNull(study.getOwnerId());
    assertEquals(member.getId(), study.getOwnerId());
    }
}
```

#### [Mock 객체 확인 - `Verify()`](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/verification/package-summary.html)

* `Mock` 객체가 어떻게 사용이 되는지를 확인할 수 있다.
* 특정 메서드가 특정 매개변수를 받아서 몇 번 호출되었는지, 최소 한 번은 호출됐는지, 아니면 전혀 호출되지 않았는지를 알 수 있다.

#### [BDD 스타일 Mockito API](https://javadoc.io/static/org.mockito/mockito-core/3.2.0/org/mockito/BDDMockito.html)

* `BDD` : 애플리케이션이 어떻게 행동해야하는지에 대한 공통된 이해를 구성하는 방법으로, TDD에서 창안된 방법이다.
  * `Given` : 어떤 상황이 주어졌을 때...
  * `When` : 뭔가를 하면...
  * `Then` : 이럴 것이다...

```java
// Given -> When : Given이 만족되었을 때, When을 실행한다.
given(memberService.findById(1L)).willReturn(Optional.of(member)); // memberService의 findById(1L) 메서드가 호출되면, Optional.of(member) 객체를 반환하도록 설정한다
given(studyRepository.save(study)).willReturn(study); // studyRepository의 save(study) 메서드가 호출되면, study 객체를 그대로 반환하도록 설정한다.
```

```java
// Then -> Verify : Then 단계에서 Verify 한다.
then(memberService).should(times(1)).notify(study); // memberService의 notify(study) 메서드가 정확히 1번 호출됐는지 검증한다.
then(memberService).shouldHaveNoMoreInteractions(); // memberService에 대해 더 이상 아무런 상호작용(메서드 호출)이 없었는지 검증한다.
```
