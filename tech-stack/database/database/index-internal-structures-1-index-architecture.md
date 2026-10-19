# Database ‐ Index Internal Structures 1 ‐ B-Tree & B+Tree

## Database ‐ Index Internal Structures 1 ‐ B-Tree & B+Tree

#### 트리 자료구조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/05ba8727-bb53-4776-8f61-ffc0a85b4e15.png)

* 트리는 부모 노드와 자식 노드로 구성된다.
* 가장 높은 조상을 루트(root)라고 한다.
* 자식이 2개까지 올 수 있는 트리를 이진 트리(Binary Tree)라고 한다.
* 여기에 노드의 왼쪽 자손은 더 작은 값을 가지고, 오른쪽 자손은 더 큰 값을 가지는 형태를 이진 탐색 트리(Binary Search Tree, BST)라고 한다.

#### 이진 탐색 트리 입력 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f8056c28-0bf5-4a42-8148-5df641bee0ed.png)

* 처음에 10을 입력했다고 가정하고 다음으로 5, 15를 입력한다.
* 5 저장 : 5는 10보다 작으므로 왼쪽에 저장된다.
* 15 저장 : 15는 10보다 크므로 오른쪽에 저장된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3971126a-f0d6-4131-9b7f-d05a33048c66.png)

* 1 저장 : 1은 10보다 작다. 왼쪽으로 찾아간다. 1은 5보다 작다. 결국 왼쪽에 저장된다.
* 6 저장 : 6은 10보다 작다. 왼쪽으로 찾아간다. 6은 5보다 크다. 결국 오른쪽에 저장된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4c5f9fc9-0e50-498d-b6b0-f800a67c12bd.png)

* 11 저장 : 11은 10보다 크다. 오른쪽으로 찾아간다. 11은 15보다 작다. 결국 왼쪽에 저장된다.
* 16 저장 : 16은 10보다 크다. 오른쪽으로 찾아간다. 16은 15보다 크다. 결국 오른쪽에 저장된다.

#### 이진 탐색 트리 검색 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/70f429ae-49f1-4711-947d-64ee5e15bb03.png)

* 총 15개의 데이터가 들어있다. 여기서 숫자 35를 찾는다고 가정하자.
* 1번 : 루트인 20과 35를 비교한다. 35가 더 크므로 오른쪽으로 찾아간다.
* 2번 : 40과 35를 비교한다. 35가 더 작으므로 왼쪽으로 찾아간다.
* 3번 : 30과 35를 비교한다. 35가 더 크므로 오른쪽으로 찾아간다.
* 4번 : 노드에 있는 값을 비교한다. 35와 같으므로 35를 찾는다.

#### 이진 탐색 트리의 Big-O - `O(log N)`

* 수학적으로 `O(log N)`이 성립한다.
* 이진 탐색 트리의 핵심은 입력 순서가 아니라 데이터의 값을 기준으로 정렬해서 보관한다.
* 정렬해서 보관했기 때문에 정렬된 순서로 데이터를 차례로 조회할 수 있다.
* 데이터를 차례로 순회하려면 중위 순회 방법을 사용하면 된다.

#### 밸런스 트리(Balanced Tree)

* 이진 탐색 트리의 경우 검색, 삽입, 삭제의 평균 성능은 `O(log N)`이다. 하지만 트리 균형이 맞지 않으면 최악의 경우 `O(N)`의 성능이 나온다.
* 이런 문제를 해결하기 위해 다양한 해결 방법들이 있는데 트리의 균형이 너무 깨진 경우 동적으로 균형을 다시 맞추는 것이다.

#### B+Tree 내부 구조

* 이진 트리는 한 노드에 데이터 1개, 자식 포인터 1개로 총 2개를 담는다. 데이터베이스는 디스크에서 데이터를 한 번 읽을 때 무조건 16KB 페이지 단위로 읽어온다.
* 이진 트리 구조에서는 16KB를 통째로 읽어와도 고작 데이터 1개만 쓰고 나머지는 텅 빈 채로 버려야 한다. 너무 비효율적이다.
* 이 낭비를 막기 위해 등장한 것이 B-Tree이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ad3a258d-0038-458f-b9e3-05bfffde7170.png)

* 결국 트리 높이 = 디스크 I/O 횟수가 된다. 검색은 루프에서 리프까지 한 층씩 내려가며 페이지를 한 장씩 읽는 작업이다.
* 예를 들어, 높이가 10층이면 I/O 10번, 3층이면 I/O 3번이다. B-Tree는 한 번 페이지를 읽을 때마다 수백 개의 갈림길을 한꺼번에 확인하니 단 몇 번의 읽기로 원하는 데이터에 도달할 수 있다.
* 페이지를 그냥 읽어 오든, 꽉 채워 읽어 오든 I/O 비용은 같다. 그렇다면 한 페이지를 꽉꽉 채운 뒤 갈림길을 최대한 많이 만들어 두는 쪽이 당연히 유리하다.

