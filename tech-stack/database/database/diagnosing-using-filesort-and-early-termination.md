# Database - Diagnosing Using filesort and Early Termination

#### 복합 인덱스 범위 조건과 물리적 정렬 한계

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d13f66b5-0d53-4447-9b1c-e780d5bfa1a0.png)

* 복합 인덱스 `(col1, col2, col3)`에서 **첫 번째 컬럼에 범위 조건**을 적용하면, 그 뒤의 컬럼들은 인덱스의 정렬 특성을 활용할 수 없다.
* **인덱스 구조상 정렬 순서는 왼쪽부터 오른쪽으로 적용된다.**

#### LIMIT 쿼리 성능 핵심 : 조기 종료(Short-circuiting)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/08244d28-5a73-4e99-bf6b-af3e72c3a643.png)

* 정렬 상태가 깨졌을 때 : `LIMIT 20`이 걸리더라도 전체 조건에 부합하는 대량의 데이터를 인덱스에서 끝까지 읽어 들이고 정렬 큐에 밀어 넣어야 비로소 상위 20건을 알 수 있다. 결과적으로 `LIMIT`가 데이터 스캔 범위를 줄여주는 혜택을 전혀 받지 못한다.
* 정렬 상태가 유지될 때 : 인덱스로 인해 이미 순서대로 정렬되어 있으므로 앞에서부터 부분적으로 훑어서 필터 조건을 만족하는 20건이 채워지는 순간 나머지 데이터를 읽을 필요 없이 그 자리에서 쿼리를 즉시 종료한다. 참고로 인덱스는 순서대로 앞으로도, 뒤로도 탐색이 가능하다. 뒤로 탐색하는 경우 `Backward index scan`이 나온다.
