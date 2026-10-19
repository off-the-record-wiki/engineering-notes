# Docker - Container Orchestration

## 📚 Container Orchestration Tool & Docker Swarm

* Orchestration Tools : 컨테이너 배포, 관리, 확장, 네트워킹을 자동화해주는 도구
* 수백, 수천 개의 컨테이너와 호스트를 배포하고 스케줄링하기 위해 사용되는 도구
* 컨테이너 오케스트레이션
  * 프로비저닝 및 배포
  * 구성 및 일정 조정
  * 리소스 할당
  * 컨테이너 가용성
  * 컨테이너 스케일링
  * 로드밸런싱 및 트래픽 라우팅
  * 컨테이너 상태 모니터링

## 📚 [Docker Swarm Practice](https://docs.docker.com/engine/swarm/)

* Manager 노드에서 Swarm 모드 활성화
* Worker 노드를 Manager 노드에 등록

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/249bea5a-55e0-4c88-8bef-4b92c557a2d3.png)

## 📚 [Docker Swarm Service](https://docs.docker.com/engine/swarm/services/)

* 애플리케이션을 구성하는 일부 컨테이너를 제어하기 위한 단위
* Worker 노드가 아닌 Manager 노드에서 실행

## 📚 [Docker Swarm Stack](https://docs.docker.com/engine/swarm/stack-deploy/)

* Stack -> 하나 이상의 서비스를 그룹으로 묶은 단위, 애플리케이션 전체 구성 정의
* Docker Swarm Service는 애플리케이션 이미지를 하나밖에 다루지 못함
* 여러 서비스를 한꺼번에 다룰 수 있다.
* Docker Swarm Stack을 사용하여 배포된 Service 그룹은 overlay 네트워크에 속핢

## 📚 [Rolling Updates](https://docs.docker.com/engine/swarm/swarm-tutorial/rolling-update/)

* 무중단배포
  * 서비스 장애와 배포에 있어서 부담감을 최소화 -> 서비스가 중단되지 않고도 코드를 배포
* Rolling Update : 서비스 각 테스크를 한 번에 업데이트하지 않고, 지연 시간을 설정해 테스크를 순차적으로 업데이트
> * --update-daly
> * --update-paralleism

## 📚 [Rollback](https://docs.docker.com/reference/cli/docker/service/rollback/)
