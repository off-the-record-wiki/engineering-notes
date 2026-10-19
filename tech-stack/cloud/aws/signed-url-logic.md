# AWS - Signed URL Logic

## 📚 AWS ‐ Signed URL 서명 로직 정리

* Signed URL이란, 서버가 미리 서명해 준 URL로, 특정 조건(시간, 경로 등) 을 만족할 때만 리소스 접근을 허용하는 방식을 말한다.

## 📚 서명에 관여하는 핵심 요소

* 키 구성은 다음과 같다.
* `private key` → 서명 생성용도, 서버만 보유한다.
* `public key` → 서명 검증용도, CloudFront/서비스에 등록한다.

## 📚 Signed URL에 포함되는 정보

|요소|의미|
|:--:|:--:|
|Resource|접근할 리소스 경로|
|ExpireTime|만료 시간(Unix epoch)|
|Policy or Expires|접근 정책|
|Signature|Private Key로 만든 서명|
|Key-Pair-Id|어떤 Public Key로 검증할지|

## 📚 Key Pair ID란?

* Key Pair ID = “이 서명이 어떤 Public Key로 검증돼야 하는지 알려주는 식별자”
* CloudFront에는 여러 개의 Public Key를 등록할 수 있다.
* Signature만 보고서 어떤 Public Key를 써야 할지 알 수 없기 때문에 Key Pair ID가 필요한 것이다.

```
[ Private Key ]  ──(서명)──▶ Signature
       │
       └── 대응되는 Public Key
               │
               └── Key Pair ID (K123ABC...)
```
* Private Key ↔ Public Key : 한 쌍으로 구성된다.
* Public Key를 Cloudfront에 등록하면 내부적으로 ID를 하나 부여하게 되는데 그것이 바로 Key Pair ID이다.

## 📚 Signed URL에서 Key Pair ID의 역할

1. Key-Pair-ID를 확인한다.
2. 해당 ID에 매핑된 Public Key를 조회한다.
3. Public Key로 Signature를 검증한다.
4. 성공하면 통과 처리가 되고 실패하면 403 Access Denied가 나오게 된다.
