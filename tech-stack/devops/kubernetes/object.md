# Kubernetes - Object

#### Labels

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/af5ce604-1674-4fc3-837d-cc20da3f0c84.png)

* Kubernetes Label은 Pod, Service, Deployment 등의 오브젝트를 식별하고 분류하는 키-값 형태의 메타데이터이다.
* Service와 Deployment는 Selector를 사용하여 특정 Label을 가진 Pod를 선택하고 연결하거나 관리한다.
* Label은 리소스 선택에 사용되지만, Annotation은 빌드 정보나 설명처럼 선택하지 않는 부가 정보를 저장하는 데 사용된다.

```yaml
"metadata": {
  "labels": {
    "key1" : "value1",
    "key2" : "value2"
  }
}
```

#### Selector

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c817f65a-2373-4b76-9984-07b8c81f8aec.png)

* Selector는 Kubernetes에서 Label 조건을 이용해 특정 오브젝트를 선택하는 기능이다.
* Service는 Selector와 일치하는 Pod로 트래픽을 전달하고, Deployment는 Selector와 일치하는 Pod를 관리한다.
* Selector는 `app=nginx`, `environment in (production, staging)`, `tier!=backend`처럼 일치, 집합, 제외 조건을 지원한다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: cuda-test
spec:
  containers:
    - name: cuda-test
      image: "registry.k8s.io/cuda-vector-add:v0.1"
      resources:
        limits:
          nvidia.com/gpu: 1
  nodeSelector:
    accelerator: nvidia-tesla-p100
```

#### Naming

* Kubernetes에서 Naming은 `metadata.name`을 사용해 Pod, Service, Deployment 같은 오브젝트에 이름을 지정한다.
* 이름은 같은 Namespace의 같은 리소스 종류 안에서 고유해야 하며 일반적으로 소문자·숫자·하이픈을 사용한다.
