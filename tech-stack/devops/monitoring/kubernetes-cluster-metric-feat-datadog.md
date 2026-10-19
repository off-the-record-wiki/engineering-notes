# Monitoring - Kubernetes Cluster Metric(feat. DataDog)

#### Pod 상태 메트릭

* **kubernetes.pods.running** : 현재 Running 상태인 Pod 수. 클러스터의 워크로드 규모를 나타냄. 급격한 증감은 스케일링 이벤트나 장애를 의미.
* **kubernetes_state.pod.status_phase** : Pod의 라이프사이클 단계. `Running` : 정상. `Pending` : 스케줄링 대기(리소스 부족 가능). `Failed` : 실패. `Succeeded` : 완료(Job/CronJob).
* **kubernetes_state.pod.ready** : Pod의 Ready 조건 충족 여부. `condition = true`인 경우 트래픽 수신 가능. `condition = false`인 경우 비정상 상태로 서비스 불가.
* 해석법 : `Pending`이 지속되면 노드 리소스 부족, `Failed`가 증가하면 애플리케이션 오류를 의심.

#### CPU 메트릭

* **kubernetes.cpu.usage.total** : 실제 CPU 사용량. 1 core = 1,000,000,000 나노코어. 값이 `requests`에 근접하면 스케일업 검토 필요.
* **kubernetes.cpu.requests** : 컨테이너에 보장된 CPU 자원. 스케줄링 기준값. Pod가 최소한 이만큼의 CPU를 보장받음.
* **kubernetes.cpu.limits** : 컨테이너 CPU 상한선. 이 값을 초과하면 CPU 스로틀링(throttling) 발생.
* **kubernetes.cpu.capacity** : 노드의 전체 CPU 코어 수. 클러스터 전체 용량 파악에 활용.
* 해석법 : `usage / requests` 비율이 지속적으로 높으면 `requests`값 상향 조정이 필요. `usage / limits` 비율이 100%에 가까우면 스로틀링이 발생하고 있을 수 있다.

#### 메모리 메트릭

* **kubernetes.memory.usage** : 실제 메모리 사용량. `limits`에 도달하면 OOMKilled 발생.
* **kubernetes.memory.requests** : 컨테이너에 보장된 메모리. 스케줄링 기준값. 노드에 충분한 메모리가 있어야 Pod 배치 가능.
* **kubernetes.memory.limits** : 메모리 상한선. 초과 시 컨테이너가 OOMKilled(강제 종료)됨.
* **kubernetes.memory.capacity** : 노드의 전체 메모리. 클러스터 용량 산정에 활용.
* 해석법 : 메모리는 CPU와 달리 스로틀링 없이 즉시 Kill된다. `usage`가 `limits`의 90% 이상이면 OOM 리스크가 높으므로 주의가 필요.

#### 컨테이너 상태 메트릭

* **kubernetes_state.container.running** : Running 상태 컨테이너 수. 정상 운영 중인 컨테이너.
* **kubernetes_state.container.ready** : Ready 상태 컨테이너 수. 서비스 준비 완료된 컨테이너.
* **kubernetes_state.container.terminated** : 종료된 컨테이너 수. 정상 종료 또는 오류 종료 모두 포함.
* **kubernetes_state.container.restarts** : 컨테이너 재시작 횟수. 빈번한 재시작은 애플리케이션 불안정 신호.
* **kubernetes_state.container.status_report.count.waiting** : Waiting 상태 컨테이너 수. `reason = crashloopbackoff`이면 반복적 크래시 발생.
* **kubernetes.containers.state.terminated** : 종료 사유별 컨테이너 수. `reason = oomkilled`이면 메모리 부족으로 강제 종료.
* 해석법 1 : `CrashLoopBackOff`는 컨테이너가 반복적으로 시작 → 크래시 → 재시작. 애플리케이션 버그나 설정 오류.
* 해석법 2 : `OOMKilled`는 메모리 limits 초과. limits 상향 또는 메모리 누수 점검 필요

#### 네트워크 메트릭

* **kubernetes.network.rx_bytes** : 수신 네트워크 트래픽.
* **kubernetes.network.tx_bytes** : 송신 네트워크 트래픽.
* **kubernetes.network.rx_errors** : 수신 네트워크 오류.
* **kubernetes.network.tx_errors** : 송신 네트워크 오류.
