# Java - Producer/Consumer Problem

#### 생산자 - 소비자 문제

* 각 용어에 대한 이해가 필요하다.
  * 생산자(Producer) : 데이터를 생산하는 역할
  * 소비자(Consumer) : 데이터를 소비(=사용)하는 역할
  * 버퍼(Buffer) : 생산자가 생성한 데이터를 일시적으로 저장하는 공간, 한정된 크기를 가지며 생산자와 소비자가 이 버퍼를 통해 데이터를 주고받는다.

>❗문제 상황1 : 생산자가 너무 빠르면? - 버퍼가 가득 차서 더 이상 데이터를 넣을 수 없을 때까지 생산자가 데이터를 생성한다. 버퍼가 가득 찬 경우 생산자는 버퍼에 빈 공간이 생길 때까지 기다려야 한다.<br>
>❗문제 상황2 : 소비자가 너무 빠르면? - 버퍼가 비어서 더 이상 소비할 데이터가 없을 때까지 소비자가 데이터를 처리한다. 버퍼가 비어있을 때 소비자는 버퍼에 새로운 데이터가 들어올 때까지 기다려야 한다.<br>

#### 생산자 소비자 문제 분석

> [📚Producer Consumer Problem](https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem)

#### 0. 큐 상황

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/108f0175-96ae-41da-ba75-58b3d0a0f134.png)

#### 1. 생산자 쓰레드 실행 시작

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/64f8cf07-597d-4c20-8942-b0a29dadbecd.png)

* p1 쓰레드가 락을 획득하고 임계영역으로 들어가 큐에 데이터를 적재한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/442fe697-de66-4d5c-a6cd-392291c704ef.png)

* p1 쓰레드가 작업을 완료한 후 락을 반납하고 종료된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2215a2b4-ddc1-4a5e-879e-cb18e0ccfb29.png)

* p2 쓰레드가 락을 획득하고 임계영역으로 들어가 큐에 데이터를 적재한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/eaa3f82e-ad4e-4871-bf3e-24f6b811fa9a.png)

* p2 쓰레드가 작업을 완료한 후 락을 반납하고 종료된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/31075c52-07bd-4843-b318-e00ad9b52623.png)

* p3 쓰레드가 임계영역으로 들어가기 위해 락을 획득한다.
* 큐에 데이터를 적재하려고 하나 이미 큐에 데이터가 다 찼다.
* p3 쓰레드를 잠시 대기한다. 
* 이 때, 반복문을 사용해서 1초마다 큐에 데이터를 적재할 수 있는 빈 자리가 있는지 반복해서 확인한다.
  * 빈 자리가 있다면 큐에 데이터를 입력하고 완료된다.
  * 빈 자리가 없다면 `sleep()`을 사용해서 잠시 대기한다. 그리고 반복문을 계속 수행한다.

#### 2. 소비자 쓰레드 실행 시작

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8cac4812-ecce-41e5-8f72-46c84e9f1b7c.png)

<br>

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/99de66b4-f06b-4f5d-a025-7f4a468efa6d.png)

* c1 쓰레드가 락을 보유하려고 하지만 현재 락이 없기 때문에 무한 대기 문제가 발생한다.
* p3 쓰레드가 큐에 공간이 남아있는지를 계속 확인하는 과정에서 락을 계속 점유하기 때문에 c 쓰레드들이 무한 대기하는 것이다.

#### wait, notify(생산자 우선)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c655feac-108f-4862-882d-23ade766970a.png)

* 쓰레드 대기 집합(wait set)
  * `synchronized` 임계 영역 안에서 `Object.wait()`를 호출하면 쓰레드는 대기 상태로 들어간다. 이렇게 대기 상태에 들어간 쓰레드를 관리하는 것을 대기 집합(wait set)이라고 한다. 참고로 모든 객체는 각자의 대기 집합을 가지고 있다.
  * 여기서는 `BoundedQueue` 인스턴스의 락을 사용한 것으로 이해할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fb57d289-1f68-44e7-bd1a-e93970ee817b.png)

