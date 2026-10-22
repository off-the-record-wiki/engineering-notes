# ElasticSearch - Managing Documents (2)

#### Create — 문서 생성

```shell
POST /my_index/_doc/100
{
  "title": "Elasticsearch Advanced",
  "author": "Anthony K",
  "publish_date": "2024-05-20",
  "tags": ["search", "analytics"]
}
```

* 경로는 `/{인덱스}/_doc/{문서 ID}` 형태다.
* 본문은 JSON 문서 전체를 그대로 보낸다.

#### Retrieve — 문서 조회

```shell
GET /my_index/_doc/100
```

* 경로 마지막의 `100`이 **Document Id**다.
* ID를 알면 해당 문서 하나를 바로 가져온다.

#### Update — 문서 수정

**기존 필드 수정**

```shell
POST /my_index/_update/100
{
  "doc": {
    "author": "Anthony K2"
  }
}
```

**새 필드 추가**

```shell
POST /my_index/_update/100
{
  "doc": {
    "price": 100
  }
}
```

* `_doc`이 아니라 `_update` 엔드포인트를 쓴다.
* 바꿀 내용을 `doc` 안에 담는다. 기존 필드 수정과 새 필드 추가가 같은 문법이다.

#### 문서는 불변(IMMUTABLE)이다

* 수정 요청이 들어와도 문서를 제자리에서 고치지 않는다. Elasticsearch 내부에서는 아래 순서로 처리한다.

1. **기존 문서 조회(Fetch)** — 인덱스에서 현재 버전의 문서를 가져온다.
2. **변경 적용(Apply Changes)** — 가져온 문서에 요청한 변경을 반영한다.
3. **새 문서로 색인(Index)** — 수정된 문서를 **새 버전으로** 색인한다.
4. **기존 문서 삭제 표시(Mark as Deleted)** — 이전 버전은 **논리적 삭제(logical deletion)** 처리한다. 인덱스에 남아 있되 삭제 플래그가 붙고 검색 대상에서 제외된다.
5. **세그먼트 병합(Segment Merging)** — 백그라운드 작업이 삭제 표시된 문서를 물리적으로 제거하고 공간을 회수한다.

#### 불변성의 이점

* **일관성(Consistency)** — 제자리 수정을 하지 않으므로 **분산 환경에서 race condition과 데이터 손상을 피할** 수 있다.
* **버전 관리(Versioning)** — 수정할 때마다 **새 버전이 생기므로** 동시 수정 관리가 쉽고, optimistic concurrency control 같은 기능을 제공할 수 있다.
* **성능(Performance)** — Elasticsearch는 **append-only 워크로드에 최적화**되어 있어 색인과 검색 성능에 유리하다.

#### Delete — 문서 삭제

```shell
# delete a document
DELETE /my_index/_doc/100

# flush a document
POST /my_index/_flush
```

* `DELETE`도 즉시 물리 삭제가 아니라 **삭제 표시**다. 실제 제거는 세그먼트 병합 시점에 일어난다.
* `_flush`는 메모리의 변경분을 디스크에 반영한다.

#### Script란

* 스크립트는 **문서 색인, 수정, 검색 과정에서 더 복잡한 연산과 계산을 수행**할 수 있게 해준다.

#### 스크립트의 종류

* **Inline Scripts** — 요청 안에 직접 작성하는 스크립트다.
* **Stored Scripts** — 서버에 저장해두고 고유 ID로 참조하는 스크립트다.
* **File Scripts** — 각 노드의 `config/scripts` 디렉터리에 저장하는 방식이다. 보안상의 이유로 **잘 쓰이지 않고 권장되지도 않는다**.

#### 스크립트 언어

* Elasticsearch는 여러 스크립트 언어를 지원하지만, **Painless가 가장 권장되며 안전하다**.
* Groovy, JavaScript, Python도 사용할 수 있으나 **성능과 보안 측면에서 Painless가 우선된다**.

**문서 수정 예시**

```shell
POST /my_index/_update/100
{
  "script": {
    "source": "ctx._source.price += 10"
  }
}
```

