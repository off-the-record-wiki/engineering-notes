# Apache Kafka - Kafka Streams

#### KStream, KTable, GlobalKTable

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9ab602a7-d7a9-4f11-b38c-d0f85ba8dfe2.png)

* `KStream`은 Kafka 토픽의 데이터를 이벤트 흐름으로 간주한다. 각 레코드는 독립적인 사건이며, 데이터베이스처럼 기존 값을 수정하는 것이 아니라 새로운 사건이 발생할 때마다 로그의 끝에 계속해서 추가하는 방식이다.
  * **Append-only** : 기존 키를 지우거나 덮어쓰지 않는다. 단순히 새로운 오프셋에 새로운 레코드를 추가한다.
  * **무상태(Stateless)** : KStream은 이전에 처리했던 동일 키의 값을 기억하지 않는다. 이전 값이 무엇인지 확인해 값을 계산하거나 변경하지 않고 그저 들어오는 대로 처리한다.
* 성능 측면 : 데이터를 덮어쓰거나 기존 데이터를 찾아 수정하는 복잡한 과정이 없으므로 쓰기 작업(Write)이 매우 빠르다.
* 복구 용이성 : 모든 이벤트 기록이 그대로 남아있으므로 장애 발생 시 처음부터 다시 데이터를 읽어와 상태를 재구성하기 매우 쉽다.
* 시계열 데이터 처리 : 모든 변화 과정이 남기 때문에 특정 시점의 상태를 추적하거나 로그 분석을 하는 데 최적화되어 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e922bac7-e8f7-4dbb-a5e7-6d562f7a753a.png)

* `KTable`은 Kafka 토픽의 데이터를 키-값(Key-Value) 쌍의 데이터베이스 상태로 간주한다. 특정 키에 새로운 메시지가 들어오면 이전 값을 덮어쓰기(Upsert)하여 항상 가장 최신 정보만을 유지한다.
  * **Upsert** : 기존 키를 덮어쓴다. 동일한 키가 들어오면 기존 데이터를 찾아서 최신 값으로 변경한다.
  * **최신 값 스냅샷** : `KTable`은 로컬 상태 저장소(State Store)를 유지한다.
  * **Changelog(변경 로그)** : `KTable` 데이터는 Kafka 토픽에 저장된 변경 사항(Changelog)들을 계속해서 반영한 결과물이다. 어떤 과정을 거쳐 현재의 값이 되었는가보다는 현재 최종 상태가 무엇인가가 중요하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/872cfcea-4782-4c0a-9c8f-989b04040635.png)

* `GlobalKTable`은 모든 파티션의 전체 데이터가 애플리케이션의 모든 태스크(인스턴스)에 복제되는 구조이다.
  * **브로드캐스트 방식** : 어떤 인스턴스가 어떤 파티션의 KStream을 처리하든 상관없이 GlobalKTable의 전체 데이터를 로컬에 가지고 있다.
  * **메모리 사용량 급증** : 모든 인스턴스가 전체 데이터를 복제해서 가지고 있어야 하므로 데이터의 크기가 매우 크다면 각 노드의 메모리 부족(OOM)이 발생할 수 있다. 따라서 `GlobalKTable`은 참조 데이터처럼 상대적으로 크기가 작은 데이터셋을 사용할 때 가장 적합하다.
* 어느 인스턴스에서든 GlobalKTable의 데이터를 로컬에서 즉시 조회할 수 있다.
* Re-partitioning 방지 : 일반 KTable과 KStream을 조인하려면 두 토픽의 파티션이 동일하게 분배되어야 하므로, 그렇지 않은 경우 Kafka 내부적으로 데이터를 재배치하는 비용이 발생한다. 하지만 GlobalKTable은 모든 인스턴스가 전체 데이터를 가지고 있어 재배치 과정 없이 즉시 조인이 가능하다.
* 데이터 조회 성능 : 조인 시 네트워크 호출 없이 로컬 상태 저장소에서 데이터를 읽으므로 매우 빠르다.

#### 토폴로지(Topology)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7c3a6ecb-672a-4460-89b1-dea76540e625.png)

* 토폴로지란, 2개 이상의 노드들과 선으로 이루어진 집합을 말한다.
* 여러 종류들이 있는데 스트림즈에서 사용하는 토폴로지는 트리 형태와 유사하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5ae54f2b-8a85-47d8-a8aa-dfab3bc6b772.png)