* 큐에 빈 공간이 남아있기 때문에 p1, p2는 문제가 없겠지만 p3 작업 시점에서는 문제가 발생한다.
* 큐가 가득 찼기 때문에 `wait()`를 호출한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/483b9ceb-bc58-4c59-8c10-972a88197776.png)

* p3에서 `wait()`를 호출하면 락을 반납하고 해당 쓰레드의 상태는 `RUNNABLE` -> `WAITING` 상태로 변경된다.
* 이렇게 상태가 변경된 쓰레드가 쓰레드 대기 집합에서 관리된다.
* 쓰레드 대기 집합에서 관리되는 쓰레드는 이후에 다른 쓰레드가 `notify()`를 호출해 쓰레드 대기 집합에 신호를 주면 깨어날 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c0be92b8-66c5-45c6-8958-a8319f037dbb.png)

* 소비자 쓰레드가 데이터를 획득했기 때문에 큐에 데이터를 보관할 자리가 생기게 되고 소비자 쓰레드가 `notify()`를 호출해서 쓰레드 대기 집합에 이 사실을 공지한다.
* `notify()`를 받으면 대기 집합에 있는 쓰레드 중 하나가 기상하는데 이 때, 깨어난다고 바로 작동하지 않는다. 깨어난 쓰레드가 여전히 임계 영역 안에 존재하지만 이 때, 소비자 쓰레드가 락을 점유하고 사용 중이기 때문에 락을 획득하기 위해서 `WAITING` -> `BLOCKED` 상태로 전환된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d6840714-688e-4ec3-b1a3-f305447d13fd.png)

* c1이 데이터 소비를 완료하고 락을 반납하고 임계 영역을 빠져나가게 되고 그 다음으로 생산자 쓰레드 p3가 락을 획득해 이후 코드를 실행한다. 큐에 빈 공간이 없어 `notify()`를 호출하게 되는데 허나 쓰레드 대기 집합에는 아무도 없기 때문에 아무런 일이 일어나지 않는다.

#### wait, notify(소비자 우선)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5a52154a-c14c-4cac-812d-e3b95214251c.png)

* c1 소비자 쓰레드가 락을 획득하고 큐에서 데이터를 소비하려 하지만 큐에 데이터가 없는 상태이다.
* 이런 상태가 되면 c1 쓰레드는 쓰레드 대기 집합으로 이동해 관리된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3ae35cf6-0447-4654-bbfa-6ed1852a0171.png)

* 이런 수순으로 나머지 c2, c3 소비자 쓰레드 역시 쓰레드 대기 집합으로 이동해 관리가 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f9f0b42f-eb23-4399-9b9f-0d2500e24b62.png)

* 그 다음으로 p1 생산자 쓰레드가 락을 획득해 작업을 시작하게 된다.
* 큐에 데이터가 있기 때문에 소비자를 깨울 수 있게 되고 `notify()`를 통해 쓰레드 대기 집합에 이 사실을 알린다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d0dcd49b-828e-4caa-8702-e50789cc6431.png)

* `notify()`를 인지한 쓰레드 대기 집합은 쓰레드 중 하나를 깨우는데 이 때, 어떤 쓰레드가 깨워질지는 예측할 수 없다.
* 허나 지금 p1 생산자 쓰레드가 락을 점유 후 사용을 하고 있기 때문에 먼저 나가게 된 c1 소비자 쓰레드는 `WAITING` -> `BLOCKED` 상태로 전환된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/22dcbe80-a797-4ac7-9038-41cdebb640e9.png)

* p1 생산자 쓰레드가 락을 반납하면 `BLOCKED` 상태의 c1 소비자 쓰레드가 락을 획득하고 큐의 데이터를 소비한다.
* 그러나 큐의 데이터가 없기 때문에 c1 소비자 쓰레드가 작업을 처리하고 `notify()` 신호를 쓰레드 대기 집합에 보낸다고 하더라도 처리를 할 수 없게 되기 때문에 위와 같은 수순의 과정이 반복이 된다.