* `ctx._source`로 현재 문서에 접근해 값을 직접 계산한다.

**커스텀 스코어링 예시**

```shell
POST /my_index/_search
{
  "query": {
    "function_score": {
      "query": {
        "match_all": {}
      },
      "script_score": {
        "script": {
          "source": "doc['price'].value * _score"
        }
      }
    }
  }
}
```

#### 보안 고려사항

* **Painless를 사용한다** — 보안과 성능 모두에서 유리하다.
* **스크립팅 권한을 신뢰할 수 있는 사용자로 제한한다.**
* 필요하지 않다면 **운영 환경에서 동적 스크립팅을 비활성화한다.**
* 공통적이고 재사용되는 스크립트는 **stored script로 만들어 공격 표면을 줄인다.**

#### Upsert란

* Elasticsearch의 upsert는 **update와 insert를 결합한** 연산이다.
* 지정한 ID의 문서가 **있으면 수정(update)** 한다.
* **없으면 제공한 내용으로 새 문서를 생성(insert)** 한다.

#### doc + upsert

```shell
POST /my_index/_update/5
{
  "doc": {
    "author": "John Doe"
  },
  "upsert": {
    "title": "Elasticsearch Basics",
    "author": "Jane Doe",
    "publish_date": "2024-05-09",
    "tags": ["search", "analytics"]
  }
}
```

* **`doc`** — 문서가 **이미 있을 때** 적용할 변경 내용이다. 여기서는 `author`만 바뀐다.
* **`upsert`** — 문서가 **없을 때** 새로 생성할 문서 전체다.
* 즉 문서 존재 여부에 따라 둘 중 하나만 사용된다.

#### script + upsert

```shell
POST /my_index/_update/7
{
  "script": {
    "source": "ctx._source.price++"
  },
  "upsert": {
    "title": "Elasticsearch Basics",
    "author": "Jane Doe",
    "publish_date": "2024-05-09",
    "price": 100,
    "tags": ["search", "analytics"]
  }
}
```

* **`script`** — 문서가 있을 때 실행할 로직이다. 기존 값을 읽어 계산한다(`price++`).
* **`upsert`** — 문서가 없을 때 생성할 초기 문서다. 초기값 `price: 100`을 함께 넣어둔다.
* 카운터처럼 **기존 값을 증가시키되 첫 요청에서는 초기값을 세워야 하는** 경우에 쓰는 형태다.

#### Replace — 문서 전체 교체

```shell
PUT /my_index/_doc/20
{
  "title": "Elasticsearch Basics",
  "author": "John Doe",
  "publish_date": "2024-05-09",
  "tags": ["search", "analytics"]
}
```

* 같은 ID에 다시 `PUT`을 보내면 문서 전체가 교체된다.
* `PUT /{index}/_doc/{id}`는 **부분 수정이 아니라 전체 덮어쓰기**다.
* 요청 본문에 없는 필드는 사라진다.

```shell
PUT /my_index/_doc/20
{
  "title": "Updated Elasticsearch Basics",
  "author": "Jane Doe",
  "publish_date": "2024-06-01",
  "tags": ["search", "analytics", "updated"]
}
```

#### Routing이란

* Routing은 **인덱스 안에서 문서가 shard들에 어떻게 분산되는지를 결정하는** 메커니즘이다.
* 제대로 활용하면 **Elasticsearch 작업의 성능과 효율을 크게 개선**할 수 있다. 특히 대용량 데이터셋이나 특정한 쿼리 패턴에서 효과가 크다.

#### 두 가지 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/47c35951-65b0-4a54-b540-2b0f9975366c.png)

* **Default Routing** — 문서 ID를 해싱해 shard를 정한다.
* **Custom Routing** — shard를 결정할 **커스텀 값(routing key)** 을 직접 지정한다. 더 효율적인 쿼리가 가능해진다.

#### Custom Routing의 이점

* **쿼리 성능 향상(Improved Query Performance)**
  * 연관된 문서들이 **같은 shard에 저장되도록** 보장하면, Elasticsearch가 질의해야 할 shard 수를 줄일 수 있어 응답이 빨라진다.
