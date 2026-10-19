# Spring AI - Spring AI Introduction

#### Spring AI란 무엇일까?

* 자바, 스프링 개발자가 ChatGPT, Claude, Gemini 같은 AI 모델들을 스프링을 활용하여 다룰 수 있도록 돕는 스프링 공식 프레임워크
* Spring AI의 핵심 역할은 AI 모델들과 백엔드 서비스를 매끄럽게 연결하는 중간다리(인터페이스) 역할
  * 파이썬과의 역할 분담 : AI 모델의 학습 및 생성은 파이썬이 주도하지만, 이미 완성된 AI 모델을 실제 서비스(웹, 앱)에 붙이고 비즈니스 로직을 구현하는 것은 백엔드의 영역이다.
  * 복잡한 통신 코드 제거 : 기존에는 개발자가 직접 HTTP 요청 코드를 짜서 AI API를 호출해야 했으나, Spring AI를 활용하면 이를 내부적으로 자동으로 처리해준다.

#### 일반 API 호출 vs Spring AI 차이점

* 일반 API 호출(직접 연동)
  * 높은 종속성 : OpenAI, 구글, 앤트로픽 등 AI 모델 제공사마다 API 문서와 요청 응답 구조가 제각각이다.
  * 유지보수 지옥 : ChatGPT를 쓰다가 비용이나 성능 문제로 Claude 모델로 변경하려면, 기존에 짜놓은 자바 통신 코드를 전부 갈아엎고 새로 작성해야하는 문제가 발생한다.
* Spring AI(프레임워크 활용)
  * 추상화 제공 : AI 모델 종류에 상관없이 개발자는 Spring AI가 제공하는 표준 인터페이스(ChatModel)만 활용하여 코딩하면 된다.
  * 유연한 대처 : AI 모델을 변경할 때 자바 비즈니스 로직은 단 한 줄도 건드리지 않고, 설정 파일의 옵션만 바꾸면 즉시 전환 가능하다.

#### VectorStore Interface

```java
/**
 * The {@code VectorStore} interface defines the operations for managing and querying
 * documents in a vector database. It extends {@link DocumentWriter} to support document
 * writing operations. Vector databases are specialized for AI applications, performing
 * similarity searches based on vector representations of data rather than exact matches.
 * This interface allows for adding, deleting, and searching documents based on their
 * similarity to a given query.
 */
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
