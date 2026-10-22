# Kubernetes - Helm vs Kustomize

#### Kubernetes - Helm

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4df58a30-c65a-4e13-a93c-9f05dde18dad.png)

* Helm은 Kubernetes 애플리케이션을 쉽게 설치하고 관리하기 위한 패키지 관리자이다.
* 애플리케이션 구성은 템플릿과 기본 설정값을 묶은 차트(Chart) 단위로 배포된다.
* 환경별 설정은 `values.yaml` 파일이나 명령줄 옵션으로 변경할 수 있어 개발·스테이징·운영 환경을 일관되게 관리할 수 있다.
* 차트를 설치하면 릴리스(Release)가 생성되며, Helm은 업그레이드 이력과 버전을 관리해 롤백을 지원한다.
* 복잡한 Kubernetes 매니페스트를 재사용하고 배포 과정을 표준화할 수 있지만, 템플릿과 설정이 지나치게 복잡해지지 않도록 관리해야 한다.

```
my-app/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── deployment.yaml
    └── service.yaml
```
```yaml
# Chart.yaml
apiVersion: v2
name: my-app
description: Nginx 애플리케이션 예제
type: application
version: 0.1.0
appVersion: "1.28"
```
```yaml
# values.yaml
replicaCount: 2

image:
  repository: nginx
  tag: "1.28"

service:
  type: ClusterIP
  port: 80
```
```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}-my-app
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: {{ .Release.Name }}-my-app
  template:
    metadata:
      labels:
        app: {{ .Release.Name }}-my-app
    spec:
      containers:
        - name: nginx
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: 80
```
```yaml
# templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ .Release.Name }}-my-app
spec:
  type: {{ .Values.service.type }}
  selector:
    app: {{ .Release.Name }}-my-app
  ports:
    - port: {{ .Values.service.port }}
      targetPort: 80
```

#### Kubernetes - Kustomize

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1b251500-9224-43d4-9f5c-378e2f35fd07.png)

* Kustomize는 Kubernetes 매니페스트를 템플릿 없이 환경별로 수정하고 조합하는 구성 관리 도구이다.
* 공통 리소스는 Base에 정의하고, 개발·스테이징·운영 환경의 차이는 Overlay로 관리한다.
* `kustomization.yaml` 파일에서 리소스, 패치, 이미지 태그, 네임스페이스 등을 선언적으로 설정한다.
* Kubernetes의 `kubectl`에 내장되어 있어 `kubectl apply -k <디렉터리>` 명령으로 바로 배포할 수 있다.
* 원본 YAML을 유지하면서 필요한 부분만 패치하므로 구성이 명확하지만, 복잡한 조건 처리나 동적 템플릿 기능에는 한계가 있다.

```
k8s/
├── base/
│   ├── deployment.yaml
│   └── kustomization.yaml
└── overlays/
    ├── dev/
    │   └── kustomization.yaml
    └── prod/
        └── kustomization.yaml
```
```yaml
# base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: nginx:1.27
```
```yaml
# base/kustomization.yaml
resources:
  - deployment.yaml
```
```yaml
# overlays/dev/kustomization.yaml
resources:
  - ../../base

namePrefix: dev-

replicas:
  - name: my-app
    count: 1
```
```yaml
# overlays/prod/kustomization.yaml
resources:
  - ../../base

namePrefix: prod-

replicas:
  - name: my-app
    count: 3

images:
  - name: nginx
    newTag: "1.28"
```
