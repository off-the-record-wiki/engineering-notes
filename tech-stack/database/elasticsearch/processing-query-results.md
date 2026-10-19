# ElasticSearch - Processing Query Results

#### 응답 형식

* Elasticsearch는 API 요청과 응답의 형식으로 **주로 JSON을 사용**하지만, **요청과 응답 모두 YAML도 지원**한다.

```shell
GET /library/_search?format=yaml
```

* 추가로, **`pretty` 파라미터**를 쓰면 **JSON 출력을 사람이 읽기 좋은 형태로** 포맷팅할 수 있다.

```shell
GET /library/_search?pretty
```

| 파라미터 | 하는 일 |
|---|---|
| (기본) | JSON. 한 줄로 압축된 형태 |
| `?format=yaml` | 응답을 YAML로 받는다 |
| `?pretty` | JSON을 들여쓰기해서 읽기 좋게 만든다 |

#### Pagination이란

* Elasticsearch의 pagination은 **많은 검색 결과를 다루기 좋은 작은 덩어리로 쪼개는** 기능이다.
* 가장 흔한 구현 방법은 검색 질의에 **`from`과 `size` 파라미터**를 쓰는 것이다.

#### from과 size

* **`from`** — 결과 목록에서 **몇 번째부터** 가져올지 지정한다. **0부터 시작**한다.
* **`size`** — **몇 건을 가져올지** 지정한다. **기본값은 10**이다.

```shell
GET /library/_search
{
  "from": 0,
  "size": 10,
  "query": { "match_all": {} }
}
```
