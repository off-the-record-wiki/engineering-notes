# Kubernetes - Pod Essentials for Application Developers

#### Kubernetes - Downward API

* Downward API는 파드가 자신의 이름, 네임스페이스, IP, 노드 이름, 라벨, 어노테이션과 컨테이너 리소스 요청량·제한량을 환경 변수나 볼륨 파일로 읽게 해준다.
* 환경 변수 방식은 `fieldRef` 또는 `resourceFieldRef`를 사용하며, 파드가 시작된 뒤 값이 바뀌어도 환경 변수에는 반영되지 않는다.
* 볼륨 방식은 `downwardAPI` 볼륨을 마운트하며, 라벨과 어노테이션 등의 변경 사항을 파일을 통해 주기적으로 반영할 수 있다.
* Downward API에서 제공하지 않는 파드 상태나 다른 Kubernetes 리소스가 필요하면, 파드 내부에서 ServiceAccount 토큰으로 kube-apiserver를 호출해야 한다.
* kube-apiserver 접근 권한은 최소 권한의 Role과 RoleBinding으로 제한해야 하며, 일반적으로 Kubernetes 클라이언트 라이브러리의 in-cluster 설정을 사용한다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-info
  labels:
    app: demo
spec:
  containers:
    - name: app
      image: busybox
      command: ["sh", "-c", "env && cat /etc/podinfo/labels && sleep 3600"]
      env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: POD_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
      volumeMounts:
        - name: pod-info
          mountPath: /etc/podinfo
  volumes:
    - name: pod-info
      downwardAPI:
        items:
          - path: labels
            fieldRef:
              fieldPath: metadata.labels
          - path: annotations
            fieldRef:
              fieldPath: metadata.annotations
```

#### Kubernetes - kube-apiserver

* kube-apiserver는 Kubernetes API를 제공하는 컨트롤 플레인의 핵심 컴포넌트이며, kubectl, 컨트롤러, 스케줄러 등의 모든 요청을 처리한다.
* 요청은 인증(Authentication), 권한 확인(Authorization), 어드미션 제어(Admission Control), 객체 유효성 검증 단계를 거쳐 처리된다.
* 검증된 클러스터 상태는 `etcd`에 저장되며, 다른 컴포넌트가 `watch` API를 통해 변경 사항을 감지할 수 있도록 제공한다.
* 파드 내부에서는 `https://kubernetes.default.svc` 주소와 ServiceAccount 토큰을 사용해 kube-apiserver에 접근한다.
* kube-apiserver 접근 권한은 Role, ClusterRole, RoleBinding 등을 통해 최소 권한으로 제한해야 하며, 일반 애플리케이션은 직접 호출보다 Kubernetes 클라이언트 라이브러리를 사용하는 것이 편리하다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
rules:
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: pod-reader-binding
subjects:
  - kind: ServiceAccount
    name: default
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: pod-reader
```

#### Kubernetes - lifecycle.preStop

* `lifecycle.preStop`은 컨테이너가 종료되기 직전에 정리 작업을 수행하는 라이프사이클 훅이며, 연결 종료, 작업 완료, 상태 저장 등에 사용한다.
* kubelet은 `preStop` 실행이 끝난 후 컨테이너에 `SIGTERM`을 보내지만, 훅이 종료 유예시간을 모두 사용하면 컨테이너가 정상 종료할 시간이 부족해질 수 있다.
* `terminationGracePeriodSeconds`는 `preStop` 실행 전에 계산되기 시작하며, 기본값은 30초이고 훅 실행과 애플리케이션 종료 시간을 모두 포함한다.
* 핸들러에는 컨테이너 내부 명령을 실행하는 `exec`, HTTP 요청을 보내는 `httpGet`, 일정 시간 대기하는 `sleep`을 사용할 수 있다.
* 이미 정상적으로 완료된 컨테이너나 강제 종료된 파드에서는 실행되지 않을 수 있으므로, 반드시 수행되어야 하는 데이터 저장 작업을 전적으로 의존해서는 안 된다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: graceful-app
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: app
      image: nginx
      lifecycle:
        preStop:
          exec:
            command:
              - /bin/sh
              - -c
              - nginx -s quit
```

#### Kubernetes - GracefulShutdown

* Graceful Shutdown은 파드가 종료될 때 진행 중인 요청을 마무리하고 리소스를 정리한 뒤 안전하게 종료하는 과정이다.
* 파드 삭제 요청이 발생하면 `terminationGracePeriodSeconds`의 카운트다운이 시작되고, 파드는 Terminating 상태로 전환된다.
* kubelet은 `preStop` 훅을 먼저 실행한 후 컨테이너 프로세스에 `SIGTERM`을 전달하며, 애플리케이션은 이 신호를 처리해 신규 요청 수신을 중단해야 한다.
* 종료 중인 파드는 EndpointSlice에서 `ready: false`로 표시되지만 트래픽 전파에 시간이 걸릴 수 있으므로, `preStop`에서 잠시 대기하도록 구성하기도 한다.
* 유예시간 안에 종료되지 않으면 kubelet이 `SIGKILL`을 보내 강제로 종료하므로, 유예시간에는 `preStop` 실행 시간과 애플리케이션 정리 시간이 모두 포함되어야 한다.

```yaml
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: app
      image: my-app:1.0
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 5"]
```
