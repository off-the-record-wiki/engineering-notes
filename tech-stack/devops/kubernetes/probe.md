# Kubernetes - Probe

#### Kubernetes ‐ Startup Probe

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fda3947f-ed3e-45ce-a3da-29a0f96ee410.png)

* 애플리케이션이 정상적으로 시작되었는지를 검사한다.
* 시작 시간이 오래 걸리는 애플리케이션에 적합하다.
* 검사에 성공하기 전까지 Liveness Probe와 Readiness Probe는 실행되지 않는다.
* 설정된 횟수만큼 검사에 실패하면 컨테이너를 종료하고 재시작 정책을 적용한다.
* 애플리케이션이 시작된 후에는 더 이상 실행되지 않는다.

#### Kubernetes ‐ Liveness Probe

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1df7b155-d8cd-4062-9043-222baf77d280.png)

* 실행 중인 컨테이너가 정상적으로 동작하고 있는지를 검사한다.
* 프로세스가 실행 중이지만 교착 상태처럼 응답할 수 없는 상황을 감지할 수 있다.
* 설정된 횟수만큼 검사에 실패하면 kubelet이 해당 컨테이너를 재시작한다.
* 잘못 설정하면 정상 컨테이너가 반복적으로 재시작될 수 있으므로 주의해야 한다.
* 일시적인 장애보다 재시작이 필요한 복구 불가능한 상태를 검사하도록 구성하는 것이 좋다.

#### Kubernetes ‐ Readiness Probe

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4c5a44f6-eab8-4d8f-ab85-ea94e133ca83.png)

* 컨테이너가 외부 요청을 처리할 준비가 되었는지를 검사한다.
* 검사에 실패하면 Pod가 Service의 트래픽 대상에서 제외된다.
* 검사 실패 시에도 컨테이너 자체는 종료되거나 재시작되지 않는다.
* 데이터 로딩, 캐시 준비 또는 외부 시스템 연결이 필요한 애플리케이션에 유용하다.
* 컨테이너가 실행되는 전체 기간에 걸쳐 주기적으로 수행된다.

#### 표준 가이드라인

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/80959256-c655-4d67-8338-80a4d5e69fd2.png)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: backend-app
spec:
  containers:
  - name: main-container
    image: backend-service:latest
    ports:
    - name: liveness-port
      containerPort: 8080

    # 1. 초기 구동 단계 방어벽 (최대 300초 동안 켜지길 기다려줌)
    startupProbe:
      httpGet:
        path: /live
        port: liveness-port
      failureThreshold: 30
      periodSeconds: 10

    # 2. 구동 완류 후, 프로세스 데드락 감시 (단 1번이라도 실패하면 즉시 재시작)
    livenessProbe:
      httpGet:
        path: /live
        port: liveness-port
      failureThreshold: 1
      periodSeconds: 10

    # 3. 구동 완료 후, 실제 트래픽 유입 가능 여부 실시간 감시 (실패 시 라우팅 차단)
    readinessProbe:
      httpGet:
        path: /ready
        port: liveness-port
      failureThreshold: 3
      periodSeconds: 10
```
