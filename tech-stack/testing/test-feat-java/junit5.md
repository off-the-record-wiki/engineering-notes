# Test - JUnit5

#### JUnit5 조건에 따라 테스트 실행하기

* `assumeTrue` : 특정 조건을 만족하는 경우에 테스트를 실행하는 방법
* `assumeThat` : 특정 조건을 만족하는 경우에 테스트를 실행하는 방법
* `@Enable__` & `@Disabled__` : `OnOS`, `OnJre`, `IfSystemProperty`, `IfEnvironmentVariable`, `If` 

#### JUnit5 태깅과 필터링

* `@Tag` : 테스트 메서드에 태그를 추가해 테스트 그룹을 만들어 원하는 테스트 그룹만을 실행할 수 있는 방법

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/44f30f09-926e-469f-8ea1-36536299bf3e.png)

#### JUnit5 커스텀 태그

* JUnit5 어노테이션을 조합해 커스텀 태그를 만들 수 있다.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("fast")
public @interface CustomAnnotation {
}
```

#### JUnit5 테스트 반복

* `@RepeatedTest` : 테스트 반복 실행. 같은 테스트 메서드를 지정한 횟수만큼 반복 실행해 동시성 버그, 불안정한 상태와 같은 비결정적 이슈를 찾기 위해 사용한다.

```kotlin
@RepeatedTest(10)
fun `좋아요를 동시에 누르면 정확히 증가한다`(repetitionInfo: RepetitionInfo) {
    // 반복 i번째임을 알 수 있다
    println("반복: ${repetitionInfo.currentRepetition}/${repetitionInfo.totalRepetitions}")
    
    val articleId = 1L
    val userId = UUID.randomUUID().toString()
    
    // 같은 사용자가 같은 글을 10번 반복 좋아요 시도
    repeat(10) {
        likeService.toggleLike(articleId, userId)
    }
    
    // 중복 방지 로직이 정상이면 좋아요는 정확히 1개여야 한다
    val likeCount = likeRepository.countByArticleId(articleId)
    assertThat(likeCount).isEqualTo(1)
}
```

* `@ParameterizedTest` : 여러 입력값으로 테스트. 하나의 테스트 메서드를 다양한 입력값으로 여러 번 실행해 경계값, 에외 케이스를 한 번에 검증할 때 사용한다.

```kotlin
@ParameterizedTest(name = "[{index}] 제목=\"{0}\" → {1}")
@CsvSource(
    value = [
        "'',                      false",  // 공백
        "'a',                     true",   // 1글자 (최소)
        "'12345678901234567890',  true",   // 20글자 (최대)
        "'123456789012345678901', false",  // 21글자 (초과)
    ]
)
fun `게시물 제목 길이 검증`(title: String, expectedValid: Boolean) {
    val request = ArticleCreateRequest(
        title = title,
        content = "본문",
        boardName = "일반"
    )
    
    val isValid = articleValidator.isValidTitle(title)
    assertThat(isValid).isEqualTo(expectedValid)
}
```

* `@ValueSource` : 단일 값 배열. 하나의 입력값 배열을 제공한다.

```kotlin
@ParameterizedTest
@ValueSource(strings = ["공지", "자유", "질문", "후기"])
fun `유효한 게시판명만 허용한다`(boardName: String) {
    val board = boardService.getBoard(boardName)
    assertThat(board).isNotNull()
    assertThat(board.name).isEqualTo(boardName)
}

// 다른 타입들
@ParameterizedTest
@ValueSource(ints = [1, 10, 100])
fun `양수 페이지 번호만 허용`(pageNumber: Int) {
    assertThatNoException().isThrownBy {
        articleService.getArticles(pageNumber)
    }
}
```

* `@NullSource` : `null` 하나. null을 유일한 인자로 제공해 `null` 처리 테스트에 특화되어있다.

```kotlin
@ParameterizedTest
@NullSource
fun `null 제목은 거부한다`(title: String?) {
    val request = ArticleCreateRequest(
        title = title,
        content = "본문",
        boardName = "일반"
    )
    
    assertThatThrownBy {
        articleService.create(request)
    }.isInstanceOf(IllegalArgumentException::class.java)
}
```

* `@EmptySource` : 빈 값. 컬렉션·문자열의 빈 상태(""·emptyList() 등)를 제공한다.

```kotlin
@ParameterizedTest
@EmptySource
fun `빈 제목은 거부한다`(title: String) {
    // title = ""
    assertThat(title).isEmpty()
    
    assertThatThrownBy {
        articleValidator.validateTitle(title)
    }.isInstanceOf(IllegalArgumentException::class.java)
}

