# Kubernetes - ConfigMap, Secret

#### Kubernetes - ConfigMap

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d1e0622b-5299-4a6d-88c3-97446e0c5149.png)

* ConfigMap은 애플리케이션의 일반 설정값을 key-value 형태로 저장하는 Kubernetes 오브젝트이다.
* 설정을 컨테이너 이미지와 분리하므로 동일한 이미지를 개발, 테스트 및 운영 환경에서 재사용할 수 있다.
* Pod는 ConfigMap을 환경 변수, 실행 명령 인자 또는 Volume에 마운트된 설정 파일로 사용할 수 있다.
* 비밀번호, 토큰처럼 민감한 정보는 암호화되지 않으므로 ConfigMap이 아닌 Secret에 저장해야 한다.
* ConfigMap의 최대 크기는 1MiB이며, 환경 변수로 주입된 값은 ConfigMap이 변경되어도 자동 갱신되지 않으므로 Pod를 재시작해야만 한다.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  APP_ENV: production
  LOG_LEVEL: info
```

#### Kubernetes - Secret

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9bfd07c1-b524-4bd9-97a6-d7bcff93265b.png)

* Secret은 비밀번호, API 토큰, 인증서처럼 민감한 데이터를 저장하는 Kubernetes 오브젝트이다.
* Pod는 Secret을 환경 변수 또는 Volume에 마운트된 파일로 사용할 수 있다.
* `data` 필드의 값은 Base64로 인코딩해야 하며, `stringData`를 사용하면 일반 문자열을 입력할 수 있다.
* Base64는 암호화가 아니며 Secret은 기본적으로 etcd에 암호화되지 않은 상태로 저장될 수 있으므로, 저장 데이터 암호화와 최소 권한 RBAC 설정이 필요하다.
* Secret 하나의 최대 크기는 1MiB이며, 일반 설정값은 ConfigMap에 저장하는 것이 적합하다.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:
  username: admin
  password: my-password
```
