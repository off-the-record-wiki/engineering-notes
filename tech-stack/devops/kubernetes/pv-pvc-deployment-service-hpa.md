# Kubernetes - PV/PVC, Deployment, Service, HPA

#### Kubernetes - PV(PersistentVolume)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/09dedd64-fdbc-45f1-8e69-b2ffa4120839.png)

* PV는 Pod에서 사용할 영구 저장 공간을 표현하는 Kubernetes 오브젝트이다.
* PV의 생명주기는 Pod와 독립적이므로 Pod가 삭제되거나 재생성되어도 데이터를 유지할 수 있다.
* 관리자가 직접 생성하는 정적 프로비저닝과 StorageClass가 자동 생성하는 동적 프로비저닝 방식이 있다.
* 사용자는 PVC를 생성해 필요한 용량과 접근 모드를 요청하며, Kubernetes가 조건에 맞는 PV와 연결한다.
* PVC가 삭제된 이후 PV의 처리 방식은 `Retain` 또는 `Delete` 같은 `persistentVolumeReclaimPolicy`로 결정된다.

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: app-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  nfs:
    server: 192.168.0.10
    path: /data
```

#### Kubernetes - PVC(PersistentVolumeClaim)

* PVC는 사용자가 필요한 영구 저장 공간의 용량과 조건을 요청하는 Kubernetes 오브젝트이다.
* Kubernetes는 요청한 용량, 접근 모드 및 StorageClass가 일치하는 PV를 찾아 PVC와 연결한다.
* 적절한 PV가 없고 StorageClass가 설정되어 있으면 새로운 PV가 동적으로 생성될 수 있다.
* Pod는 PV를 직접 지정하지 않고 PVC를 Volume으로 참조하여 저장 공간을 사용한다.
* PVC는 Namespace에 속하며, Pod와 PVC는 동일한 Namespace에 있어야 한다.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: manual
  resources:
    requests:
      storage: 5Gi
```
```yaml
volumes:
  - name: app-storage
    persistentVolumeClaim:
      claimName: app-pvc
```

#### Kubernetes - Deployment

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/99213186-ca9a-4c60-98e9-c1a8883bee52.png)

* Deployment는 주로 상태가 없는 애플리케이션의 여러 Pod를 선언적으로 배포하고 관리하는 Kubernetes 오브젝트이다.
* Deployment는 ReplicaSet을 생성하고, ReplicaSet은 지정된 개수의 Pod가 계속 실행되도록 관리한다.
* Pod가 삭제되거나 장애로 사라지면 ReplicaSet이 새로운 Pod를 생성해 설정된 복제본 수를 유지한다.
* 컨테이너 이미지나 Pod 템플릿이 변경되면 기본적으로 Rolling Update 방식으로 Pod를 점진적으로 교체한다.
* 복제본 수 조정, 배포 상태 확인, 배포 일시 정지 및 이전 버전으로의 롤백을 지원한다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      containers:
        - name: web
          image: nginx:1.27
          ports:
            - containerPort: 80
```

#### Kubernetes - Service

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3e47bf26-9e9a-4e6b-98b4-644b96cba98c.png)

* Service는 여러 Pod에 접근할 수 있는 고정된 IP 주소와 DNS 이름을 제공하는 Kubernetes 오브젝트이다.
* Label Selector와 일치하는 Pod를 찾아 해당 Pod들의 EndpointSlice를 구성하고 트래픽을 분산한다.
* Pod가 재생성되어 IP 주소가 변경되더라도 클라이언트는 동일한 Service 주소를 계속 사용할 수 있다.
* `port`는 Service가 요청을 받는 포트이고, `targetPort`는 요청을 전달할 컨테이너의 포트이다.
* Service 유형에는 클러스터 내부용 `ClusterIP`, 노드 포트로 공개하는 `NodePort`, 외부 로드 밸런서를 사용하는 `LoadBalancer`, 외부 DNS를 연결하는 `ExternalName`이 있다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: web-service
spec:
  type: ClusterIP
  selector:
    app: web
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
```

#### Kubernetes - HPA(Horizontal Pod Autoscaler)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f86160d2-e892-4066-81da-72dc79edb0fd.png)

* HPA는 CPU, 메모리 또는 사용자 정의 지표를 기준으로 Pod의 복제본 수를 자동 조정하는 Kubernetes 오브젝트이다.
* 부하가 증가하면 Pod를 추가하는 Scale-out을 수행하고, 부하가 감소하면 Pod를 줄이는 Scale-in을 수행한다.
* `minReplicas`와 `maxReplicas`를 사용해 Pod 개수의 최솟값과 최댓값을 제한한다.
* CPU 사용률 기반 HPA를 사용하려면 일반적으로 Metrics Server가 필요하며, 컨테이너에 `CPU resources.requests`가 설정되어 있어야 한다.
* Deployment나 StatefulSet처럼 `scale` 기능을 제공하는 워크로드에 적용할 수 있지만, 모든 노드에 Pod를 실행하는 DaemonSet에는 적용할 수 없다.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: web-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: web-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```
