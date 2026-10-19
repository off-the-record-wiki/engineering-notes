# Spring - File Upload

#### 서블릿을 활용한 파일 업로드

* 큰 파일을 무제한 업로드하게 둘 수는 없으므로 업로드 사이즈를 제한할 수 있다.
* 사이즈를 넘으면 `SizeLimitExceededException` 예외가 발생한다.

```properties
spring.servlet.multipart.max-file-size=1MB     # 파일 하나의 최대 사이즈
spring.servlet.multipart.max-request-size=10MB # 멀티파트 요청 하나에 여러 파일을 업로드 할 수 있는데 그 전체 합
```

* 파일 데이터는 `multipart/form-data` 방식으로 전송되기 때문에 `@RequestBody`를 사용할 수 없다.
* `@RequestPart`, `@RequestParam`, `@ModelAttribute` 어노테이션으로는 받을 수 있다.

> 📚multipart/form-data
>
> **1. 핵심 개념**
> * 일반적인 폼 데이터(application/x-www-form-urlencoded)는 `key=value&key=value`형태의 단순 문자열로 전송된다.
> * 하지만 바이너리 데이터는 이런 방식으로 전송하기 어렵기 때문에, HTTP는 multipart/form-data라는 방식을 제공한다.
>
> **2. HTTP 요청 메시지 구조**
```http
POST /save HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

------WebKitFormBoundary...
Content-Disposition: form-data; name="itemName"

Spring Book
------WebKitFormBoundary...
Content-Disposition: form-data; name="imageFile"; filename="logo.png"
Content-Type: image/png

(바이너리 데이터...)
------WebKitFormBoundary...--
```

```java
public ResponseEntity<> file(@RequestPart MultipartFile files){}
public ResponseEntity<> file(@RequestParam MultipartFile file){}
public ResponseEntity<> file(@ModelAttribute MultipartFile file){}
// 생략시 modelAttribute 적용
public ResponseEntity<> file(MultipartFile file){}
```
> * `Content-Disposition` : 각 파트의 이름과 실제 파일명 정보를 담고 있다.
> * 개별 `Content-Type` : 파일 파트의 경우 해당 파일이 어떤 형식인지 별도의 헤더를 가질 수 있다.
>
> **3. 스프링 MVC의 처리 방식**
> * Spring은 복잡한 멀티파트 요청을 쉽게 처리할 수 있도록 `MultipartResolver`를 제공한다.
> * DispatcherServlet의 개입 : 요청을 받으면 `MultipartResolver`를 실행하고 해당 요청이 `multipart/form-data`인지 확인하고, 맞다면 일반적인 `HttpServletRequest`를 `MultipartHttpServletRequest`로 변환한다.
> * 컨트롤러에서의 활용 : 스프링에서 파일을 받는 방법으로 `MultipartFile` 인터페이스를 사용한다.
```java
@PostMapping("/upload")
public String saveFile(@RequestParam String itemName,
                       @RequestParam MultipartFile imageFile) throws IOException {
    if (!imageFile.isEmpty()) {
        String fullPath = "/upload/" + imageFile.getOriginalFilename();
        imageFile.transferTo(new File(fullPath)); // 파일 저장
    }
    return "upload-success";
}
```
> **4. 스프링 부트 설정(Configuration)**
> * 스프링 부트는 별도의 설정 없이도 `StandardServletMultipartResolver`를 기본으로 등록한다.
```properties
# 파일 하나당 최대 용량 (기본 1MB)
spring.servlet.multipart.max-file-size=10MB

# 한 번의 요청에 포함된 모든 파일의 총 합계 용량 (기본 10MB)
spring.servlet.multipart.max-request-size=50MB

# 멀티파트 처리 여부 (기본 true)
spring.servlet.multipart.enabled=true
```

#### 스프링과 파일 업로드

* 스프링은 `MultipartFile`이라는 인터페이스로 멀티파트 파일을 매우 편리하게 지원한다.

```java
public interface MultipartFile extends InputStreamSource {

    /**
     * 멀티파트 폼 데이터에서 파라미터의 이름을 반환합니다. (HTML input 태그의 name 속성값)
     */
    String getName();

    /**
     * 클라이언트가 업로드한 파일의 실제 이름을 반환합니다.
     * 주의: 보안상의 이유로(경로 조작 등) 이 이름을 그대로 서버 저장 경로에 사용하는 것은 위험합니다.
     */
    @Nullable String getOriginalFilename();

    /**
     * 파일의 콘텐츠 타입(MIME type)을 반환합니다. (예: image/png)
     */
    @Nullable String getContentType();

    /**
     * 파일이 비어있는지 여부를 확인합니다. (선택된 파일이 없거나 내용이 없는 경우 true)
     */
    boolean isEmpty();

    /**
     * 파일의 크기를 바이트(byte) 단위로 반환합니다.
     */
    long getSize();

    /**
     * 파일의 내용을 바이트 배열로 반환합니다.
     */
    byte[] getBytes() throws IOException;

    /**
     * 파일의 내용을 읽기 위한 InputStream을 반환합니다.
     */
    @Override
    InputStream getInputStream() throws IOException;

    /**
     * MultipartFile을 추상화된 Resource 객체로 변환합니다. (Spring 5.1+)
     */
    default Resource getResource() {
        return new MultipartFileResource(this);
    }

    /**
     * 업로드된 파일을 지정된 목적지(File)로 저장합니다.
     * 내부적으로 임시 저장소에 있는 파일을 이동하거나 복사합니다.
     */
    void transferTo(File dest) throws IOException, IllegalStateException;

    /**
     * 업로드된 파일을 지정된 경로(Path)로 저장합니다. (Spring 5.1+)
     */
    default void transferTo(Path dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
    }
}
```

> 관련 예제 코드는 [여기](https://github.com/dnwls16071/Backend_Study_TIL/tree/main/mvc2/src/main/java/com/jwj/mvc2/upload)에서 볼 수 있습니다.
