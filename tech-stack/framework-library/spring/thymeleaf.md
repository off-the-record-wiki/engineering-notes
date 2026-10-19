# Spring - Thymeleaf

#### Thymeleaf 특징

* 서버 사이드 HTML 렌더링(Server Side Rendering)
* 네츄럴 템플릿 → 순수 HTML을 유지하면서 뷰 템플릿도 사용할 수 있는 것
* 스프링 통합 지원

```html
<!-- 타임리프를 사용할 것이라면 맨 위에 선언을 해줘야 한다.-->
<html xmlns:th="http://www.thymeleaf.org">
```

#### 단순 출력 - th:text, th:utext

```html
<ul>
  <li th:text="${data}">th:text 사용 </li>
  <li>컨텐츠 안에서 직접 출력하기 = [[${data}]]</li>
</ul>

<!-- 실제 출력 화면 -->
Hello Spring
컨텐츠 안에서 직접 출력하기 = Hello Spring
```
```html
<ul>
    <li>th:text = <span th:text="${data}"></span></li>
    <li>th:utext = <span th:utext="${data}"></span></li>
</ul>

<h1><span th:inline="none">[[...]] vs [(...)]</span></h1>
<ul>
    <li><span th:inline="none">[[...]] = </span>[[${data}]]</li>
    <li><span th:inline="none">[(...)] = </span>[(${data})]</li>
</ul>

<!-- 실제 출력 화면 -->
th:text = Hello <b>Spring</b>
th:utext = Hello Spring <!-- spring 글씨가 굵게 출력됨-->

[[...]] = Hello <b>Spring</b>
[(...)] = Hello Spring <!-- spring 글씨가 굵게 출력됨-->
```

#### SpringEL

```java
@GetMapping("/variable")
public String variable(Model model){
    User userA = new User("userA", 10);
    User userB = new User("userB", 20);

    List<User> list = new ArrayList<>();
    list.add(userA);
    list.add(userB);

    HashMap<String, User> map = new HashMap<>();
    map.put("userA",userA);
    map.put("userB",userB);

    model.addAttribute("user",userA);
    model.addAttribute("users",list);
    model.addAttribute("userMap",map);

    return "basic/variable";
}
```
```html
<ul>Object
    <li>${user.username} =    <span th:text="${user.username}"></span></li>
    <li>${user['username']} = <span th:text="${user['username']}"></span></li>
    <li>${user.getUsername()} = <span th:text="${user.getUsername()}"></span></li>
</ul>
<ul>List
    <li>${users[0].username}    = <span th:text="${users[0].username}"></span></li>
    <li>${users[0]['username']} = <span th:text="${users[0]['username']}"></span></li>
    <li>${users[0].getUsername()} = <span th:text="${users[0].getUsername()}"></span></li>
</ul>
<ul>Map
    <li>${userMap['userA'].username} =  <span th:text="${userMap['userA'].username}"></span></li>
    <li>${userMap['userA']['username']} = <span th:text="${userMap['userA']['username']}"></span></li>
    <li>${userMap['userA'].getUsername()} = <span th:text="${userMap['userA'].getUsername()}"></span></li>
</ul>


<div th:with="first=${users[0]}">
<p>처음 사람의 이름은 <span th:text="${first.username}"></span></p>
</div>

<!-- 실제 출력 화면 -->
Object
+ ${user.username} = userA
+ ${user['username']} = userA
+ ${user.getUsername()} = userA
List
+ ${users[0].username} = userA
+ ${users[0]['username']} = userA
+ ${users[0].getUsername()} = userA
Map
+ ${userMap['userA'].username} = userA
+ ${userMap['userA']['username']} = userA
+ ${userMap['userA'].getUsername()} = userA

처음 사람의 이름은 userA
```

#### 기본 객체