* 토폴로지를 이루는 노드를 하나의 프로세서(Processor)라고 부르고 노드와 노드를 이은 선을 스트림(Stream)이라고 부른다.
* 소스 프로세서 : 데이터를 처리하기 위해 최초로 선언해야 하는 노드로, 하나 이상의 토픽에서 데이터를 가져온다.
* 스트림 프로세서 : 다른 프로세서가 반환한 데이터를 처리하는 역할을 한다.(변환, 분기 처리와 같은 로직)
* 싱크 프로세서 : 데이터를 특정 카프카 토픽으로 저장하는 역할을 하며 스트림즈로 처리된 데이터의 최종 종착지이다.

#### 필수 옵션

* **`bootstrap.servers`** : `프로듀서가 데이터를 전송할 대상 카프카 클러스터에 속한 브로커의 호스트 이름:포트`를 1개 이상 작성한다. 2개 이상 브로커 정보를 입력해 일부 브로커에서 이슈가 발생하더라도 접속하는 데에 이슈가 없도록 설정할 수 있다.
* **`application.id`** : 스트림즈 애플리케이션을 구분하기 위한 고유 아이디를 설정한다. 다른 로직을 가진 스트림즈 애플리케이션들은 서로 다른 `application.id`값을 가져야 한다.

#### 선택 옵션

* **`default.key.serde`** : 레코드의 메시지 키를 직렬화/역직렬화하는 클래스를 지정한다. 기본값은 바이트 직렬화/역직렬화 클래스인 `Serdes.ByteArray().getClass().getName()`이다.
* **`default.value.serde`** : 레코드의 메시지 값을 직렬화/역직렬화하는 클래스를 지정한다. 기본값은 바이트 직렬화/역직렬화 클래스인 `Serdes.ByteArray().getClass().getName()`이다.
* **`num.stream.threads`** : 스트림 프로세싱 실행 시 실행될 쓰레드 개수를 지정하는 것으로 기본값은 1이다.
  * 처리량 극대화 가능 : 쓰레드가 하나라면, 애플리케이션이 담당하는 전체 파티션의 데이터를 혼자서 순차적으로 처리하는데 `num.stream.threads`를 늘리면 하나의 애플리케이션 인스턴스 내에서 병렬 처리가 가능해진다. 각 쓰레드가 여러 태스크를 분담하여 실행하므로 CPU 자원을 더 효율적으로 사용해 전체적인 처리 속도가 빨라진다.
  * CPU 활용도 최적화 : 현재 쓰레드가 하나인데 CPU 사용률이 매우 낮다면 파티션 데이터를 처리할 때 대기 시간이 길다는 뜻이다. 이 떄, 쓰레드 개수를 늘리면 대기 시간 동안 다른 스레드가 연산을 수행하게 하여 CPU 코어 활용률을 높일 수 있다.
  * 최적화 시 고려해야 할 핵심 규칙 : 무조건 개수를 늘린다고 좋은 것이 아니다. 쓰레드 개수는 입력 토픽의 파티션 개수보다 클 수 없다.
  * 메모리 자원 : 쓰레드가 늘어나면 각 쓰레드가 로컬 상태 저장소에 접근하거나 버퍼를 사용하는 빈도가 늘어난다. 따라서 전체 애플리케이션의 메모리 사용량도 함께 증가한다. 메모리가 부족하면 GC 빈도가 높아져 오히려 성능이 저하될 수 있다.
  * 상태 저장소(RocksDB) 부하 : 상태가 있는 처리를 할 경우 쓰레드가 늘어나면 RocksDB에 대한 동시 접근이 많아진다. 디스크 I/O 병목이 발생할 수 있으므로 주의해야 한다.
* **`state.dir`** : 상태기반 데이터 처리를 할 때, 데이터를 저장할 디렉터리를 지정하는 것으로 기본값은 `/tmp/kafka-streams`이다.

#### Streams DSL Window Processing

* 스트림 데이터를 분석할 때, 가장 많이 활용하는 프로세싱 중 하나는 `Window` 연산이다.
* `Window` 연산은 특정 시간에 대응하여 취합 연산을 처리할 때 활용한다.
* 카프카 스트림즈에서 제공하는 `Window Processing` 4가지를 지원한다. 모든 프로세싱은 메시지 키를 기준으로 취합된다.
* 그러므로 해당 토픽에 동일한 파티션에는 동일한 메시지 키가 있는 레코드가 존재해야지만 정확한 취합이 가능하다.

#### 텀블링 윈도우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/393d87aa-c6d0-4919-a5fe-8d2ffef43294.png)