* **데이터 지역성(Data Locality)**
  * 연관 데이터를 함께 저장하므로, **멀티테넌시나 데이터 분할 시나리오**를 가진 애플리케이션에 특히 유용하다.

#### 사용 예시

**색인할 때 routing key 지정**

```shell
POST /my_index/_doc/1?routing=user123
{
  "title": "Elasticsearch Basics",
  "author": "John Doe",
  "publish_date": "2024-05-09",
  "tags": ["search", "analytics"]
}
```

**검색할 때 같은 routing key 지정**

```shell
GET /my_index/_search?routing=user123
{
  "query": {
    "match": {
      "author": "John Doe"
    }
  }
}
```

* 쿼리 파라미터 `?routing=` 으로 값을 넘긴다.
* 색인과 검색에 **같은 키**를 쓰면, 검색이 모든 shard가 아니라 해당 shard만 조회한다.

#### 고려사항

* **shard 불균형(Shard Imbalance)**
  * custom routing을 조심해서 쓰지 않으면 **데이터가 고르지 않게 분포**할 수 있다. routing key가 shard 전체에 고르게 퍼지는 값인지 확인해야 한다.
* **routing key 변경(Routing Key Changes)**
  * 문서를 routing key와 함께 색인하고 나면, **문서를 재색인하지 않고는 routing key를 바꿀 수 없다.**

#### 읽기 요청의 처리 순서

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c460c434-d100-455d-98dc-5c9b48395f71.png)

* **Client Request** — 클라이언트가 Elasticsearch 노드(coordinating node)로 요청을 보낸다. 검색 쿼리, ID로 문서 조회, 집계 등이 모두 해당한다.
* **Coordinating Node** — 요청 처리를 담당한다. 요청을 파싱해 **어떤 인덱스와 shard를 조회해야 하는지 판단**한다.
* **Routing** — 쿼리에 관련된 각 shard에 대해, coordinating node가 **적절한 shard 복사본으로 요청을 전달**한다. 각 인덱스는 shard로 나뉘고 각 shard는 primary 하나와 0개 이상의 replica를 가지므로, **어느 복사본을 조회할지 결정**해야 한다.
* **Shard Level Execution** — 요청이 primary shard 또는 replica shard 중 하나로 전달된다(연산 종류와 부하 분산 설정에 따라 달라진다). 해당 shard가 **로컬에서** 쿼리나 조회를 수행한다.
* **Aggregation and Reduction** — 검색 쿼리의 경우 각 shard가 쿼리를 실행하고 결과를 coordinating node로 돌려준다. coordinating node는 이 결과들을 **취합해**(검색 결과 병합, 최종 집계 수행) 완전한 응답을 만든다.
* **Response to Client** — coordinating node가 최종 취합된 응답을 클라이언트에 보낸다.

#### Adaptive Replica Selection (ARS)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/11134184-8d3b-4cd1-b574-ceb82b0fdca5.png)

* **읽기 요청을 처리할 최적의 shard 복사본(primary 또는 replica)을 동적으로 선택**해 읽기 성능과 효율을 개선하도록 설계된 기능이다.
* 이 선택은 **노드의 현재 성능과 부하 지표**를 근거로 이루어진다.

**동작 방식**

* **지표 수집(Metrics Collection)** — Elasticsearch는 각 shard 복사본(primary와 replica 모두)에 대해 **응답 시간, 부하, 큐 크기** 같은 지표를 수집한다.
* **동적 선택(Dynamic Selection)** — 읽기 요청이 오면 특정 복사본으로 **정적으로 라우팅하지 않고**, coordinating node가 이 지표들을 이용해 **가장 빠른 응답이 기대되는 복사본을 동적으로 선택**한다.

**효과**

