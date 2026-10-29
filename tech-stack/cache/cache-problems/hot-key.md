# Cache - Hot Key

#### Hot Key

![](https://github.com/user-attachments/assets/9aa939ab-5a3b-4d01-bb2b-5531dbc6af84)

* 정의 : 접근이 소수의 키에 몰리는 현상이다. 데이터 개수로 보면 Cold Key가 90%로 압도적으로 많은데 접근 빈도로 보면 Hot Key(전체 데이터의 10%)가 전체 접근의 90%를 차지한다.
* Bloom Filter : 없는 데이터에 대한 Hot Key 요청을 바로 끝낸다.
* Jitter : 만료 시점을 서로 다르게 해서 여러 Hot Key가 함께 만료되며 Data Source로 트래픽이 한꺼번에 몰리는 것을 막는다.
* Probailistic Early Recomputation : Hot Key를 확률적으로 미리 갱신해서 Data Source로 트래픽이 한꺼번에 몰리는 것을 막는다.
* Request Collapsing : 같은 Hot Key에 대한 중복 요청을 하나로 합친다.
* Rate Limit : 요청량 자체를 제한한다.
* Write Through : Hot Key를 미리 캐시에 넣어 둔다.
* 남는 문제 : 캐시 서버 부하 불균형
  * 이런 전략들은 캐시 서버 사이의 불균형은 해결하지 못한다. Redis Cluster에서도 하나의 Key는 하나의 노드에서만 처리한다. 키는 해시 슬롯 16,384개 중 하나에 배정되고 각 Shard가 슬롯 범위를 나눠 맡는다. 예를 들면 `0~5460`, `5461~10921`, `10922~16383`가 된다.
  * 데이터는 클러스터 전체에 고르게 나뉘어도 한 노드에 Hot Key가 여러 개 몰리면 그 노드만 부하가 커진다.
  * 노드를 늘려도 키 하나에 오는 트래픽은 나뉘지 않는다. 그래서 그 노드가 CPU나 네트워크 한계에 닿으면 그 노드가 맡은 슬롯 전체가 느려지거나 장애가 발생한다.