@ParameterizedTest
@EmptySource
fun `빈 검색어는 전체 목록 반환`(keyword: List<String>) {
    // keyword = emptyList()
    val results = searchService.search(keyword)
    assertThat(results).hasSize(totalArticleCount)
}
```

* `@NullAndEmptySource` : `null` + 빈 값 조합. `@NullSource` + `@EmptySource`를 합친 것으로 `null`과 빈 값 둘 다 한 번에 테스트할 수 있다.

```kotlin
@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = ["   ", "\t", "\n"])  // null, "", 공백들
fun `유효하지 않은 본문은 거부한다`(content: String?) {
    // content = null, "", "   ", "\t", "\n" 각각 테스트됨
    assertThatThrownBy {
        articleValidator.validateContent(content)
    }.isInstanceOf(IllegalArgumentException::class.java)
}
```

* `@EnumSource` : Enum 상수들. Enum의 모든 상수(또는 특정 상수들)를 자동으로 제공한다.

```kotlin
enum class SortOrder {
    LATEST, OLDEST, POPULAR, SCORE
}

@ParameterizedTest
@EnumSource(SortOrder::class)
fun `모든 정렬 방식이 동작한다`(sortOrder: SortOrder) {
    val articles = articleService.getArticles(sortOrder)
    assertThat(articles).isNotEmpty()
    
    // 정렬 순서 검증
    when (sortOrder) {
        SortOrder.LATEST -> assertThat(articles)
            .isSortedAccordingTo { a, b -> b.createdAt.compareTo(a.createdAt) }
        SortOrder.POPULAR -> assertThat(articles)
            .isSortedAccordingTo { a, b -> b.likeCount.compareTo(a.likeCount) }
        else -> {}
    }
}

// 특정 Enum만 테스트
@ParameterizedTest
@EnumSource(SortOrder::class, names = ["LATEST", "POPULAR"])
fun `주요 정렬 방식 두 가지만 테스트`(sortOrder: SortOrder) {
    // LATEST, POPULAR만 실행됨
}
```

* `@MethodSource` : 메서드에서 생성. 정적 팩토리 메서드에서 복잡한 인자 조합을 생성한다. 여러 인자 + 복잡한 객체 테스트에 적합하다.

```kotlin
@ParameterizedTest(name = "[{index}] 제목={0}, 기대점수={1}")
@MethodSource("provideArticlesWithExpectedScores")
fun `인기글 점수 산식이 정확하다`(article: Article, expectedScore: Int) {
    val score = rankingService.calculateScore(article)
    assertThat(score).isEqualTo(expectedScore)
}

// 팩토리 메서드 (companion object 안에)
companion object {
    @JvmStatic
    fun provideArticlesWithExpectedScores() = listOf(
        Arguments.of(
            Article(title = "대박글", likes = 100, comments = 50, views = 1000),
            100 * 3 + 50 * 2 + 1000 * 1  // 좋아요×3 + 댓글×2 + 조회×1
        ),
        Arguments.of(
            Article(title = "보통글", likes = 10, comments = 5, views = 100),
            10 * 3 + 5 * 2 + 100 * 1
        ),
    )
}

// 다중 인자 예제
@ParameterizedTest
@MethodSource("providePageNumbers")
fun `페이지 범위 검증`(pageNum: Int, expectedValid: Boolean) {
    val isValid = articleService.isValidPageNumber(pageNum)
    assertThat(isValid).isEqualTo(expectedValid)
}

companion object {
    @JvmStatic
    fun providePageNumbers() = listOf(
        Arguments.of(-1, false),
        Arguments.of(0, false),
        Arguments.of(1, true),
        Arguments.of(9999, true),
        Arguments.of(10000, false),  // 최대 페이지 초과
    )
}
```

* `@CsvSource` : CSV 문자열. 쉼표 구분 문자열로 인자를 제공하며 간단한 표 형식의 테스트에 최적화되어있다.

```kotlin
@ParameterizedTest(name = "[{index}] 제목 길이={0}, 유효={1}")
@CsvSource(
    value = [
        ",                     false",  // null → 공백으로 해석
        "'',                   false",  // 빈 문자열
        "'a',                  true",   // 1글자
        "'안녕하세요 반갑습니다', true",  // 10글자
        "'12345678901234567890', false",  // 21글자 (최대 초과)
    ],
    nullValues = ["<NULL>"]  // <NULL>을 null로 해석
)
fun `제목 길이 검증`(title: String?, expectedValid: Boolean) {
    val isValid = articleValidator.isValidTitle(title)
    assertThat(isValid).isEqualTo(expectedValid)
}

// 다중 열 예제: (게시판, 예상 권한)
@ParameterizedTest
@CsvSource(
    "공지, ADMIN",
    "자유, USER",
    "후기, USER"
)
fun `게시판별 작성 권한`(boardName: String, expectedRole: String) {
    val board = boardService.getBoard(boardName)
    assertThat(board.requiredRole).isEqualTo(expectedRole)
}
```

* `@CsvFileSource` : CSV 파일. 외부 CSV 파일에서 데이터를 읽는다. 대량의 테스트 데이터가 필요할 때 정리된 파일로 관리한다.

```csv
title,likes,comments,views,expectedScore
"대박글",100,50,1000,500
"보통글",10,5,100,50
"신입글",1,0,10,3
```
```kotlin
@ParameterizedTest
@CsvFileSource(resources = "/test-articles.csv", numLinesToSkip = 1)
fun `CSV 파일로부터 인기글 점수 검증`(
    title: String,
    likes: Int,
    comments: Int,
    views: Int,
    expectedScore: Int
) {
    val article = Article(
        title = title,
        likes = likes,
        comments = comments,
        views = views
    )
    
    val score = rankingService.calculateScore(article)
    assertThat(score).isEqualTo(expectedScore)
}