* **부하 분산(Load Balancing)** — 가장 덜 바쁘거나 가장 빠르게 응답하는 shard를 고르므로, 노드 간 부하가 더 효과적으로 분산되고 전체 시스템 성능이 최적화된다.
* **지연 개선(Improved Latency)** — 읽기 요청을 가장 응답성이 좋은 복사본으로 보내므로, 지연이 줄고 클러스터의 응답성이 향상된다.

#### 쓰기 요청의 처리 순서

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a870a66d-1aa2-45e6-b727-e5d2346150cb.png)

* **Client Request** — 클라이언트가 문서를 쓰기 위해 Elasticsearch 노드(coordinating node)로 색인 요청을 보낸다. 요청에는 **문서 내용과 저장할 인덱스**가 담긴다.
* **Document Routing** — coordinating node가 **어느 shard가 이 문서를 담아야 하는지** 결정한다. 기본적으로 **문서 ID의 해시**로 shard를 정하며, custom routing을 설정할 수도 있다.
* **Primary Shard Processing** — 요청이 **해당 문서를 담당하는 primary shard로 전달**된다. 인덱스가 샤딩되어 있으면 각 문서는 routing에 따라 하나의 primary shard에 배정된다.
  * primary shard가 쓰기를 처리하고 **sequence number를 부여**한다.
  * 현재 primary shard의 **primary term이 함께 포함**된다.
  * primary shard는 연산을 영속화한 뒤 **local checkpoint를 갱신**한다.
* **Replication** — primary shard가 쓰기를 처리하고 나면, **sequence number와 primary term을 포함해** 모든 replica shard로 연산을 전달한다. 각 replica가 쓰기를 처리하고 자신의 local checkpoint를 갱신한다. **primary와 모든 replica가 쓰기를 확인해야만** 쓰기 연산이 성공으로 간주된다.
* **Global Checkpoint Update** — primary shard가 모든 replica의 local checkpoint를 추적한다. **in-sync 상태인 replica 전부가 쓰기를 확인하면**, primary shard가 global checkpoint를 **모든 replica가 확인한 가장 높은 sequence number**로 갱신한다.
* **Acknowledgment** — coordinating node가 primary와 모든 replica의 확인 응답을 기다린다. 전부 확인되면 클라이언트에 성공 응답을 보낸다.

#### 핵심 개념 네 가지

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f4adc451-c9d4-4eb6-9b23-8f30eeb439a8.png)

| 개념 | 의미 |
|---|---|
| **Translog** | write-ahead log 역할을 한다 |
| **Checkpoint** | translog에서 커밋 성공 지점을 표시하는 marker |
| ├ **Global Checkpoint** | shard의 **in-sync 복사본 전체**가 도달한 지점 |
| └ **Local Checkpoint** | **개별 shard 하나**가 도달한 지점 |
| **Primary Term** | primary shard가 **재할당될 때마다 증가**하는 카운터 |
| **Sequence Number** | 연산을 **순서대로 처리**하기 위한 카운터 |

#### Global Checkpoint의 진행

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ef5f958b-771b-4f5e-8229-b89b4fa2ed71.png)

**1단계 — 첫 쓰기**

```
Global checkpoint: 99
Primary:   99 → 100
Replica1:  99 → 100
Replica2:  99 → 99 (in progress)
```

**2단계 — 두 번째 쓰기**

```
Global checkpoint: 99
Primary:   99 → 100 → 101
Replica1:  99 → 100 → 101
Replica2:  99 → 99 (in progress)
```

**3단계 — 뒤처진 replica가 따라잡음**

```
Global checkpoint: 99 → 101
Primary:   99 → 100 → 101
Replica1:  99 → 100 → 101
Replica2:  99 → 99 → 100 → 101
```

* primary와 replica1이 101까지 앞서가도, **replica2가 99에 머물러 있는 동안 global checkpoint는 99에서 움직이지 않는다.**
* replica2가 101까지 따라잡은 뒤에야 global checkpoint가 101로 올라간다.
* 즉 global checkpoint는 **가장 뒤처진 in-sync 복사본**이 결정한다. 이 지점까지는 모든 복사본이 동일함이 보장되므로, 장애 복구 시 어디부터 재전송해야 하는지의 기준이 된다.

