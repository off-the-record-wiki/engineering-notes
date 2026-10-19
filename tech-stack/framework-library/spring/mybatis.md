# Spring - MyBatis

#### MyBatis

* MyBatis는 앞서 설명한 JdbcTemplate보다 더 많은 기능을 제공하는 SQL Mapper이다.
* 기본적으로 `JdbcTemplate`가 제공하는 대부분의 기능을 제공한다.
* `JdbcTemplate`과 비교해서 MyBatis의 가장 매력적인 기능은 SQL을 XML에 편리하게 작성할 수 있고 동적 쿼리를 매우 편리하게 작성할 수 있다는 점이다.

> [참고](https://mybatis.org/mybatis-3/ko/index.html)

#### MyBatis 설정

```java
package com.jwj.myBatis.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemMapper {
    void save(Item item);
     
    void update(@Param("id") Long id, @Param("updateParam") ItemUpdateDto updateParam);
    
    Optional<Item> findById(Long id);
    
    List<Item> findAll(ItemSearchCond itemSearch);
}
```

* MyBatis 매핑 XML을 호출해주는 매퍼 인터페이스이다.
* 이 인터페이스에는 `@Mapper` 어노테이션을 붙여주어야 MyBatis에서 인식할 수 있다.
* 이 인터페이스 메서드를 호출하면 다음에 보이는 `.xml`의 해당 SQL을 실행하고 결과를 반환한다.
* 이 때, `.xml` 코드는 자바 코드가 아니기 때문에 `/src/main/resources` 하위에 만들되, 패키지 위치는 반드시 맞추어 주어야 한다.
 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
"http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="hello.itemservice.repository.mybatis.ItemMapper">

<insert id="save" useGeneratedKeys="true" keyProperty="id">
  insert into item (item_name, price, quantity) values (#{itemName}, #{price}, #{quantity})
</insert>

<update id="update">
  update item
  set item_name=#{updateParam.itemName},
  price=#{updateParam.price},
  quantity=#{updateParam.quantity}
  where id = #{id}
</update>

<select id="findById" resultType="Item">
  select id, item_name, price, quantity from item where id = #{id}
</select>

<select id="findAll" resultType="Item">
  select id, item_name, price, quantity. from item
  <where>
    <if test="itemName != null and itemName != ''">
      and item_name like concat('%',#{itemName},'%')
    </if>
    <if test="maxPrice != null">
      and price &lt;= #{maxPrice}
    </if>
  </where>
</select>
</mapper>
```

* `namespace` : 만든 매퍼 인터페이스를 지정한다.
* XML을 커스텀 경로에 두고 싶다면 `application.yml` 혹은 `application.properties`에서 `mybatis.mapper-locations` 프로퍼티를 수정하면 된다.

### [ insert - save ]

```xml
<insert id="save" useGeneratedKeys="true" keyProperty="id">
  insert into item (item_name, price, quantity) values (#{itemName}, #{price}, #{quantity})
</insert>
```

* 파라미터는 `#{}` 문법을 사용하면 된다. 그리고 매퍼에서 넘긴 객체의 프로퍼티 이름을 적어준다.
* `#{}` 문법을 사용하면 `PreparedStatement`를 사용한다
* `useGeneratedKeys`는 데이터베이스가 키를 생성해주는 `IDENTITY` 전략일 때 사용한다. `keyProperty`는 생성되는 키의 속성 이름을 지정한다.

### [ update - update ]

```xml
<update id="update">
  update item
  set item_name=#{updateParam.itemName},
  price=#{updateParam.price},
  quantity=#{updateParam.quantity}
  where id = #{id}
</update>
```

* 파라미터가 1개만 있으면 굳이 `@Param`을 지정하지 않아도 가능하나 파라미터가 2개 이상이라면 `@Param`으로 이름을 지정해서 파라미터를 반드시 구분해주어야 한다.
* MyBatis는 `<where>`, `<if>`같은 동적 쿼리 문법을 통해 편리한 동적 쿼리를 지원한다.
* `<if>`는 해당 조건이 만족하면 구문을 추가한다.
* `<where>`는 적절하게 WHERE 조건절을 만든다.
* 이 때, `<if>`가 하나라도 성공하면 처음 나타나는 `and`를 WHERE절로 변환한다.
* XML에서 비교하는 조건을 써야하는 경우 TAG를 사용할 수 없다. 다른 해결 방안으로는 XML에서 지원하는 CDATA 구문 문법을 사용하면 되는데, 특수문자와 CDATA 각각 상황에 따른 장단점이 있기에 원하는 방법을 그때그때 선택하면 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c46c4510-6f80-4772-977c-68c07b4e0966.png)

1. 애플리케이션 로딩 시점에 MyBatis 스프링 연동 모듈은 `@Mapper` 어노테이션이 붙은 인터페이스를 조사한다.
2. 해당 인터페이스가 발견되면 동적 프록시 기술을 사용해서 매퍼의 인터페이스의 구현체를 만든다.
3. 생성된 구현체를 스프링 컨테이너에 스프링 빈으로 등록한다.
* 매퍼 구현체는 예외 변환까지 처리해준다. MyBatis에서 발생한 예외를 스프링 예외 추상화인 `DataAccessException`에 맞게 변환해서 반환해준다.

#### MyBatis 기본

> [참고](https://mybatis.org/spring/ko/index.html)<br>
> [동적 쿼리](https://mybatis.org/mybatis-3/ko/dynamic-sql.html)<br>

* MyBatis가 제공하는 최고의 기능이자 MyBatis를 사용하는 이유가 바로 동적 SQL 기능 때문이다.
* 동적 쿼리를 위해 제공하는 기능은 다음과 같다.
  * `if`
  * `choose(when, otherwise)`
  * `trim(where, set)`
  * `foreach`

#### MyBatis 기타 기능

* XML 대신에 어노테이션에 SQL을 직접 작성할 수 있다.
* `@Insert` , `@Update` , `@Delete` , `@Select` 기능이 제공된다.
* 이 경우 XML에는 쿼리를 제거해야 한다. 단, 이 어노테이션은 동적 SQL이 해결되지 않기 때문에 간단한 경우에만 사용한다.

>❗주의할 점
>
> * `${}`를 사용하면 SQL 인젝션 공격을 당할 수 있다. 따라서 가급적 사용하면 안 된다.<br>
> * MyBatis에서도 매우 복잡한 결과에 객체 연관관계를 고려해서 데이터를 조회하는 것이 가능한데, 이 때는 `<association>`, `<collection>` 등을 사용한다. 이 부분은 성능과 실효성 측면에서 많은 고민을 필요로 한다.<br>
> * JPA는 객체와 관계형 데이터베이스를 ORM 개념으로 매핑하기 때문에 이런 부분이 자연스럽지만 MyBatis에서는 들어가는 공수가 많고 성능을 최적화하기 어렵다. 따라서 해당 기능을 사용할 때는 신중하게 사용해야 한다.<br>
> * [결과 매핑](https://mybatis.org/mybatis-3/ko/sqlmap-xml.html#Result_Maps)
