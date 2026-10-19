# Redis - DAU Counting Application

## 📚 Redis - DAU 카운팅 수집하기

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/91f6def0-e5fe-48f2-81bb-87fa771acf24.png)

* Sorted Set은 score(점수)를 기준으로 자동 정렬되는 중복없는 자료 구조다. 
* Set은 정렬되지 않는 중복 없는 자료구조를 뜻한다. 이런 특성 때문에 Set은 순서는 중요하지 않지만 중복되면 안 되는 데이터를 다룰 때 적합하다.

```shell
# Set에 데이터 저장
# SADD [key] [member...]
$ SADD member yeonwoo 
$ SADD member jihoon jaeseong # 한 번에 여러 개의 값을 넣을 수도 있음
$ SADD member jaeseong yeonwoo # 테스트를 위해 중복되는 값 넣어보기

$ keys * # Set이 잘 생성됐는 지 확인하기

# SET에 들어있는 모든 데이터 조회
# SMEMBERS [key]
$ SMEMBERS member # 중복이 제거된 채로 데이터가 조회되는 걸 확인하기 (정렬도 되어 있지 않음)

# SET에 들어있는 데이터 개수 조회
# SCARD [key]
$ SCARD member
```

## 📚 [Bitmap](https://redis.io/docs/latest/develop/data-types/bitmaps/)

* Redis에서 Bitmap은 아주 적은 메모리로 대량의 boolean 상태를 저장할 때 사용하는 자료구조이다. 
* Bitmap의 가장 큰 장점은 공간 효율성이다. 일반적으로 boolean 값을 하나 저장하려면 8bit 이상이 필요하지만, Bitmap은 1bit만 사용하기 때문에 수백만~수억 개의 상태값도 매우 적은 메모리로 관리할 수 있다.
* 이러한 특징 때문에 Bitmap은 아래와 같은 예시에서 많이 활용된다. 
  * 사용자별 일일 출석 여부 확인 (출석 : true / 결석 : false)
  * 대규모 이벤트에서 사용자별 참여 여부 (참석 : true / 불참 : false)
  * 하루 동안 서비스에 방문한 사용자 수인 DAU 구하기 (방문 : true / 방문 X : false)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/36b67512-adc6-4b17-b345-9b2f8a5549c4.png)

* `key`, `offset`, `value`의 구성 요소를 가지고 있다.
* `offset`에는 0 이상의 정수만 넣을 수 있다. 인덱스(index)라고 생각하면 편하다.
* `value`에는 0 또는 1의 숫자만 들어갈 수 있다. 이 값을 활용해서 true(1), false(0)를 표현하면 된다.

```shell
# Bitmap 데이터 저장
# SETBIT [key] [offset] [value]
$ SETBIT user:attend 4 1
$ SETBIT user:attend 7 1# Bitmap이 잘 저장됐는 지 key로 확인하기
$ keys *

# Bitmap에서 특정 offset의 value 확인하기
# GETBIT [key] [offset] 
$ GETBIT user:attend 2 # offset에 아무값도 저장되어 있지 않을 때는 0으로 출력
$ GETBIT user:attend 4 
$ GETBIT user:attend 7

# 특정 Bitmap에 value가 1로 저장된 모든 데이터의 개수를 카운팅하기
# BITCOUNT [key]
$ BITCOUNT user:attend
```

> ❓Set과 Bitmap은 어떤 점이 다를까?
>
> * Set과 Bitmap은 둘 다 중복을 허용하지 않는 자료구조라는 점이 공통점이다.<br>
> * Set은 실제 값을 있는 그대로 저장하는 자료구조이다. 문자열, 숫자, UUID 등 다양한 형태의 데이터를 그대로 담을 수 있다. 반면, Bitmap은 값을 있는 그대로 저장하지 않고, 정수 offset에 대응되는 bit(0또는 1)만 기록하는 구조이다. 그래서 문자열, UUID와 같은 값은 저장할 수 없다.<br>
> * Set은 member 하나하나가 메모리를 차지하기 때문에 데이터 수가 늘어날수록 메모리 사용량이 선형적으로 증가한다. 반면, Bitmap은 데이터 하나당 1bit만 사용하므로 대규모 데이터를 저장해야 하는 상황에서도 매우 적은 메모리로 데이터를 저장할 수 있다. 다만, Bitmap은 0과 1만 저장할 수 있고 문자열, UUID와 같은 값은 저장할 수 없다는 제약이 있다.<br>
> * Set은 데이터 목록 조회, 집합 연산 등 데이터 자체를 다룰 수 있는 명령어를 제공한다. 반면, Bitmap은 데이터 목록을 조회할 수 없고 bit로 저장되기 때문에 데이터를 사람이 읽기도 불편하지만, 대량의 boolean 상태를 적은 메모리 용량으로 처리하는 데 특화되어 있다.<br>
