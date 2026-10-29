# Cache - Cache Stampede

#### Cache Stampede

![](https://github.com/user-attachments/assets/02861a9e-7c44-4595-a46f-a3e94d516f07)

* 정의 : 다수의 요청에서 동시에 캐시 미스가 발생하고 그 부하가 Data Source로 그대로 전파되는 현상을 말한다.
* 발생 상황 : 요청이 많이 몰리는 키(Hot Key)가 캐시에서 비는 순간 생긴다. 주로 TTL 만료, eviction, 배포 직후처럼 캐시가 비어 있을 때 발생한다.
* 흐름 : 동시 요청이 모두 캐시 미스 → 모두 Data Source 조회 → 모두 같은 값을 캐시에 중복 저장
* 문제
  * Data Source에 부하가 순간적으로 몰려 응답이 저하되고 심하면 연쇄 장애로 번진다.
  * 같은 값을 여러 번 조회하고 저장하는 낭비가 발생한다.
* 캐시 관통은 없는 키라서 캐시에 영원히 저장되지 않고 캐시 스탬피드는 있는 키인데 캐시가 잠깐 비는 순간에 동시 요청이 몰리는 것이다.

#### Jitter