#### B-Tree 약점

* 하지만 B-Tree에는 구조적인 약점이 있는데 바로 트리의 모든 노드에 실제 데이터를 함께 저장한다는 점이다.
* 만약 테이블의 행 1개 크기가 1KB에 달하는 대용량 데이터라고 가정하면 16KB 페이지 하나에는 고작 16건의 데이터밖에 들어가지 못한다. 노드 하나에 16건의 데이터가 들어가면, 여기서 뻗어나갈 수 있는 자식 노드 역시 16개로 뚝 떨어진다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/75c54879-6916-4db2-ab8b-f39fee2ddcd4.png)

#### B+Tree 내부 구조 1

* 트리 상단과 중간에는 무거운 실제 데이터를 다 빼버리고 가벼운 검색 키만 남기고 무거운 실제 데이터는 리프 노드에 넣는 구조가 바로 B+Tree이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5a14ff4f-84b1-48cd-aa59-87b9ca251269.png)

* 모든 인덱스 검색 요청은 반드시 루프와 브랜치 노드를 거쳐야만 리프 노드로 갈 수 있다.
* 데이터베이스의 캐시 관리 알고리즘은 자주 사용되는 페이지를 메모리에 남겨두고 오랫동안 사용되지 않는 페이지를 메모리에서 몰아낸다. 매초 수천 번씩 두들겨지는 루트와 브랜치 노드는 캐시에서 밀려날 틈이 없다. 사실상 100% 확률로 메모리에 늘 상주하게 된다.
* 비싸고 느린 디스크를 직접 읽어야 하는 진짜 디스크 I/O는 최종적으로 원하는 데이터가 담긴 맨 아래 리프 노드를 읽을 때만 발생한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/aae52e26-3515-43ff-a306-834d557acb44.png)

#### B+Tree : 리프 노드 연결 리스트

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/555b83d2-3753-4f73-a7a5-31d6e286ced8.png)

* B-Tree : 범위 검색을 할 때 인접한 데이터를 찾으려면 트리를 계속 위아래로 오르락내리락 탐색해야 한다.
* B+Tree : 리프 노드들끼리 서로 이중 연결 리스트로 이어져 있다. 하나의 시작점만 찾고 나면, 그 다음부터는 다시 트리 위로 올라갈 필요 없이 리프 노드에 있는 연결선을 따라 옆으로 밀면서 연속적으로 읽어들인다. 범위 검색과 순차 검색에 압도적으로 유리하다.
* B+Tree는 모든 실제 데이터가 오직 리프 노드에만 있다. 모든 데이터가 한 층에 있어 정렬된 순서로 한 줄에 놓는 것이 가능해진다.
* B+Tree는 삽입, 삭제 시 페이지가 꽉 차면 페이지 분할, 텅 비면 병합으로 정렬 상태를 유지한다. 이 때, 새로 생기거나 삭제되는 페이지의 `PREV / NEXT` 포인터를 함께 갱신해서 물리적 연결 순서 = 논리적 정렬 순서가 절대로 깨지지 않도록 관리한다.

#### B+Tree 내부 구조 2
```sql
-- 페이지 크기 확인
SELECT @@innodb_page_size / 1024 AS page_size_kb;
```

* 노드 = 페이지(16KB)이다. B+Tree의 각 노드는 하나의 InnoDB 페이지에 대응한다.
* MySQL 인스턴스를 처음 만들 때 정해지며 이후에는 변경할 수 없다.

#### B+Tree 3층 구조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/606beaec-afd2-4b06-be2a-32d2eeffdd5b.png)

* B+Tree는 루트 노드(Root Node) + 브랜치 노드(Branch Node) + 리프 노드(Leaf Node)의 계층 구조를 가진다.
* 루트 노드 : 트리의 최상단. 검색의 출발점이다. 검색 키 값과 하위 노드 포인터를 저장한다.
* 브랜치 노드 : 중간 계층. 루트와 리프 사이를 연결한다. 역시 키 값과 하위 노드 포인터만 저장한다.
* 리프 노드 : 최하단. 실제 데이터가 여기에만 저장된다. 리프 노드끼리 이중 연결 리스트로 이어져 있어 범위 검색 시 옆으로 읽을 수 있다.

#### 트리 높이 = I/O 횟수

* B+Tree에서 특정 키를 찾으려면 루트에서 리프까지 한 층씩 내려가야한다. 그리고 각 층에서 1번의 페이지 읽기가 발생한다. 따라서 트리 높이가 결국 I/O 횟수가 된다.

```sql
-- innodb_index_stats에서 리프 페이지 수와 전체 페이지 수 확인
SELECT index_name,
       stat_name,
       stat_value,
       stat_description
FROM mysql.innodb_index_stats
WHERE database_name = 'shop'
  AND table_name = 'product'
ORDER BY index_name, stat_name;
```