* 서로 겹치지 않은 윈도우를 특정 간격으로 지속적으로 처리할 때 사용한다.
* 윈도우 최대 사이즈에 도달하면 해당 시점에 데이터를 취합하여 결과를 도출한다.
* 텀블링 윈도우는 단위 시간당 데이터가 필요할 경우 사용할 수 있다.

#### 호핑 윈도우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4b25b495-daef-4a33-9404-5d19151b7452.png)

* 일정 시간 간격으로 겹치는 윈도우가 존재하는 윈도우 연산을 처리할 경우 사용한다.
* 호핑 윈도우는 윈도우 사이즈와 윈도우 간격 2가지 변수를 가진다.
* 윈도우 사이즈는 연산을 수행할 최대 윈도우 사이즈를 뜻하고 윈도우 간격은 서로 다른 윈도우 간격을 뜻한다.
* 텀블링 윈도우와 다르게 동일한 키의 데이터가 서로 다른 윈도우에서 여러 번 연산될 수 있다.

#### 슬라이딩 윈도우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d2989070-0882-447a-bdf3-1711ca656540.png)

* 슬라이딩 윈도우는 호핑 윈도우와 유사하지만 데이터의 정확한 시간을 바탕으로 윈도우 사이즈에 포함되는 데이터를 모두 연산에 포함시키는 특징이 있다.

#### 세션 윈도우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bedd0a4a-5343-4905-9260-fd0267faace9.png)

* 세션 윈도우는 동일한 메시지 키의 데이터를 한 세션에 묶어 연산할 때 사용한다.
* 세션의 최대 만료시간에 따라 윈도우 사이즈가 달라진다.
* 세션 만료 시간이 지나게 되면 세션 윈도우가 종료되고 해당 윈도우의 모든 데이터를 취합하여 연산한다.
* 그렇기 때문에 세션 윈도우의 윈도우 사이즈는 가변적이다.

#### Processor API

* Processor API는 스트림즈 DSL보다 저수준이지만 토폴로지를 기준으로 데이터를 처리한다는 관점에서 동일한 역할을 수행한다.
* 스트림즈 DSL은 데이터 처리, 분기, 조인을 위한 다양한 메서드들을 제공하지만 추가적인 상세 로직 구현이 필요하다면 Processor API를 활용할 수 있다.
* 프로세서 API에서는 스트림즈 DSL에서 사용했던 `KStream`, `KTable`, `GlobalKTable` 개념이 없다.
* 다만 스트림즈 DSL과 프로세서 API는 함께 구현하여 사용할 때는 활용할 수 있다.
* Processor API를 구현하기 위해서 `Processor` 또는 `Transformer` 인터페이스로 구현한 클래스가 필요하다.
* `Processor` 인터페이스는 일정 로직이 이루어진 뒤 다음 프로세서로 데이터가 넘어가지 않을 때 사용하고 `Transformer` 인터페이스는 일정 로직이 이루어진 뒤 다음 프로세서로 데이터를 넘길 때 사용한다.

> Processor API는 DSL이 제공하지 못하는 자유도가 필요할 때 사용한다.
>
> * 초고성능/정밀 튜닝 : DSL보다 더 세밀하게 상태 저장소에 접근하거나 메모리 사용량을 극한으로 줄여야 할 때
> * 복잡한 비즈니스 로직 : DSL 연산 체인으로 표현하기 힘든 복잡한 흐름
> * 기존 라이브러리 연동 : 특정 라이브러리가 명시적인 `Processor` 인터페이스를 요구하거나 아주 독특한 방식으로 토픽의 오프셋을 직접 제어해야 할 때
> * Punctuators(스케줄링) : DSL에서는 지원되지 않는 시간 또는 스트림 시간 기반의 정기적인 작업을 구현해야 할 때 Processor API의 `context.schedule()`이 필수적이다.

