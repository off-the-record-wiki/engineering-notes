# Spring AI - RAG Introduction

#### Vector Database

* Vector Database는 일반적인 텍스트, 이미지, 오디오 등의 비정형 데이터를 AI가 이해할 수 있는 '숫자들의 배열(고차원 벡터)'로 변환하여 저장하는 곳이다.
* 전통적인 RDB(키워드 매칭)
  * 정확히 일치하는 단어가 있어야만 찾을 수 있다.
* Vector DB(의미적 유사성 검색 - Semantic Search)
  * 단어가 달라도 문맥과 의미가 비슷하면 찾아낸다.

#### Similarity

* AI가 텍스트를 숫자로 된 좌표(Vector)로 바꿨다면, 이제 "어떤 단어들이 서로 비슷한 의미를 가질까?"를 수학적으로 계산할 수 있게 된다.
* 두 벡터 사이의 각도 세타

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e49ad399-b199-42eb-bd1f-c883407a2d81.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/81c8a918-49c5-495f-9eb1-18e56f877825.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c96ecca3-1023-4435-a23d-a650b664ef68.png)

#### Cosine Similarity(코사인 유사도)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7f6b57cf-e3ab-4135-bed7-12f3e02906cd.png)

* 수학적으로 계산하기 위해 제2 코사인 법칙(Law of Cosines)을 활용한 코사인 유사도 공식이 사용된다.
* 백엔드 성능을 고려할 때, 이 공식은 수백 ~ 수천 차원의 엄청난 고차원 데이터 공간에서도 연산이 아주 빠르고 정확하게 잘 동작하기 때문에 LLM 생태계에서 가장 표준적으로 사용된다.

#### Spring AI의 Similarity Score

* 이런 수학적 배경을 바탕으로 Spring AI 프레임워크는 백엔드 개발자들이 복잡한 수학 공식 없이도 유사도를 쉽게 다룰 수 있도록 수치화된 Similarity Score(유사도 점수)를 제공한다.
* 값의 범위는 0.0 ~ 1.0으로 점수가 1에 가까울수록 완벽하게 똑같거나 매우 높은 의미적 유사성을 가진다는 뜻. 반대로 0에 가까울수록 전혀 관련 없는 데이터로 판별한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ec16eb9f-796a-4b8e-b8ce-45c678a801da.png)

* 임베딩 모델을 사용해 벡터값을 계산할 수 있다.
* Spring AI는 개발자가 다양한 Vector Database를 통일된 코드로 쉽게 다룰 수 있도록 VectorStore라는 공통 인터페이스(구현 클래스)를 제공한다.
* 다국어 처리도 가능하다.

#### Spring AI Vector Store API

* Spring AI Vector Store API는 '벤더 종속성' 문제를 완벽하게 해결해주는 일관된 추상화 인터페이스이다.
* Spring AI는 현재 실무에서 쓰이는 거의 모든 벡터 DB를 공식 지원한다.
* 모든 벡터 DB 구현체가 공통으로 상속받는 최상위 인터페이스로 저장 단위는 `Document`이다.
  * `Document` 객체를 만들어 Vector Store에 넣으면 Spring AI가 알아서 임베딩 모델을 호출, 텍스트를 숫자로 변환한 뒤, 기존 Document + 임베딩 벡터 형태를 하나로 묶어 DB에 저장한다.  

