# ElasticSearch - Aggregations

#### Metric Aggregations란?

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2b7432da-f8a5-4677-92a0-16ee74b14b36.png)

* **숫자 필드에 대해 `sum`, `average`, `min`, `max`, `stats` 같은 지표를 계산**한다.
* 여러 문서의 값이 **하나의 숫자로 집계**된다.

| 이름 | 계산 |
|---|---|
| `sum` | 합계 |
| `avg` | 평균 |
| `min` | 최솟값 |
| `max` | 최댓값 |
| `stats` | count, min, max, avg, sum을 **한 번에** |
| `value_count` | 값이 있는 문서 수 |
| `cardinality` | 고유값 개수 (근사) |

```shell
GET /products/_search
{
  "size": 0,
  "aggs": {
    "avg_price": {
      "avg": { "field": "price" }
    }
  }
}
```

#### Bucket Aggregations란?

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/138bae37-99ec-49e6-9394-dfa22aeb8291.png)

* bucket aggregation은 **어떤 기준에 따라 문서를 버킷으로 묶는** 기능이다.
* 각 버킷은 **공통된 특성을 공유하는 문서 그룹**을 나타낸다. 특성은 **필드 값, 값의 범위**, 또는 더 복잡한 기준일 수 있다.

#### Bucket Aggregation의 종류

* **Terms Aggregation** — 지정한 필드의 **고유값을 기준으로 문서를 묶는다.** 즉 **`GROUP BY`** 에 해당한다.
* **Range Aggregation** — **미리 정의한 숫자 범위**를 기준으로 문서를 버킷에 담는다.
* **Histogram Aggregation** — range aggregation과 비슷하지만, **지정한 간격(interval)에 따라 균등한 버킷을 자동으로 만든다.**
* **Date Histogram Aggregation** — **일, 월, 년 같은 날짜 간격**으로 문서를 묶는다.
* **Filter and Filters Aggregation** — `filter`는 주어진 필터에 맞는 문서로 **버킷 하나**를 만들고, `filters`는 **여러 필터**를 두어 **각각이 자기 버킷**을 만든다.
* **Missing Aggregation** — 지정한 **필드가 없는 문서들로** 버킷을 만든다.
* **Nested Aggregation** — **nested object 안의 필드에 대해** 집계를 수행한다.
* **Global Aggregation** — **질의의 필터를 무시하고 인덱스의 모든 문서를 담는** 버킷을 만든다.
