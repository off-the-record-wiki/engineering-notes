# Querydsl - Querydsl

## 📚 JPQL vs Querydsl

```java
@Test
public void JPQLCode() {
	Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

	assertThat(result.getUsername()).isEqualTo("member1");
}

@Test
public void QuerydslCode() {

	JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 별도로 분리해서 쓰길 권장
	QMember m = new QMember("m");	// 별칭

	Member member = queryFactory
			.select(m)
			.from(m)
			.where(m.username.eq("member1"))
			.fetchOne();

	assertThat(member.getUsername()).isEqualTo("member1");
}
```

## 📚 검색 조건 쿼리

```java
@Test
public void search() {
	Member findMember = queryFactory
			.selectFrom(member)    // select()와 from() 합친 형태
			.where(member.username.eq("member1").and(member.age.eq(10)))
			.fetchOne();
	assertThat(findMember.getUsername()).isEqualTo("member1");
}
```

* `and()`, `or()` 등의 체이닝으로 검색 조건을 여러가지 지정할 수 있다.
* [JPQL 공식 문서](https://docs.oracle.com/html/E13946_04/ejb3_langref.html)

## 📚 결과 조회

* `fetch()` : 리스트 조회, 데이터 없으면 빈 리스트 반환
* `fetchOne()` : 단 건 조회, 결과가 없으면 null, 결과가 둘 이상이면 NonUniqueResultException 예외 발생
* `fetchFirst()` : limit(1).fetchOne()
* `fetchResults()` : 페이징 정보 포함, total count 쿼리 추가 실행
* `fetchCount()` : count 쿼리로 변경해서 count 수 조회

```java
@Test
public void resultFetch() {
        // 리스트 조회, 데이터 없으면 빈 리스트 반환
	List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();

        // 단 건 조회, 결과가 없으면 null, 결과가 둘 이상이면 NonUniqueResultException 예외 발생
	Member fetchOne = queryFactory
			.selectFrom(member)
			.fetchOne();

        // 페이징 정보 포함, total count 쿼리 추가 실행
	QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.fetchResults();

	results.getTotal();
	List<Member> content = results.getResults();

        // count 쿼리로 변경해서 count 수 조회
	long total = queryFactory
			.selectFrom(member)
			.fetchCount();
}
```

## 📚 정렬

> Example Code. 회원을 정렬하되 먼저 나이 순으로 내림차순, 이름 순으로 오름차순 정렬하기(단, 회원 이름이 없으면 마지막에 null)

```java
List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();
```

## 📚 페이징

```java
@Test
public void paging1() {
	List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();
}
```

## 📚 집합

```java
@Test
public void aggregation() {

	List<Tuple> result = queryFactory
			.select(member.count(),
					member.age.sum(),
					member.age.avg(),
					member.age.max(),
					member.age.min()
			)
			.from(member)
			.fetch();
}
```
```java
@Test
public void group() {
	List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())    // 팀 이름과 평균 연령
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)                        // 팀 이름 그룹화
			.fetch();
}
```

## 📚 조인 - 기본 조인

```java
@Test
public void join() {
	List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();
}
```

* `join()`, `innerJoin()`: 내부 조인
* `leftJoin()` : left 외부 조인
* `rightJoin()` : right 외부 조인

## 📚 조인 - ON절

```java
@Test
// Member를 조회하되 팀의 이름이 teamA인 Member들을 조인
public void join_on_filtering() {
	List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team)
			.on(team.name.eq("teamA"))
			.fetch();
}
```

* `join()`을 사용할 경우 내부 조인에 해당한다. 따라서 where절에서 필터링 하는 것과 동일하다.
* 조인 대상 필터링을 사용할 때, 내부조인이면 익숙한 where절로 해결하고 외부 조인을 할 때 필요한 경우에만 사용하는 것을 권장한다.

## 📚 조인 - 페치 조인

* 페치 조인은 SQL 조인을 활용해서 연관된 엔티티를 SQL 한 번에 조회하는 기능이다.

```java
@Test
public void fetchJoin() {
	em.flush();
	em.clear();

	Member findMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();

	boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
	assertThat(loaded).as("페치 조인 적용").isTrue();
}
```

## 📚 서브 쿼리

```java
@Test
public void subQuery() {

	QMember memberSub = new QMember("memberSub");

	// 바깥 루프와 안쪽 루프의 Member 타입은 달라야 한다.
	List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
			        JPAExpressions
				        .select(memberSub.age.max())
					.from(memberSub)
			))
			.fetch();
}
```

* JPA JPQL의 경우 from절에서 서브 쿼리를 작성할 수 없다. 이는 Querydsl도 마찬가지이다.

## 📚 Case문

```java
@Test
public void caseTest() {

        // 복잡한 경우
	queryFactory
		.select(new CaseBuilder()
			.when(member.age.between(0, 20)).then("0~20살")
			.when(member.age.between(21, 30)).then("21~30살")
			.otherwise("기타")
		)
		.from(member)
		.fetch();

        // 간단한 경우
	queryFactory
		.select(member.age
			.when(10).then("열살")
			.when(20).then("스무살")
			.otherwise("기타")
		)
		.from(member)
		.fetch();
}
```

## 📚 프로젝션과 결과 반환 - 기본

* 프로젝션(Projection) : SELECT 대상 지정
* 프로젝션 대상이 하나인 경우
  * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있다.

```java
@Test
public void singleProjection() {
	List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();
}
```

* 프로젝션 대상이 둘 이상인 경우
  * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회한다.

```java
@Test
// 튜플로 조회하는 경우
public void tupleProjection() {
	List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

	for (Tuple tuple : result) {
		String username = tuple.get(member.username);
		Integer age = tuple.get(member.age);
		System.out.println("username = " + username);
		System.out.println("age = " + age);
	}
}
```	
```java
@Test
// 순수 JPA의 프로젝션
public void dtoProjection() {
	List<MemberDto> results = em.createQuery("select new com.jwj.querydsl_2nd.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList();

}

@Test
// QueryDsl의 프로젝션
public void dtoProjectionByQuerydsl() {
	List<MemberDto> results = queryFactory
                        // 이 때, 생성자의 필드 순서에 주의
			.select(Projections.bean(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
}
```

## 📚 프로젝션과 결과 반환 - @QueryProjection

* 생성자 + `@QueryProjection`
  * DTO가 QueryDsl에 의존적이라는 단점이 발생한다.

```java
@Test
public void QueryProjection() {
	List<MemberDto> results = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();
}
```

## 📚 동적 쿼리 - BooleanBuilder

* BooleanBuilder를 사용해서 동적 쿼리를 작성할 수 있다.

```java
@Test
public void dynamicQueryByBooleanBuilder() {
	String usernameParam = "member1";
        Integer ageParam = 10;

	List<Member> members = searchMember1(usernameParam, ageParam);
	assertThat(members.size()).isEqualTo(1);
}

private List<Member> searchMember1(String usernameCond, Integer ageCond) {

	BooleanBuilder builder = new BooleanBuilder();
	if (usernameCond != null) {
		builder.and(member.username.eq(usernameCond));
	}

	if (ageCond != null) {
		builder.and(member.age.eq(ageCond));
	}

	return queryFactory
		.selectFrom(member)
		.where(builder)
		.fetch();
}
```

## 📚 동적 쿼리 - Where 다중 파라미터 사용

```java
@Test
public void dynamicQueryByWhereParam() {
	String usernameParam = "member1";
	Integer ageParam = 10;

	List<Member> members = searchMember2(usernameParam, ageParam);
	assertThat(members.size()).isEqualTo(1);
}

private List<Member> searchMember2(String usernameCond, Integer ageCond) {
	return queryFactory
		.selectFrom(member)
		.where(usernameEq(usernameCond), ageEq(ageCond))
		.fetch();
}

private Predicate ageEq(Integer ageCond) {
	if (ageCond != null) {
		return member.age.eq(ageCond);
	}
	return null;
}

private Predicate usernameEq(String usernameCond) {
	if (usernameCond != null) {
		return member.username.eq(usernameCond);
	}
	return null;
}
```

* where절에 여러 가지 넣는 것을 원하지 않는다면 BooleanExpression을 사용해서 한 번에 넣을 수 있다.
* 쿼리 자체의 가독성이 높아진다.

## 📚 수정, 삭제 벌크 연산

* 순수 JPA의 경우 `executeUpdate()`를 사용해서 대량의 데이터에 대한 연산 처리가 가능하다.
* Querydsl의 경우 `execute()`를 사용해서 대량의 데이터에 대한 연산 처리가 가능하다. 역시 반환되는 데이터는 변화된 데이터의 개수이다.
* JPA와 마찬가지로 영속성 컨텍스트를 무시하고 DB에 쿼리가 나가 바로 반영이 되기 때문에 안전하게 사용하려면 벌크 연산 후 영속성 컨텍스트를 반드시 초기화하도록 한다.

```java
@Test
public void bulk() {
	long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.eq(20))
			.execute();
}
```

## 📚 순수 JPA 리포지토리와 Querydsl

```java
@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

	private final EntityManager em;
	private final JPAQueryFactory queryFactory;

	public void save(Member member) {
		em.persist(member);
	}

	public Optional<Member> findById(Long id) {
		Member member = em.find(Member.class, id);
		return Optional.ofNullable(member);
	}

	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class)
				.getResultList();
	}

	public List<Member> findAll_Querydsl() {
		return queryFactory
				.selectFrom(member)
				.fetch();
	}

	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<Member> findByUsername_Querydsl(String username) {
		return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
	}
}
```

* JPAQueryFactory는 멀티 쓰레드 환경에서 안전하게 사용 가능하다.
* EntityManager를 생성자로 주입받아 사용하는데 Spring에서는 EntityManager가 프록시 객체로 주입되어 트랜잭션 스코프에 따라 실제 EntityManager를 생성/관리한다.
* 쿼리를 생성할 때마다 새로운 JPAQuery 인스턴스를 생성하기 때문에 여러 스레드에서 동시에 접근해도 안전하다.

## 📚 동적 쿼리와 성능 최적화 조회 - Builder 사용

```java
@Test
public void booleanBuilder() {
	MemberSearchCond memberSearchCond = new MemberSearchCond();
	memberSearchCond.setAgeGoe(35);
	memberSearchCond.setAgeLoe(40);
	memberSearchCond.setTeamName("teamB");

	List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(memberSearchCond);
}

// ...
// MemberJpaRepository 코드

public List<MemberTeamDto> searchByBuilder(MemberSearchCond memberSearchCond) {

	BooleanBuilder builder = new BooleanBuilder();
	if (StringUtils.hasText(memberSearchCond.getUsername())) {
		builder.and(member.username.eq(memberSearchCond.getUsername()));
	}

	if (StringUtils.hasText(memberSearchCond.getTeamName())) {
		builder.and(team.name.eq(memberSearchCond.getTeamName()));
	}

	if (memberSearchCond.getAgeGoe() != null) {
		builder.and(member.age.goe(memberSearchCond.getAgeGoe()));
	}

	if (memberSearchCond.getAgeLoe()!= null) {
                builder.and(member.age.loe(memberSearchCond.getAgeLoe()));
        }

	return queryFactory
			.select(new QMemberTeamDto(
					member.id,
					member.username,
					member.age,
					team.id.as("team_id"),
					team.name.as("teamName")))
			.from(member)
			.leftJoin(member.team, team)
			.fetch();
}
```


## 📚 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용

```java
@Test
public void whereParam() {
	MemberSearchCond memberSearchCond = new MemberSearchCond();
	memberSearchCond.setAgeGoe(35);
	memberSearchCond.setAgeLoe(40);
	memberSearchCond.setTeamName("teamB");

	List<MemberTeamDto> search = memberJpaRepository.search(memberSearchCond);
}

// ...
// MemberJpaRepository 코드

public List<MemberTeamDto> search(MemberSearchCond memberSearchCond) {
	return queryFactory
		.select(new QMemberTeamDto(
				member.id,
				member.username,
				member.age,
				team.id.as("team_id"),
				team.name.as("teamName")))
		.from(member)
		.leftJoin(member.team, team)
		.where(
			usernameEq(memberSearchCond.getUsername()),
			teamNameEq(memberSearchCond.getTeamName()),
			ageGoe(memberSearchCond.getAgeGoe()),
			ageLoe(memberSearchCond.getAgeLoe())
		)
                .fetch();
        }

	private Predicate ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}

	private Predicate ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private Predicate teamNameEq(String teamName) {
		return hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private Predicate usernameEq(String username) {
		return hasText(username) ? member.username.eq(username) : null;
	}
```

* Predicate 대신 BooleanExpression 반환 타입으로 바꾸면 조합이 가능하다.

## 📚 스프링 데이터 페이징 활용1 - Querydsl 페이징 연동

```java
@Override
public Page<MemberTeamDto> searchPageSimple(MemberSearchCond memberSearchCond, Pageable pageable) {
	QueryResults<MemberTeamDto> results = queryFactory
			.select(new QMemberTeamDto(member.id.as("member_id"),
					member.username,
					member.age,
					member.team.id.as("team_id"),
					member.team.name.as("teamName")))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(memberSearchCond.getUsername()),
				teamNameEq(memberSearchCond.getTeamName()),
				ageGoe(memberSearchCond.getAgeGoe()),
				ageLoe(memberSearchCond.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetchResults();

	List<MemberTeamDto> result = results.getResults();
	long total = results.getTotal();

	return new PageImpl<>(result, pageable, total);
}
```

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCond memberSearchCond, Pageable pageable) {
	List<MemberTeamDto> results = queryFactory
			.select(new QMemberTeamDto(member.id.as("member_id"),
					member.username,
					member.age,
					member.team.id.as("team_id"),
					member.team.name.as("teamName")))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(memberSearchCond.getUsername()),
				teamNameEq(memberSearchCond.getTeamName()),
				ageGoe(memberSearchCond.getAgeGoe()),
				ageLoe(memberSearchCond.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

	// 페이지 카운트를 별도로 분리
	// 카운트 쿼리 최적화
	long count = queryFactory
			.selectFrom(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(memberSearchCond.getUsername()),
				teamNameEq(memberSearchCond.getTeamName()),
				ageGoe(memberSearchCond.getAgeGoe()),
				ageLoe(memberSearchCond.getAgeLoe())
			)
			.fetchCount();

	return new PageImpl<>(results, pageable, count);
}
```

## 📚 스프링 데이터 페이징 활용2 - CountQuery 최적화

* `PageableExecutionUtils.getPage()`로 최적화한다.
* 스프링 데이터 라이브러리가 제공하며 count 쿼리가 생략 가능한 경우 생략해서 처리한다.
  * 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
  * 마지막 페이지일 때(offset + 컨텐츠 사이즈를 더해서 전체 사이즈를 구함)

```java
@Override
public Page<MemberTeamDto> searchPageComplex(MemberSearchCond memberSearchCond, Pageable pageable) {
	List<MemberTeamDto> results = queryFactory
			.select(new QMemberTeamDto(member.id.as("member_id"),
					member.username,
					member.age,
					member.team.id.as("team_id"),
					member.team.name.as("teamName")))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(memberSearchCond.getUsername()),
				teamNameEq(memberSearchCond.getTeamName()),
				ageGoe(memberSearchCond.getAgeGoe()),
				ageLoe(memberSearchCond.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

	// Count Query 최적화
	JPAQuery<Member> countQuery = queryFactory
			.selectFrom(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(memberSearchCond.getUsername()),
				teamNameEq(memberSearchCond.getTeamName()),
				ageGoe(memberSearchCond.getAgeGoe()),
				ageLoe(memberSearchCond.getAgeLoe())
			);

	return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchCount);
}
```

## 📚 인터페이스 지원 - QuerydslPredicateExecutor

* 조인이 불가능하다.
* 복잡한 환경에서의 사용이 어렵다.
* Querydsl에 의존해야 한다.
* [QuerydslPredicateExecutor](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html#core.extensions.querydsl)

```java
@Test
void querydslPredicateExecutorTest() {

	QMember member = QMember.member;
	Iterable<Member> result = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));

	for (Member m : result) {
		System.out.println("m = " + m);
	}
}
```

## 📚 Querydsl Web 지원

* 단순 조건만 가능하다.
* 조건을 커스텀하는 기능이 복잡하고 명시적이지 않다.
* 컨트롤러가 Querydsl에 의존해야 한다.
* 복잡한 환경에서의 사용이 어렵다.
* [Querydsl Web Support](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html#core.web.basic)

## 📚 Querydsl 지원 클래스 직접 만들기

* 스프링 데이터가 제공하는 `QuerydslRepositorySupport`가 지닌 한계를 극복하기 위해 직접 개발해서 사용할 수 있다. 
* 스프링 데이터가 제공하는 페이징을 편리하게 반환한다.
* 페이징과 카운트 쿼리 분리가 가능하다.
* 스프링 데이터 Sort 지원도 가능하다.
* `select()` or `selectFrom()`으로 시작 가능하다.
* `EntityManager`, `JPAQueryFactory 제공이 가능하다.
* [QuerydslRepositorySupport](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/support/QuerydslRepositorySupport.html) → 각 메서드별 내용은 여기서 확인 가능하다.