| index_name | stat_name | stat_value | stat_description |
|---|---|---|---|
| PRIMARY | n_leaf_pages | 약 24,900 | 리프 페이지 수 |
| PRIMARY | size | 약 24,960 | 전체 페이지 수 |
| idx_product_category_id | n_leaf_pages | 약 6,670 | 리프 페이지 수 |
| idx_product_category_id | size | 약 7,670 | 전체 페이지 수 |

#### H = 2계층(루트 + 리프)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/751bb1b5-df81-49ea-a04b-d1d0a3ab582c.png)

* 저장 가능 건수 : 500 x 200 = 약 10만 건
* PK 검색 I/O : 2회(루트 ⭢ 리프)

#### H = 3계층(루트 + 브랜치 + 리프)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/265c4a8e-599b-4557-ad51-48c542bde550.png)

* 저장 가능 건수 : 250,000 x 200 = 약 5,000만 건
* PK 검색 I/O : 3회(루트 ⭢ 브랜치 ⭢ 리프)

#### H = 4계층(루트 + 브랜치 + 브랜치 + 리프)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8bb6bc87-a7f8-4378-8542-13d282efafb1.png)

* 저장 가능 건수 : 1.25억 x 200 = 약 250억 건
* PK 검색 I/O : 4회(루트 ⭢ 브랜치 ⭢ 브랜치 ⭢ 리프)

#### B+Tree 내부 구조 3

* InnoDB의 모든 데이터는 16KB 페이지 단위로 저장된다. 리프 노드가 가득 찬 상태에서 새로운 레코드를 삽입해야 하면, 더 이상 공간이 없기 때문에 페이지 분할이 발생한다. 페이지 분할은 데이터베이스에서 비용이 매우 큰 작업이다.
  * 새로운 디스크 공간 할당 : 새로운 16KB 빈 페이지를 디스크에서 할당받아야 한다.
  * 데이터의 물리적 이동 : 기존 페이지에 있던 레코드의 약 절반을 새로운 페이지로 복사하고 이동시켜야 한다.
  * 트리 구조 업데이트 : 상위 브랜치 노드에 새로운 페이지를 가리키는 포인터를 추가해야 한다. 만약 상위 노드마저 꽉 찼다면 연쇄적 분할이 일어난다.
  * 리두 로그(Redo Log) : 장애 복구를 위해 이 모든 구조적 변경 사항을 리두 로그에 기록해야 하므로 디스크 쓰기가 추가로 발생한다.
* **페이지 분할 지연** : InnoDB는 데이터를 삽입할 때, 페이지를 100% 꽉 채우지 않고 미리 1/16 빈 공간을 남겨둔다. 향후 해당 페이지 중간에 새로운 데이터가 끼어들거나(INSERT), 기존 데이터가 변경되어 길이가 늘어날 때(UPDATE) 페이지 분할이 즉시 발생하는 것을 지연시키기 위해서이다.
* **페이지 병합** : 데이터가 삽입될 때 분할이 일어난다면 반대로 삭제되거나 업데이트로 인해 레코드 크기가 줄어들면 페이지 내부에 빈 공간이 생긴다. 공간 낭비가 심해지면 같은 건수의 데이터를 읽을 때 더 많은 페이지를 디스크에서 읽어와야 하므로, InnoDB는 인접 페이지와 합치는 페이지 병합을 시도한다.

```sql
-- MERGE_THRESHOLD 확인
SELECT t.NAME AS table_name,
       i.NAME AS index_name,
       i.MERGE_THRESHOLD
FROM INFORMATION_SCHEMA.INNODB_INDEXES i
         JOIN INFORMATION_SCHEMA.INNODB_TABLES t
              ON i.TABLE_ID = t.TABLE_ID
WHERE t.NAME IN ('shop/product', 'shop/member', 'shop/orders');
```
* 기본값은 50이다. 즉, 페이지 채움률이 50% 이하로 떨어지면 인접 페이지와 병합을 시도한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/daa4a1ad-4a54-4956-8250-892540b23914.png)

> **❗순차 삽입과 페이지 분할 최적화**
>
> * 페이지 분할이 언제나 50 : 50으로 정확히 반반씩 일어난다면 순차적으로 증가하는 값을 삽입할 때 엄청난 낭비가 생긴다. 1번부터 100번까지 데이터를 넣는데, 꽉 차서 분할될 때마다 기존 페이지의 앞부분(50%)만 채우고 뒷부분은 비워둔 채 새 페이지로 넘어가기 때문이다. 기존 페이지의 비어버린 50%는 영구적으로 낭비된다.
> * InnoDB는 연속적으로 값이 증가하는 패턴을 감지하면 페이지를 50 : 50으로 쪼개지 않는다. 기존 페이지는  15/16 수준까지 채워둔 상태로 놔두고, 새로운 페이지를 바로 옆에 개설해 데이터를 이어나간다. 이를 통해 디스크 공간 낭비를 획기적으로 방지한다.