* ${#request}
* ${#response}
* ${#session}
* ${#servletContext}
* ${#locale}

```html
<ul>
    <li>request = <span th:text="${#request}"></span></li>
    <li>response = <span th:text="${#response}"></span></li>
    <li>session = <span th:text="${#session}"></span></li>
    <li>servletContext = <span th:text="${#servletContext}"></span></li>
    <li>locale = <span th:text="${#locale}"></span></li>
</ul>

<h1>편의 객체</h1>
<ul>
    <li>Request Parameter = <span th:text="${param.paramData}"></span></li>
    <li>session = <span th:text="${session.sessionData}"></span></li>
</ul>

<!-- 실제 출력 화면 -->
request = org.apache.catalina.connector.RequestFacade@7e2b84d1
response = org.apache.catalina.connector.ResponseFacade@47e652a6
session = org.apache.catalina.session.StandardSessionFacade@4ef9b74b
servletContext = org.apache.catalina.core.ApplicationContextFacade@7546035e
locale = ko_KR

편의 객체
Request Parameter = hello
session = Hello Session
spring bean = Spring!
```

#### 유틸리티 객체와 날짜

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-b-expression-utility-objects)

#### URL

```java
model.addAttribute("param1", "data1");
model.addAttribute("param2", "data2");
```
```html
<ul>
    <li><a th:href="@{/hello}">basic url</a></li>
    <li><a th:href="@{/hello(param1=${param1}, param2=${param2})}">hello query param</a></li>
    <li><a th:href="@{/hello/{param1}/{param2}(param1=${param1}, param2=${param2})}">path variable</a></li>
    <li><a th:href="@{/hello/{param1}(param1=${param1}, param2=${param2})}">path variable + query parameter</a></li>
</ul>

<!--굳이 파람을 가져올 필요가 없다면 'test' 처럼 직접 넣어도 됩니다.-->
th:href="@{/basic/items/{itemId}(itemId=${item.id}, query='test')}"

<!-- 리터럴 대체 문법 응용 -->
th:href="@{|/basic/items/${item.id}|}"

<!-- 리터럴 사용하겠다고 먼저 || 를 사용하고 그 안에서 url 경로식 있으면 그 안에 다시 || 써줘야합니다. -->
th:onclick="|location.href='@{|/basic/items/${item.id}/edit|}'|"
```

#### 리터럴

* 리터털이란, 소스 코드에서 고정된 값을 말한다.
* Thymeleaf에서 리터럴을 사용하고자 한다면 항상 '(작은 따옴표)로 감싸야 한다.
* 리터럴 대체 문법으로 ||안에 문자열을 작성하면 하나의 의미 있는 토큰으로 인식하여 작은 따옴표를 사용하지 않아도 된다.

```html
<!-- 원래는 작은 따옴표로 감싸야함-->
<span th:text="'hello'">

<!-- 붙어있으면 생략 가능-->
<span th:text="hello">

<!-- 붙어있지 않으므로 작은 따옴표 필수-->
<span th:text="'hello world'">

<!-- 덧셈 연산 가능-->
<span th:text="'hello' + ' world'">

<!-- 리터럴 대체 문법 ||-->
<span th:text="|hello ${data}|"></span>
```

#### 연산

```html
<li>10 + 2 = <span th:text="10 + 2"></span></li>
<li>10 % 2 == 0 = <span th:text="10 % 2 == 0"></span></li>
<li>1 gt 10 = <span th:text="1 gt 10"></span></li>
<li>1 >= 10 = <span th:text="1 >= 10"></span></li>
<li>1 ge 10 = <span th:text="1 ge 10"></span></li>
<li>1 == 10 = <span th:text="1 == 10"></span></li>
<li>1 != 10 = <span th:text="1 != 10"></span></li>
<li>(10 % 2 == 0)? '짝수':'홀수' = <span th:text="(10 % 2 == 0)? '짝수':'홀수'"></span></li>
<li>${data}?: '데이터가 없습니다.' = <span th:text="${data}?: '데이터가 없습니다.'"></span></li>
<li>${nullData}?: '데이터가 없습니다.' = <span th:text="${nullData}?: '데이터가 없습니다.'"></span></li>
<li>${data}?: _ = <span th:text="${data}?: _">데이터가 없습니다.</span></li>
<li>${nullData}?: _ = <span th:text="${nullData}?: _">데이터가 없습니다.</span></li>
```

#### 속성 값 설정 방법

* Thymeleaf는 HTML 태그에 `th:` 속성을 지정하는 방식으로 동작한다.

```html
<!-- name 태그를 th:name의 태그로 대체해서 랜더링-->
<input type="text" name="mock" th:name="userA" />

<!-- class 태그 뒤쪽에 large를 붙여서 랜더링 -> text large -->
<input type="text" class="text" th:classappend="large" /><br/>

<!-- html은 check 속성이 있으면 true, false에 관계없이 그냥 체크박스에 체크를 해버림 -->
<input type="checkbox" name="active" checked="false" /><br/>

<!-- 그에 반해 타임리프는 값이 적용된다. -->
<input type="checkbox" name="active" th:checked="false" /><br/>
```

#### 반복

```html
<tr th:each="user : ${users}">
    <td th:text="${user.username}">username</td>
    <td th:text="${user.age}">0</td>
</tr>

<tr th:each="user, userStat : ${users}">
    <td th:text="${userStat.count}">username</td>
    <td th:text="${user.username}">username</td>
    <td th:text="${user.age}">0</td>
    <td>
      index = <span th:text="${userStat.index}"></span>
      count = <span th:text="${userStat.count}"></span>
      size = <span th:text="${userStat.size}"></span>
      even? = <span th:text="${userStat.even}"></span>
      odd? = <span th:text="${userStat.odd}"></span>
      first? = <span th:text="${userStat.first}"></span>
      last? = <span th:text="${userStat.last}"></span>
      current = <span th:text="${userStat.current}"></span>
    </td>
</tr>
```

* index : 0부터 시작하는 값
* count : 1부터 시작하는 값
* size : 전체 사이즈
* even , odd : count를 기준으로 홀수, 짝수 여부( boolean )
* first , last :처음, 마지막 여부( boolean )
* current : 현재 객체

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#keeping-iteration-status)

#### 조건부 평가

```html
 <tr th:each="user, userStat : ${users}">
    <td th:text="${userStat.count}">1</td>
    <td th:text="${user.username}">username</td>
    <td>
        <span th:text="${user.age}">0</span>
        <span th:text="'미성년자'" th:if="${user.age lt 20}"></span>
        <span th:text="'미성년자'" th:unless="${user.age ge 20}"></span>
    </td>
</tr>
<tr th:each="user, userStat : ${users}">
    <td th:text="${userStat.count}">1</td>
    <td th:text="${user.username}">username</td>
    <td th:switch="${user.age}">
        <span th:case="10">10살</span>
        <span th:case="20">20살</span>
        <span th:case="*">기타</span>
    </td>
</tr>
```

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#simple-conditionals-if-and-unless)

#### 주석

```html
html 표준 주석
<!-- -->

타임리프 파서 주석
<!--/* */-->
```

#### 블록

* `th:block`은 HTML 태그가 아닌 타임리프 태그이다.
* each로 해결하기 어려운 경우 사용한다.

```html
<th:block th:each="user : ${users}">
    <div>
        사용자 이름1 <span th:text="${user.username}"></span>
        사용자 나이1 <span th:text="${user.age}"></span>
    </div>
    <div>
        요약 <span th:text="${user.username} + ' / ' + ${user.age}"></span>
    </div>
</th:block>
```

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#synthetic-thblock-tag)

#### 자바스크립트 인라인

```html
<!-- 자바스크립트 인라인 사용 후 -->
<script th:inline="javascript">
    var username = [[${user.username}]];
    var age = [[${user.age}]];

    //자바스크립트 내추럴 템플릿
    var username2 = /*[[${user.username}]]*/ "test username";

    //객체
    var user = [[${user}]];
</script>
```

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#javascript-inlining)

#### 템플릿 조각

* header, footer 등 공통 영역을 볼 수 있는데 이런 공통 영역에 대한 중복을 해소하기 위해 템플릿 조각과 레이아웃 기능을 지원한다.

```java
@GetMapping("/fragment")
public String template() {
    return "template/fragment/fragmentMain";
}
```
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<body>

<footer th:fragment="copy">
    푸터 자리 입니다.
</footer>

<footer th:fragment="copyParam (param1, param2)">
    <p>파라미터 자리 입니다.</p>
    <p th:text="${param1}"></p>
    <p th:text="${param2}"></p>
</footer>

</body>

</html>
```

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<h1>부분 포함</h1>
<h2>부분 포함 insert</h2>
<div th:insert="~{template/fragment/footer :: copy}"></div>

<h2>부분 포함 replace</h2>
<div th:replace="~{template/fragment/footer :: copy}"></div>

<h2>부분 포함 단순 표현식</h2>
<div th:replace="template/fragment/footer :: copy"></div>

<h1>파라미터 사용</h1>
<div th:replace="~{template/fragment/footer :: copyParam ('데이터1', '데이터2')}"></div>
</body>
</html>
```

#### 템플릿 레이아웃

```html
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="common_header(title,links)">

    <title th:replace="${title}">레이아웃 타이틀</title>

    <!-- 공통 -->
    <link rel="stylesheet" type="text/css" media="all" th:href="@{/css/awesomeapp.css}">
    <link rel="shortcut icon" th:href="@{/images/favicon.ico}">
    <script type="text/javascript" th:src="@{/sh/scripts/codebase.js}"></script>

    <!-- 추가 -->
    <th:block th:replace="${links}" />

</head>
```
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<!-- 이 부분이 핵심 -->
<head th:replace="template/layout/base :: common_header(~{::title},~{::link})">
    <title>메인 타이틀</title>
    <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/themes/smoothness/jquery-ui.css}">
</head>
<body>
메인 컨텐츠
</body>
</html>
```

[Thymeleaf 공식문서](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#template-layout)

#### 타임리프 - 스프링 통합

* Spring Boot에서 Thymeleaf를 사용하려면 아래와 같은 의존성을 추가해주어야 한다.

```
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

#### 입력 폼 처리

```html
<!-- form에서 사용할 객체 이름 modelAttribute로 지정 -->
<form action="item.html" th:action th:object="${item}" method="post"> 
        <div>
            <label for="itemName">상품명</label>
            <input type="text" id="itemName" th:field="*{itemName}" class="form-control" placeholder="이름을 입력하세요"> 
        </div>
        <div>
            <label for="price">가격</label>
            <input type="text" id="price" th:field="*{price}" class="form-control" placeholder="가격을 입력하세요">
        </div> 
        <div>
            <label for="quantity">수량</label>
            <input type="text" id="quantity" th:field="*{quantity}" class="form-control" placeholder="수량을 입력하세요"> 
        </div>
```

#### 체크 박스 - 단일

```html
<div class="form-check">
    <input type="checkbox" id="open" th:field="*{open}" class="form-check-input">
    <label for="open" class="form-check-label">판매 오픈</label>    
</div>
```

* 체크 박스를 th:field로 작성할 경우 id, name, value는 기본이고 히든필드까지 자동으로 넣어준다.
* 추가적으로 th:field에서 open 값이 true이면 checked 속성도 자동으로 만들어서 넣어준다.

#### 체크 박스 - 멀티

```html
<div>
    <div>등록 지역</div>
    <div th:each="region : ${regions}" class="form-check form-check-inline">
        <input type="checkbox" th:field="*{regions}" th:value="${region.key}" class="form-check-input">
        <label th:for="${#ids.prev('regions')}"
                th:text="${region.value}" class="form-check-label">서울</label>
    </div>
</div>
```

#### 라디오버튼

* 라디오 버튼의 경우 한 번 선택하면 다른 것으로 변경할 수 있으나 아예 선택을 안하는 것은 불가능하다.

```html
<div th:each="type : ${itemTypes}" class="form-check form-check-inline">
    <input type="radio" th:field="*{itemType}" th:value="${type.name()}" class="form-check-input">
    <label th:for="${#ids.prev('itemType')}" th:text="${type.description}" class="form-check-label"> Book </label>
</div>
```

#### 셀렉트 박스

```html
<div>배송 방식</div>
<select th:field="*{deliveryCode}" class="form-select">
    <option value="">==배송 방식 선택==</option>
    <option th:each="deliveryCode : ${deliveryCodes}" th:value="${deliveryCode.code}"
            th:text="${deliveryCode.displayName}">FAST</option>
</select>
```