#### 동시성 처리가 왜 필요한가?

* Elasticsearch에서 동시성 문제를 다루는 것은 **데이터 일관성과 정확성을 보장하기 위해** 반드시 필요하다. 특히 처리량이 많은 환경에서 중요하다.

#### 문제 상황 — 갱신 유실(lost update)

* 두 클라이언트가 같은 문서를 동시에 다룰 때 일어나는 일이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7098663a-54da-4800-a242-375400d926f4.png)

* 각자 자기가 읽은 값(10)을 기준으로 계산했기 때문에, **나중 쓰기가 앞선 쓰기를 덮어쓴다.**

#### Optimistic Concurrency Control (OCC)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/91eb8b4c-0c25-4327-919a-0634a25ddb3f.png)

* OCC는 Elasticsearch가 **여러 클라이언트의 동시 읽기는 허용하되, 특정 시점에 쓸 수 있는 클라이언트는 하나뿐임을 보장**하는 방식이다.
* **`if_primary_term`** 과 **`if_seq_no`** 로 구현한다.

```shell
POST /store/_update/1?if_primary_term=1&if_seq_no=3
```

* **성공** — 문서가 여전히 `_seq_no: 3`, `_primary_term: 1`이면 갱신된다.
* **실패** — 그 사이 다른 클라이언트가 문서를 바꿔 `_seq_no`가 달라졌다면 거부되고 **`version_conflict_engine_exception`** 오류가 발생한다.
* 즉 "내가 읽은 그 버전이 아직 그대로일 때만 써라"는 조건부 쓰기다.

#### Update multiple documents

* 쿼리를 기준으로 문서들을 수정하려면 **`_update_by_query` API**를 사용한다.

**전체 문서 수정 — 쿼리 없이**

```shell
POST /store/_update_by_query
{
  "script": {
    "source": "ctx._source.stock -= 1"
  }
}
```

* `query`를 생략하면 **인덱스의 모든 문서**에 스크립트가 적용된다.

**조건에 맞는 문서만 수정 — 쿼리 지정**

```shell
POST /store/_update_by_query
{
  "script": {
    "source": "ctx._source.stock -= 1"
  },
  "query": {
    "term": {
      "product": "peach"
    }
  }
}
```

* `query`를 함께 주면 **매칭된 문서에만** 스크립트가 적용된다.
* 여기서는 `product`가 `peach`인 문서들의 `stock`만 1씩 줄어든다.

#### Delete multiple documents

* 쿼리를 기준으로 문서들을 삭제하려면 **`_delete_by_query` API**를 사용한다.

```
POST /store/_delete_by_query
{
  "query": {
    "term": {
      "product": "apple"
    }
  }
}
```

* `product`가 `apple`인 문서들이 삭제된다.

#### Bulk란

* Elasticsearch의 bulk action은 **여러 건의 index, update, delete, create 연산을 하나의 API 호출로 수행**하는 방법이다.
* **개별 요청을 여러 번 보내는 오버헤드를 줄여 효율적**이다. 대용량 데이터 색인, 다건 문서 수정, 문서 일괄 삭제에 특히 유용하다.

#### 네 가지 Action Type

| Action | 동작 | 문서가 이미 있으면 |
|---|---|---|
| **Index** | 문서를 추가하거나 갱신한다 | **교체(replace)** 된다 |
| **Update** | 문서를 부분 수정한다 | — (없으면 **upsert**로 생성 가능) |
| **Delete** | ID로 문서를 삭제한다 | — |
| **Create** | 새 문서를 추가한다 | **실패**한다 |

* **Index** — 있으면 덮어쓰고 없으면 만든다. 존재 여부를 신경 쓰지 않을 때 쓴다.
* **Update** — 지정한 필드만 바꾼다. 문서가 없을 때를 대비하려면 upsert를 함께 쓴다.
* **Delete** — 문서 ID를 지정해 삭제한다.
* **Create** — 새로 만드는 경우에만 성공한다. **중복 생성을 막고 싶을 때** 쓴다.