```java
public interface VectorStore extends DocumentWriter, VectorStoreRetriever {

	default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Adds list of {@link Document}s to the vector store.
	 * @param documents the list of documents to store. Throws an exception if the
	 * underlying provider checks for duplicate IDs.
	 */
	void add(List<Document> documents);

	@Override
	default void accept(List<Document> documents) {
		add(documents);
	}

	/**
	 * Deletes documents from the vector store.
	 * @param idList list of document ids for which documents will be removed.
	 */
	void delete(List<String> idList);

	/**
	 * Deletes documents from the vector store based on filter criteria.
	 * @param filterExpression Filter expression to identify documents to delete
	 * @throws IllegalStateException if the underlying delete causes an exception
	 */
	void delete(Filter.Expression filterExpression);

	/**
	 * Deletes documents from the vector store using a string filter expression. Converts
	 * the string filter to an Expression object and delegates to
	 * {@link #delete(Filter.Expression)}.
	 * @param filterExpression String representation of the filter criteria
	 * @throws IllegalArgumentException if the filter expression is null
	 * @throws IllegalStateException if the underlying delete causes an exception
	 */
	default void delete(String filterExpression) {
		SearchRequest searchRequest = SearchRequest.builder().filterExpression(filterExpression).build();
		Filter.Expression textExpression = searchRequest.getFilterExpression();
		Assert.notNull(textExpression, "Filter expression must not be null");
		this.delete(textExpression);
	}

	/**
	 * Returns the native client if available in this vector store implementation.
	 *
	 * Note on usage: 1. Returns empty Optional when no native client is available 2. Due
	 * to Java type erasure, runtime type checking is not possible
	 *
	 * Example usage: When working with implementation with known native client:
	 * Optional<NativeClientType> client = vectorStore.getNativeClient();
	 *
	 * Note: Using Optional<?> will return the native client if one exists, rather than an
	 * empty Optional. For type safety, prefer using the specific client type.
	 * @return Optional containing native client if available, empty Optional otherwise
	 * @param <T> The type of the native client
	 */
	default <T> Optional<T> getNativeClient() {
		return Optional.empty();
	}

	/**
	 * Builder interface for creating VectorStore instances. Implements a fluent builder
	 * pattern for configuring observation-related settings.
	 *
	 * @param <T> the concrete builder type, enabling method chaining with the correct
	 * return type
	 */
	interface Builder<T extends Builder<T>> {

		/**
		 * Sets the registry for collecting observations and metrics. Defaults to
		 * {@link ObservationRegistry#NOOP} if not specified.
		 * @param observationRegistry the registry to use for observations
		 * @return the builder instance for method chaining
		 */
		T observationRegistry(ObservationRegistry observationRegistry);

		/**
		 * Sets a custom convention for creating observations. If not specified,
		 * {@link DefaultVectorStoreObservationConvention} will be used.
		 * @param convention the custom observation convention to use
		 * @return the builder instance for method chaining
		 */
		T customObservationConvention(VectorStoreObservationConvention convention);

		/**
		 * Sets the batching strategy.
		 * @param batchingStrategy the strategy to use
		 * @return the builder instance for method chaining
		 */
		T batchingStrategy(BatchingStrategy batchingStrategy);

		/**
		 * Builds and returns a new VectorStore instance with the configured settings.
		 * @return a new VectorStore instance
		 */
		VectorStore build();

	}

}
```

#### Metadata Filters

* Vector DB에서 유사도 검색을 할 때, 단순히 의미(벡터)만 비슷한 문서를 찾는 것이 아니라 우리가 원하는 특정 조건(메타데이터)에 맞는 문서들 속에서만 유사도를 검색하도록 대상을 좁혀주는 기능이다.
* Spring AI는 `Filter.Expression` 객체를 사용해 조건을 지정한다.
* SQL의 WHERE절과 거의 똑같은 문자열 문법을 지원하기 때문에 매우 직관적이다.
* 지원하는 연산자
  * 관계형 DB(RDB)에서 쿼리를 짤 때 쓰는 연산자들을 그대로 사용할 수 있다.
  * 비교 연산자: `==`, `!=`, `>`, `<`, `>=`, `<=`
  * 포함 연산자: `IN`
  * 논리 연산자: `AND(&&)`, `OR(||)`

#### LLM 한계

* LLM은 뛰어난 문장 생성 능력을 갖추고 있지만, 실무에 바로 적용하기에는 두 가지 치명적인 단점이 있다.
  * 사실 기반 정확성의 부족: 모르는 것도 아는 것처럼 꾸며내는 '환각 현상(Hallucination)'이 발생한다.
  * 문맥 인식 및 최신 정보의 한계: 과거의 특정 시점까지만 학습되어 있어서, 어제 일어난 일이나 회사의 내부 사정은 전혀 알지 못한다.
