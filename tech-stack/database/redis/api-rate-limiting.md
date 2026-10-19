# Redis - API Rate Limiting

## 📚 Sorted Set 자료 구조 (ZCARD, ZREMRANGEBYSCORE, EXPIRE)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e12b41b0-dfec-4f4b-b05c-4592bd36e476.png)

* 요청이 들어올 때마다 Sorted Set에서 Member에는 각 요청을 구분할 수 있는 고유의 값을 넣고, Score에는 요청을 보낸 시간을 unixtime으로 넣는다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e0b605b8-d99d-4313-ab1b-7e675fa642f4.png)

* 현재 시간(unixtime)에서 ‘Score에 저장되어 있는 요청을 보낸 시간(unixtime)’을 빼서 10,000(10초)을 넘는 값은 전부 삭제한다.
* 최근 10초에서 발생한 API 요청 횟수를 카운팅해서 제한하는 방식이기 때문에, 현재 시간을 기준으로 10초가 넘어간 요청은 삭제해준다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3a35ec79-f1aa-44a9-a6c3-a737283dbc09.png)

* 현재 시간을 기준으로 10초가 넘어간 요청은 삭제했기 때문에, 남아있는 요청은 10초 이내의 요청들이다. 따라서 남아있는 요청의 개수가 곧 최근 10초 동안 사용자가 보낸 요청의 개수이다.
* 이런 방식으로 사용자가 보낸 요청의 개수로 API 요청을 허용하거나 제한한다.
