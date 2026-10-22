# (Effective Kotlin Item 55) Kotlin - 요소들을 맵으로 묶는 것을 고려하라

#### 요소들을 맵으로 묶는 것을 고려하라

* 요소의 수가 많은 집합에서 키(Key)로 요소를 찾아야 할 때가 많다.
  * 하나 이상의 파일에서 로드된 설정을 저장하는 클래스
  * 다운로드된 데이터를 저장하는 네트워크 리포지토리
  * 인메모리 리포지토리(테스트를 위해 사용되는 리포지토리)
* 이런 종류 데이터는 사용자, 아이디, 설정 등을 목록으로 가지고 있다. 데이터를 가져오는 형태가 주로 리스트가 될 것이므로, 메모리에도 리스트로 저장하고 싶을 것이다.

```kotlin
// ❌ Bad : 조회할 때마다 리스트 전체를 훑는다 — 호출 1회당 O(n)
class StudentRepository(private val students: List<Student>) {
    fun findById(id: Int): Student? = students.find { it.id == id }
}

// 성적 5만 건에 대해 학생을 찾으면 O(n × m)
grades.forEach { grade -> repository.findById(grade.studentId) }
```

* 하지만 리스트로 저장하는 것은 좋은 방법이 아니다. 
* 리스트에서 요소를 찾는 것은 선형 복잡도(O(N)), 여기서 N은 리스트 크기. 평균적으로 리스트에서 요소를 찾기 위해 N/2번의 비교를 거친다.
* 리스트 규모가 큰 경우 문제가 될 수 있다. 요소를 찾을 때마다 리스트에 있는 다른 요소들과 비교해야 하기 때문이다.
* `List`대신 `Map`을 사용하는 것이 훨씬 더 좋다. Kotlin은 기본적으로 해시맵(LinkedHashMap)을 사용한다.
* JVM에서 요소는 단 한 번의 비교만으로 찾을 수 있는데, 사용되는 해시 맵의 크기가 맵 자체의 크기에 맞게 조정되기 때문이다.

#### 요소를 키로 엮기

* 리스트를 맵으로 혹은 그 반대로 어떻게 변환할지가 중요하다.
* `associate()` 메서드를 사용하면 순회 가능한 객체를 맵으로 변경할 수 있다. 이 메서드는 각 요소에 대해 키-값(key-value) 쌍을 생성한다.
* `associateBy()` 메서드는 기존 요소를 값으로 지정하고 어떤 것을 키로 할 것인지 정하면 된다.
* 맵의 키는 고유해야 하므로, 중복된 키는 제거된다는 점에 유의해야한다. 고유하지 않은 기준으로 그룹화하려면 `groupBy()` 함수를 사용한다.
* 요소들을 키와 연관지어 맵으로 만들게 되면 코드 성능을 향상시킬 수 있다. 요소를 자주 접근해야 한다면 반드시 맵으로 만드는 것이 좋다.

```kotlin
// ❌ Bad
class ConfigurationRepository(
    private val configurations: List<Configuration>
) {
    fun getByName(name: String) = configurations
        .firstOrNull { it.name == name }  // 조회마다 평균 n/2 비교 → O(n)
}
```

```kotlin
// ⭕ Good
class ConfigurationRepository(
    configurations: List<Configuration>  // 프로퍼티로 두지 않고 받기만 한다
) {
    private val configurations: Map<String, Configuration> = configurations.associateBy { it.name }  // 생성 시 한 번, O(n)

    fun getByName(name: String) = configurations[name]  // 이후 O(1)
}
```

* 맵을 사용하게 변경하면 설정과 사용자를 즉시 찾을 수 있다. 원하는 만큼 조회해도 성능에 문제가 되지 않는다.
* 물론 키와 연관지어 요소를 맵으로 만드는 것도 비용이 들지만, 리스트에서 요소를 찾는 것과 마찬가지로 선형 복잡도를 갖는 작업이다.
* 좀 더 복잡한 연산을 처리할 때도 맵으로 만들면 효과를 볼 수 있다.