* 이런 한계를 극복하기 위해 탄생한 필수 기술이 바로 RAG(검색 증강 생성, Retrieval-Augmented Generation)이다.

#### 왜 RAG를 도입해야 하는지?

* 최신 정보 활용
  * LLM을 새로 학습(Fine-tuning)시키는 것은 천문학적인 비용과 시간이 발생한다.
  * 하지만 RAG를 사용하면 모델을 뜯어고칠 필요가 없다.
  * 새로운 매뉴얼이나 최신 뉴스 데이터를 Vector DB에 넣기만 하면, AI가 즉시 그 최신 데이터를 검색해서 답변에 활용할 수 있다.
* 환각 현상(Hallucination) 감소 및 신뢰성 확보
  * AI가 상상력을 발휘해서 대답하는 것을 막고, 철저히 주어진 문서(근거) 안에서만 대답하도록 통제한다.
  * RAG 파이프라인을 구축할 때 데이터에 '출처(Source)'를 함께 저장한다.
  * AI가 출처를 명시하며 답변하게 만들 수 있어, 사용자가 결과를 검증하고 신뢰할 수 있다.
* 도메인 특화 및 강력한 보안 (망분리 환경 대응)
  * 내부 문서나 사내 DB와 연동하여 특정 전문 분야(법률, 의료, 사내 HR 등)에 특화된 질의응답 시스템을 만들 수 있게 된다.
  * 전자금융거래법(전금법)이나 망분리 규제로 인해 외부 클라우드 AI(OpenAI 등)를 쓰지 못하는 기업들이 많은데, 이 때 ‘사내 구축형 Local LLM + 사내 폐쇄망 내부 문서/DB(Vector DB)' 조합으로 RAG를 구축하면, 회사 기밀 유출 걱정 없이 안전하게 AI를 활용할 수 있다.

#### Spring AI에서 RAG를 구현하는 방법

* Spring AI는 RAG 구현을 위해 프롬프트를 가로채고 조작하는 Advisor패턴을 적극적으로 사용한다.
* 실무에서는 목적에 따라 크게 두 가지 Advisor를 선택해서 사용한다.
  * `QuestionAnswerAdvisor`
    * 사용자의 질문을 받으면 Vector DB에서 관련 문서를 찾아 프롬프트에 끼워 넣고 답변을 생성하는 가장 기본적인 RAG 흐름이다.
    * 구조가 단순해서 빠르게 초기 프로토타입을 만들 때 유용하다.
  * `RetrievalAugmentationAdvisor`
    * 단순한 검색을 넘어, RAG 시스템을 레고 블록처럼 자유롭게 조립하고 확장할 수 있게 해주는 고급형 Advisor이다.
    * 새로운 검색 기법이나 문서 정제 기술(패러다임)이 등장했을 때, 전체 코드를 갈아엎을 필요 없이 필요한 모듈(블록)만 갈아 끼워 대응할 수 있는 유연함을 제공한다.
> **Modular RAG 처리 흐름(RetrievalAugmentationAdvisor)**
>
> * 쿼리 변환 및 전처리 : 모호한 질문을 AI가 검색하기 좋은 형태로 다듬는 과정. `QueryTransformer`를 체인처럼 연결하여 질문을 압축, 재작성, 번역한다.
> * 쿼리 확장 : 하나의 질문으로만 검색하면 놓치는 문서가 생길 수 있다. `MultiQueryExpander`를 사용해 "스프링 부트 에러"라는 질문을 "Spring Boot Exception", "스프링 서버 장애 원인" 등 다양한 관점의 쿼리로 확장하여 검색의 사각지대를 없앤다.
> * 문서 검색 : 다듬어지고 확장된 쿼리들을 가지고 실제로 Vector Database를 뒤지는 단계. `VectorStoreDocumentRetriever`를 통해 `Metadata Filter` 등 다양한 조건으로 VectorStore 검색을 수행한다.
> * 문서 결합 및 후처리 : 여러 쿼리에서 검색된 문서들을 하나로 결합하고 중복/불필요 문서를 제거한다. 가장 중요한 문서가 위로 오도록 순서를 재배치(리랭킹, Reranking)하거나 요약하여, 컨텍스트 길이를 최적화한 정보만 정제한다.
> * 문서 기반 답변 생성 : 정제된 최종 문서 리스트를 사용자의 쿼리에 덧붙여 LLM에게 보내준다. `ContextualQueryAugmenter`가 이 결합 작업을 수행하며, 만약 검색된 문서가 아무것도 없을 경우 "관련 정보를 찾을 수 없습니다" 등의 방어하는 처리도 지원한다.