> 정리
>
> * 최종적으로는 생산자 우선이나 소비자 우선이나 모두 데이터를 정상 생산하고 데이터를 정상 소비할 수 있었다.
> * 허나 소비자 우선인 경우 소비자가 같은 소비자를 깨울 수 있는데 이 경우 큐에 데이터가 없을 가능성이 있다.
> * 이 때, 깨어난 소비자 쓰레드가 CPU 자원만을 소모하고 다시 대기 집합으로 들어가기 때문에 비효율적이다.
> * `notify()` 대신 `notifyAll()`도 있는데 해당 메서드는 쓰레드 대기 집합(work set)에 있는 모든 쓰레드를 전부 깨운다.
> * 이 때, 쓰레드 대기 집합에 정말 많은 쓰레드가 대기하는 상황이라면 락을 먼저 획득하려고 할텐데 락 경합이 발생할 수 밖에 없게 된다.
> * `notifyAll()`을 통해 쓰레드 기아 문제를 막을 수 있지만, 비효율까지는 막지 못한다.

#### Lock Condition

* 자바는 1.0부터 존재한 `synchronized`와 `BLOCKED` 상태를 통한 임계 영역 관리 단점을 해결하기 위해 자바 1.5부터 Lock 인터페이스와 ReentrantLock 구현체를 사용한다.

#### 생산자 소비자 대기 공간 분리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ebaddba2-307e-47e1-9d62-3d7efbc1fd0b.png)

* `consumerCond` : 소비자를 위한 쓰레드 대기 공간
* `producerCond` : 생산자를 위한 쓰레드 대기 공간

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f0ce119b-268f-45aa-847d-5b4a18521f69.png)

* p0 쓰레드는 ReentrantLock의 락을 획득하고 큐에 데이터를 보관한 다음 소비자 쓰레드 대기 공간에 `signal()`을 보내 알려준다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/29074d64-e04a-4422-99fd-90488dd70964.png)

* 소비자 쓰레드 중 임의의 쓰레드 하나가 깨어난다.
* 아직 p0 쓰레드가 락을 획득하고 있으므로 락을 반납하면 소비자 쓰레드가 락을 획득해서 큐의 데이터를 소비하게 된다.

#### `notify()` vs `signal()`

* `notify()` : 대기 중인 쓰레드 중 임의의 쓰레드 하나를 선택해서 깨운다. 이 때, 쓰레드가 깨어나는 순서는 정의되어 있지 않다.
* `signal()` : 대기 중인 쓰레드 중 하나를 깨우며 일반적으로는 FIFO 순서로 깨운다.

#### 쓰레드의 대기

* 자바의 모든 객체 인스턴스는 멀티쓰레드와 임계 영역을 다루기 위해 내부에 3가지 기본 요소를 가진다.
  * 모니터 락
  * 락 대기 집합
  * 쓰레드 대기 집합
* `synchronized` 사용하는 임계 영역에 들어가려면 모니터 락이 필요하다.
* 모니터 락이 없으면 락 대기 집합에 들어가서 `BLOCKED` 상태로 기다린다.
* 모니터 락을 반납하면 락 대기 집합에 있는 쓰레드 중 하나가 락을 획득하게 되고 `BLOCKED` -> `RUNNABLE` 상태가 된다.
* `wait()`를 호출해서 쓰레드 대기 집합에 들어가기 위해서 모니터 락을 사용하게 된다.
* 쓰레드 대기 집합에 들어가면 모니터 락을 반납한다.
* 쓰레드가 `notify()`를 호출하면 쓰레드 대기 집합에 있는 쓰레드 중 하나가 쓰레드 대기 집합을 빠져나오고 모니터 락 획득을 시도한다.
* 모니터 락을 획득했다면 임계 영역을 수행한다. 만약 모니터 락을 획득하지 못하면 락 대기 집합에 들어가서 `BLOCKED` 상태로 기다린다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2a030938-ad02-44b7-91ba-fda051739142.png)