```java
/**
 * A processor of key-value pair records.
 *
 * @param <KIn> the type of input keys
 * @param <VIn> the type of input values
 * @param <KOut> the type of output keys
 * @param <VOut> the type of output values
 */
@FunctionalInterface
public interface Processor<KIn, VIn, KOut, VOut> {

    /**
     * Initialize this processor with the given context. The framework ensures this is called once per processor when the topology
     * that contains it is initialized. When the framework is done with the processor, {@link #close()} will be called on it; the
     * framework may later re-use the processor by calling {@code #init()} again.
     * <p>
     * The provided {@link ProcessorContext context} can be used to access topology and record meta data, to
     * {@link ProcessorContext#schedule(Duration, PunctuationType, Punctuator) schedule} a method to be
     * {@link Punctuator#punctuate(long) called periodically} and to access attached {@link StateStore}s.
     *
     * @param context the context; may not be null
     */
    default void init(final ProcessorContext<KOut, VOut> context) {}

    /**
     * Process the record. Note that record metadata is undefined in cases such as a forward call from a punctuator.
     *
     * @param record the record to process
     */
    void process(Record<KIn, VIn> record);

    /**
     * Close this processor and clean up any resources. Be aware that {@code #close()} is called after an internal cleanup.
     * Thus, it is not possible to write anything to Kafka as underlying clients are already closed. The framework may
     * later re-use this processor by calling {@code #init()} on it again.
     * <p>
     * Note: Do not close any streams managed resources, like {@link StateStore}s here, as they are managed by the library.
     */
    default void close() {}
}
```
```java
/**
 * The {@code Transformer} interface is for stateful mapping of an input record to zero, one, or multiple new output
 * records (both key and value type can be altered arbitrarily).
 * This is a stateful record-by-record operation, i.e, {@link #transform(Object, Object)} is invoked individually for
 * each record of a stream and can access and modify a state that is available beyond a single call of
 * {@link #transform(Object, Object)} (cf. {@link KeyValueMapper} for stateless record transformation).
 * Additionally, this {@code Transformer} can {@link ProcessorContext#schedule(Duration, PunctuationType, Punctuator) schedule}
 * a method to be {@link Punctuator#punctuate(long) called periodically} with the provided context.
 * <p>
 * Use {@link TransformerSupplier} to provide new instances of {@code Transformer} to Kafka Stream's runtime.
 * <p>
 * If only a record's value should be modified {@link ValueTransformer} can be used.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <R> {@link KeyValue} return type (both key and value type can be set
 *            arbitrarily)
 * @see TransformerSupplier
 * @see ValueTransformer
 * @see KStream#map(KeyValueMapper)
 * @see KStream#flatMap(KeyValueMapper)
 * @deprecated Since 4.0. Use {@link org.apache.kafka.streams.processor.api.Processor api.Processor} instead.
 */
@Deprecated
public interface Transformer<K, V, R> {

    /**
     * Initialize this transformer.
     * This is called once per instance when the topology gets initialized.
     * When the framework is done with the transformer, {@link #close()} will be called on it; the
     * framework may later re-use the transformer by calling {@link #init(ProcessorContext)} again.
     * <p>
     * The provided {@link ProcessorContext context} can be used to access topology and record meta data, to
     * {@link ProcessorContext#schedule(Duration, PunctuationType, Punctuator) schedule} a method to be
     * {@link Punctuator#punctuate(long) called periodically} and to access attached {@link StateStore}s.
     * <p>
     * Note, that {@link ProcessorContext} is updated in the background with the current record's meta data.
     * Thus, it only contains valid record meta data when accessed within {@link #transform(Object, Object)}.
     *
     * @param context the context
     */
    void init(final ProcessorContext context);

    /**
     * Transform the record with the given key and value.
     * Additionally, any {@link StateStore state} that is attached to this operator can be accessed and modified
     * arbitrarily (cf. {@link ProcessorContext#getStateStore(String)}).
     * <p>
     * If only one record should be forward downstream, {@code transform} can return a new {@link KeyValue}. If
     * more than one output record should be forwarded downstream, {@link ProcessorContext#forward(Object, Object)}
     * and {@link ProcessorContext#forward(Object, Object, To)} can be used.
     * If no record should be forwarded downstream, {@code transform} can return {@code null}.
     *
     * Note that returning a new {@link KeyValue} is merely for convenience. The same can be achieved by using
     * {@link ProcessorContext#forward(Object, Object)} and returning {@code null}.
     *
     * @param key the key for the record
     * @param value the value for the record
     * @return new {@link KeyValue} pair&mdash;if {@code null} no key-value pair will
     * be forwarded to down stream
     */
    R transform(final K key, final V value);

    /**
     * Close this transformer and clean up any resources. The framework may
     * later re-use this transformer by calling {@link #init(ProcessorContext)} on it again.
     * <p>
     * To generate new {@link KeyValue} pairs {@link ProcessorContext#forward(Object, Object)} and
     * {@link ProcessorContext#forward(Object, Object, To)} can be used.
     */
    void close();

}
```
