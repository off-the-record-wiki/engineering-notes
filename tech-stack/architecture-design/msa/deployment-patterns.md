# MicroService Architecture - Deployment Patterns

#### Docker Containers / Images / Registries

* Dockerfile : 어떤 환경을 만들지 적은 설계도
* Image : 그 설계도로 구워낸 읽기 전용 스냅샷. 불변이라 어디서 띄워도 똑같이 동작
* Container : 이미지 위에 쓰기 레이어를 얹어 실제로 돌아가는 프로세스

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/03380fdc-2029-40da-9d91-21d156a9fca7.png)

#### Deployment Strategies

* Blue-Green : 마이크로서비스의 이전 버전(블루 배포)이 계속 실행되는 동안 새로운 마이크로서비스 집합(그린 배포)에 업데이트 배포
* Rolling Update : 마이크로서비스 하위 집합에 업데이트를 배포 ⭢ 시간이 지남에 따라 새로운 버전으로 교체
* Canary : 마이크로서비스 하위 집합에 업데이트를 배포 ⭢ 시간이 지남에 따라 나머지 마이크로서비스에 업데이트를 점진적으로 롤아웃
* A/B Test : 마이크로서비스 하위 집합에 업데이트를 배포 ⭢ 업데이트된 마이크로서비스의 성능을 수정되지 않은 마이크로서비스의 성능과 비교

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0df6c898-a423-4803-ae0a-526b05144cea.png)

#### Infrastructure as Code(IaC)

* 애플리케이션 코드처럼 버전 관리 ⭢ 코드 베이스를 이용해 인프라 구축/관리
* 자동화되고 반복 가능한 방식으로 인프라 관리 ⭢ 마이크로서비스를 실행하는 데 필요한 인프라를 배포, 관리하는 프로세스 자동화