* `ReentrantLock` 락 획득 대기
  * `ReentrantLock` 대기 큐에서 관리
  * `WAITING` 상태로 락 획득을 대기
  * `lock.lock()`을 호출했을 때 락이 없으면 대기
  * 다른 쓰레드가 `lock.unlock()`을 호출했을 때 대기가 풀리며 락 획득을 시도, 락을 획득하면 대기 큐를 빠져 나간다.
* `await()` 대기
  * `condition.await()`을 호출했을 때 객체 쓰레드 대기 공간에서 관리
  * `WAITING` 상태로 대기
  * 다른 쓰레드가 `condition.signal()`을 호출했을 때 `condition` 객체의 쓰레드 대기 공간에서 빠져 나간다.

> `synchronized` 락 획득 대기 쓰레드(BLOCKED)와 `ReentrantLock`의 Condition에서 대기하는 쓰레드(WAITING)의 주요 차이점
>
> * `synchronized` 락 획득 대기 상태는 일반적으로 인터럽트가 불가능하지만 `ReentrantLock`의 Condition에서 `await()`으로 대기하는 쓰레드는 인터럽트가 될 수 있다.

#### BlockingQueue

* 생산자/소비자 문제, 한정된 버퍼 문제라고 잘 알려진 문제를 매우 효율적으로 해결할 수 있는 자료구조
* 단순 큐 기능을 넘어서 쓰레드를 효과적으로 제어하는 기능도 포함된다.
* 쓰레드 관점에서 보면 큐가 특정 조건이 만족될 때까지 쓰레드 작업을 차단한다.
  * 데이터 추가 차단 : 큐가 가득 차면 추가 작업을 시도하는 쓰레드(생산자 쓰레드)가 공간이 생길 때까지 차단된다.
  * 데이터 획득 차단 : 큐가 비어 있으면 획득 작업을 시도하는 쓰레드(소비자 쓰레드)는 큐에 데이터가 들어올 때까지 차단된다.

#### BlockingQueue 기능 정리

```java
public interface BlockingQueue<E> extends Queue<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted queue, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean add(E e);

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted queue, this method is
     * generally preferable to {@link #add}, which can fail to insert an
     * element only by throwing an exception.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean offer(E e);

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    void put(E e) throws InterruptedException;

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    E take() throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking, or {@code Integer.MAX_VALUE} if there is no intrinsic
     * limit.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     *
     * @return the remaining capacity
     */
    int remainingCapacity();

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this queue
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object o);

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this queue
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean contains(Object o);

    /**
     * Removes all available elements from this queue and adds them
     * to the given collection.  This operation may be more
     * efficient than repeatedly polling this queue.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *         is not supported by the specified collection
     * @throws ClassCastException if the class of an element of this queue
     *         prevents it from being added to the specified collection
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this
     *         queue, or some property of an element of this queue prevents
     *         it from being added to the specified collection
     */
    int drainTo(Collection<? super E> c);

    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given collection.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *         is not supported by the specified collection
     * @throws ClassCastException if the class of an element of this queue
     *         prevents it from being added to the specified collection
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this
     *         queue, or some property of an element of this queue prevents
     *         it from being added to the specified collection
     */
    int drainTo(Collection<? super E> c, int maxElements);
}
```

* 큐가 가득 찼을 때 생각할 수 있는 선택지로 4가지가 있다.

1. 예외를 던진다. 예외를 받아서 처리한다.
2. 대기하지 않는다. 즉시 `false`를 반환한다.
3. 대기한다.
4. 특정 시간만큼만 대기한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d5a42f75-c065-40d9-b030-65559aa7959f.png)