#### [ETL 파이프라인](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#page-title)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c526ec87-c0dc-4c3f-a3d3-95eac8ac42ff.png)

* 다양한 형태의 원본 데이터를 AI가 소화하기 가장 좋은 형태로 가공하여 DB에 밀어 넣는 ETL(Extract, Transform, Load) 파이프라인을 체계적인 클래스 구조를 말한다.

#### 1. 추출(Extract) - `Document Reader`

* 다양한 데이터 소스(파일, DB, 웹페이지 등)에서 텍스트를 읽어와 Spring AI의 기본 규격인 Document 객체 리스트로 변환하는 핵심 컴포넌트이다.
* 텍스트 및 웹 데이터 추출기
  * `JsonReader` : JSON 파일에서 특정 키(Key)의 값만 쏙쏙 뽑아낸다.
  * `TextReader` : 단순 텍스트 파일을 하나의 Document로 변환한다.
  * `JsoupDocumentReader` : 크롤링할 때 많이 쓰는 JSoup을 기반으로, HTML 문서에서 특정 CSS 선택자(Selector)에 해당하는 부분만 정밀하게 추출한다.
  * `MarkdownDocumentReader` : 마크다운의 구조(수평선, 코드 블록 등)를 이해하고, 그 기준에 맞춰 문서를 지능적으로 분할해준다.
* PDF 및 오피스 문서 추출기
  * `PagePdfDocumentReader` : PDF 문서를 '페이지 단위'로  잘라서 여러 Document로 만든다.
  * `ParagraphPdfDocumentReader` : PDF 내부에 있는 목차(TOC) 정보를 이용해 '논리적인 단락별'로 잘라준다.
  * `TikaDocumentReader` : Apache Tika 엔진을 사용하여 PDF, DOC/DOCX, PPT/PPTX, HTML 등 수많은 포맷을 이 리더 하나로 전부 처리할 수 있는 강력한 클래스이다.

#### 2. 변환(Transform) - `DocumentTransformer`

* 추출된 거대한 Document를 AI 모델의 제한된 기억력(Context Window)에 맞게 잘게 쪼개거나(Chunking), 검색이 더 잘 되도록 유용한 정보를 붙이는 가공 단계이다.
* 문서 분할(Chunking)
  * AI가 한 번에 읽을 수 있는 토큰 수에는 한계가 있기 때문에, 긴 문서를 적절한 크기로 자르는 것은 매우 중요하다. 그래서 `TokenTextSplitter` 등을 활용하여 분할한다.
* 메타데이터 자동 생성 및 강화
  * 데이터를 가공하는 과정에서 ChatModel(AI)을 직접 호출하여 문서를 더 똑똑하게 태깅할 수 있다.
  * `KeywordMetadataEnricher` : AI에게 문서를 읽히고 핵심 키워드를 뽑아낸다.
  * `SummaryMetadataEnricher` : AI에게 문서 내용의 요약본을 작성하게 한다.

#### 3. 적재(Load) - `DocumentWriter`

* 모든 가공이 끝난 `Document` 객체 리스트를 최종 목적지에 저장하는 단계이다.
* `vectorStore.add(documents)`를 호출하는 순간 적재된다.
* `FileDocumentWriter` : 가공된 문서 리스트를 사람이 눈으로 읽기 쉬운 형태의 텍스트 파일로 뽑아낸다. 주로 디버깅시 사용한다.

#### Spring AI RAG 기본 설계

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fa6cb343-b5da-48f7-b937-1856a7328796.png)