```java
@Repository
public abstract class Querydsl4RepositorySupport {

	private final Class domainClass;
	private Querydsl querydsl;
	private EntityManager entityManager;
	private JPAQueryFactory queryFactory;

	public Querydsl4RepositorySupport(Class<?> domainClass) {
		Assert.notNull(domainClass, "Domain class must not be null!");
		this.domainClass = domainClass;
	}

	@Autowired
	public void setEntityManager(EntityManager entityManager) {
		Assert.notNull(entityManager, "EntityManager must not be null!");
		JpaEntityInformation entityInformation =
				JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
		SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
		EntityPath path = resolver.createPath(entityInformation.getJavaType());
		this.entityManager = entityManager;
		this.querydsl = new Querydsl(entityManager, new
				PathBuilder<>(path.getType(), path.getMetadata()));
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@PostConstruct
	public void validate() {
		Assert.notNull(entityManager, "EntityManager must not be null!");
		Assert.notNull(querydsl, "Querydsl must not be null!");
		Assert.notNull(queryFactory, "QueryFactory must not be null!");
	}

	protected JPAQueryFactory getQueryFactory() {
		return queryFactory;
	}

	protected Querydsl getQuerydsl() {
		return querydsl;
	}

	protected EntityManager getEntityManager() {
		return entityManager;
	}

	protected <T> JPAQuery<T> select(Expression<T> expr) {
		return getQueryFactory().select(expr);
	}

	protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
		return getQueryFactory().selectFrom(from);
	}

	protected <T> Page<T> applyPagination(Pageable pageable,
										  Function<JPAQueryFactory, JPAQuery> contentQuery) {
		JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
		List<T> content = getQuerydsl().applyPagination(pageable,
				jpaQuery).fetch();
		return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
	}

	protected <T> Page<T> applyPagination(Pageable pageable,
										  Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory,
			JPAQuery> countQuery) {
		JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
		List<T> content = getQuerydsl().applyPagination(pageable,
				jpaContentQuery).fetch();

		JPAQuery countResult = countQuery.apply(getQueryFactory());
		return PageableExecutionUtils.getPage(content, pageable,
				countResult::fetchCount);
	}
}
```
