# AWS - Signed URL

## 📚 Signed URL(서명된 URL)

* Signed URL은 CloudFront나 S3와 같은 서비스에서 특정 리소스에 대한 접근 권한을 제한적으로 부여하기 위해 사용되는 URL이다.
* 특정 날짜나 시간이 지나면 액세스가 불가능해지기 때문에 "보안"이 중요한 서비스라면 고려 대상이 된다.

```
https://example.cloudfront.net/exmaple.jpg?Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGV2Y2RuLmdvY2hvLWJhY2suY29tL2NvbXBhbmllcy8xMDIyL2xvZ28ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNjkyODc1Mjk1fSwiSXBBZGRyZXNzIjp7IkFXUzpTb3VyY2VJcCI6IjAuMC4wLjAvMCJ9LCJEYXRlR3JlYXRlclRoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTY5Mjc4ODg5NX19fV19&Signature=fABMv0XTCesYBg9QfWviX~Rqeee9bs2gID68c5FaF~J4lVHmXjBNe9YbC7MbfUcVeR8nLr6v0epVQnSrZOd4gsai~~uB7wE5tSGC-tHgx3KJ78mGPPRHS0xS3jsg0EyhAKhgteOjFE96EMsDPmdLkiWy07oe3PMIn6vfxKLqTyL1G413CaBFz5S5esHtTb3uQ6sKsDJg416Yxmy6oUeJuo0WQrj7M6Qn8LYCtbR5Rmo37mywHwpBOB8SREIMVo05HT2RNreoOprmH21J-820l5RZtH2TeBvlti3MZt48xMIWZkqNiApQC9RS889kA34OtUgDM4Ajojr04mgKe1HQPA__&Key-Pair-Id=Key-Pair-ID
```

* https://example.cloudfront.net : CloudFront 도메인의 URL
* example.jpg : CloudFront를 통해 서빙되는 파일
* Policy : 리소스 사용 제한을 정의하는 정책의 내용을 Base64로 인코딩한 값
* Signature : 리소스 사용 제한을 정의하는 정책의 내용을 CloudFront 개인키(Private Key)로 서명한 해시 값을 Base64로 인코딩한 값
* Key-Pair-Id : CloudFront 전용 키 쌍(Key Pair)의 액세스 키

### [Signed URL Policy](https://docs.aws.amazon.com/ko_kr/AmazonCloudFront/latest/DeveloperGuide/private-content-signed-urls.html)

* Canned Policy
  * resource 1개의 사용을 제한한다.
  * 특정 날짜가 지나면 액세스를 불가능하게 하는 기능만 사용할 수 있다.
  * Policy가 URL의 쿼리 파라미터에 포함되지 않아 URL의 길이가 대체로 짧다. 
* Custom Policy
  * resource 여러 개 사용을 제한할 수 있다.
  * 특정 날짜가 지나면 액세스를 불가능하게 하는 기능, 특정 날짜가 지나야만 액세스를 가능하게 하는 기능, 특정 IP만 액세스가 가능하게 하는 기능들을 사용할 수 있다.
  * Policy가 URL의 쿼리 파라미터에 포함되어 URL이 길게 나온다.

### Signed URL 사용 배경

* 회사의 서비스 중에 서비스 고객센터에 접수되는 문의를 처리할 수 있는 창구가 있는데, 요구사항으로 이미지와 더불어 요청사항 텍스트를 작성해서 전송할 수 있어야 한다. 허나 기업을 대상으로 하는 서비스와 개인을 대상으로 하는 서비스가 다르고 보안에 민감한 요소를 캡처해서 전송할 수 있다는 클라이언트 측의 불확실성을 대비해야했다.
* Signed URL(서명된 URL)은 임시로 생성되는 접근 권한이 부여된 URL로, 인증된 사용자가 제한된 시간 동안만 S3 객체에 접근 가능하도록 해준다.
* 객체의 경로와 이름이 노출되더라도, 서명되지 않은 URL로는 접근이 불가능하다.
* Signed URL은 만료 시간이 지나면 더 이상 접근이 허용되지 않으므로, 보안성을 크게 강화할 수 있다.
* 서버에서 인증된 사용자에게만 Signed URL을 발급하여, 민감한 미디어 파일의 안전한 조회가 가능하도록 처리하면서 서비스의 보안 요구사항(기업/개인 모두)을 만족시키며, 외부 공격으로부터 S3 버킷의 객체를 안전하게 보호할 수 있다고 판단해서 채택하게 되었다.

### Signed URL 아키텍처

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9f20e847-a12f-4f0e-9fd1-d135dfe78df5.png)
