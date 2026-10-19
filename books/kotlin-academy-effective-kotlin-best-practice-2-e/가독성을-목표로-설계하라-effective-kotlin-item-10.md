# (Effective Kotlin Item 10) Kotlin - 가독성을 목표로 설계하라

#### 가독성을 목표로 설계하라

```kotlin
// 구현 A
if (person != null && person.isAdult) {
    view.showPerson(person)
} else {
    view.showError()
}
```
```kotlin
// 구현 B
person?.takeIf { it.isAdult }
      ?.let(view::showPerson)
      ?: view.showError()
```

* 사람마다 가독성이 무엇을 의미하는지에 대한 관점이 다르다.
* 코틀린은 간결성이 아닌 가독성을 목표로 설계되었지만, 다른 인기 있는 언어에 비해서는 간결하다.
* **코드의 주인공은 작성자가 아니라 읽는 사람이다. 복잡한 연쇄 호출이 멋있어 보일지 몰라도 나중에 유지보수하는 동료가 "이게 도대체 어떻게 돌아가는거지?"라고 고민하게 만드는 순간 기술 부채가 된다는 점을 인지해야 한다.**