// 로컬 파일시스템도 가능
@ParameterizedTest
@CsvFileSource(
    files = ["src/test/resources/articles.csv"],
    numLinesToSkip = 1
)
fun `로컬 파일 CSV 로드`(title: String, boardName: String) {
    assertThatNoException().isThrownBy {
        articleService.create(title, boardName)
    }
}
```

* `@ArgumentSource` : 커스텀 ArgumentsProvider. 직접 구현한 ArgumentsProvider로 복잡한 로직을 담을 수 있다.

```kotlin
// 커스텀 ArgumentsProvider
class RealisticArticleProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return (1..100).map { i ->
            Arguments.of(
                Article(
                    title = "테스트 제목 #$i",
                    likes = i * 10,
                    comments = i * 5,
                    views = i * 100,
                    createdAt = LocalDateTime.now().minusDays(i.toLong())
                ),
                i * (10 * 3 + 5 * 2 + 100 * 1)  // 동적 점수 계산
            )
        }.stream()
    }
}

@ParameterizedTest
@ArgumentsSource(RealisticArticleProvider::class)
fun `100개의 현실적 데이터로 랭킹 검증`(article: Article, expectedScore: Int) {
    val score = rankingService.calculateScore(article)
    assertThat(score).isEqualTo(expectedScore)
}

// 또는 데이터를 DB에서 동적으로 로드
class DatabaseArticleProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        // DB에서 모든 테스트 데이터 로드
        return articleRepository.findAll()
            .map { article ->
                Arguments.of(
                    article,
                    calculateExpectedScore(article)
                )
            }
            .stream()
    }
    
    private fun calculateExpectedScore(article: Article): Int {
        return article.likes * 3 + article.comments * 2 + article.views
    }
}
```

#### JUnit5 테스트 인스턴스

* JUnit은 테스트 메서드마다 테스트 인스턴스를 새로 만든다.
* 테스트 메서드를 독립적으로 실행해 예상치 못한 부작용을 막기 위함이다. 이런 기본 전략을 JUnit5에서 변경할 수 있다.
* `@TestInstance(LifeCycle.PER_CLASS)`
  * 테스트 클래스당 인스턴스를 하나만 만들어 사용한다.
  * 필요에 따라 테스트 간 공유하는 모든 상태를 @BeforeEach 또는 @AfterEach에서 초기화 할 필요가 있다.
  * 테스트 A가 테스트 B의 실행에 영향을 미치면 안되기 때문에 이런 것과 무관한 경우의 테스트라면 한 번쯤은 고민해 볼 필요가 있어보인다.

```java
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
```

#### JUnit5 테스트 순서

* 경우에 따라 특정 순서대로 테스트를 실행하고픈 순간이 온다. 이런 경우에는 테스트 메서드를 원하는 순서에 따라 실행하도록 `@TestInstance(LifeCycle.PER_CLASS)`와 함께 `@TestMethodOrder`를 사용할 수 있다.
* `MethodOrderer` 구현체를 설정한다. -> 기본 구현체 : `Alphanumeric`, `OrderAnnotation`, `Random`
* `@TestInstance` : 테스트 인스턴스 생명주기 제어. 테스트 클래스의 인스턴스를 언제 생성하고 재사용할지를 결정한다.

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArticleRepositoryTest {
    private lateinit var connection: Connection
    
    // @BeforeAll은 보통 static이어야 하지만,
    // PER_CLASS면 non-static @BeforeAll 사용 가능!
    @BeforeAll
    fun setupDatabase() {
        // 무거운 DB 초기화를 한 번만
        println("DB 연결 생성 (1회만)")
        connection = DriverManager.getConnection("jdbc:mysql://localhost/test")
    }
    
    @Test
    fun `게시물 저장 테스트`() {
        val article = Article(title = "테스트", content = "본문")
        articleRepository.save(article)
        assertThat(articleRepository.count()).isEqualTo(1)
    }
    
    @Test
    fun `게시물 조회 테스트`() {
        val articles = articleRepository.findAll()
        assertThat(articles).hasSize(1)  // 이전 테스트에서 저장한 데이터 있음
    }
    
    @AfterAll
    fun cleanupDatabase() {
        println("DB 정리 (1회만)")
        connection.close()
    }
}
```

#### JUnit5 - `junit-platform.properties`

* JUnit5 설정 파일로 클래스패스 루트(`/src/test/resources/`)에 넣어두면 적용이 된다.
* 테스트 인스턴스 라이프사이클 설정 : `junit.jupiter.testinstance.lifecycle.default = per_class`
* 확장팩 자동 감지 기능 설정 : `junit.jupiter.extensions.autodetection.enabled = true`
* `@Disabled` 무시하고 진행하는 설정 : `junit.jupiter.conditions.deactivate = org.junit.*DisabledCondition`
* 테스트 이름 표기 전략 설정 : `junit.jupiter.displayname.generator.default = \org.junit.jupiter.api.DisplayNameGenerator$ReplaceUnderscores`
