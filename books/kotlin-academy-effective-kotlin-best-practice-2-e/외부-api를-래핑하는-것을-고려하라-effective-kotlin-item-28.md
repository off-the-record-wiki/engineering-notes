# (Effective Kotlin Item 28) Kotlin - 외부 API를 래핑하는 것을 고려하라

#### 외부 API를 래핑하는 것을 고려하라

* 변경될 가능성이 높은 API를 과도하게 사용하는 것은 위험하다.
* 개발자가 불안정하다고 명시되어 있거나 API의 안정성을 신뢰할 수 없는 경우 모두 마찬가지이다.
* API가 불가피하게 변경되었을 때 API가 사용된 모든 곳을 변경해야 함을 기억해야 한다.
* 바로 이런 이유 때문에 변경 가능성이 높은 외부 라이브러리 API를 래핑하는 것이다.
* API를  래핑하면 다음과 같은 장점들이 있다.
  * 외부 API가 변경되더라도 래퍼(wrapper) 내에서 사용 중인 곳만 변경하면 된다.
  * 프로젝트 스타일과 로직에 맞게 API를 조정할 수 있다.
  * 라이브러리에 문제가 발생할 경우 다른 라이브러리로 교체할 수 있다.
  * 필요하다면 사용한 API와 다르게 동작하게 만들 수 있다.
* API를 래핑하는 것에는 다음과 같은 단점들도 있다.
  * 모든 래퍼를 정의해야 한다.
  * 단지 한 프로젝트 때문에 개발자가 내부 API를 배워야 한다.
  * 내부 API 작동 방식을 가르치는 과정이 없다.

```kotlin
// 한 줄로 이미지를 불러와 ImageView에 넣어주는 라이브러리
Picasso.get()
    .load("https://example.com/image.png")
    .into(imageView)
```
```kotlin
// ❌ Bad - 앱 곳곳에서 Picasso를 직접 호출한다
class ProfileView {
    fun showAvatar(url: String, imageView: ImageView) {
        Picasso.get().load(url).into(imageView)  // ❌ Picasso에 직접 결합
    }
}

class FeedView {
    fun showThumbnail(url: String, imageView: ImageView) {
        Picasso.get().load(url).into(imageView)  // ❌ 여기도 직접
    }
}
```
```kotlin
// ⭕ Good - '무엇을 하는가'만 정의한 우리만의 계약
interface ImageLoader {
    fun loadImage(url: String, imageView: ImageView)
}

// Picasso는 이 구현체 안에만 갇힌다 — 유일한 접점
class PicassoImageLoader : ImageLoader {
    override fun loadImage(url: String, imageView: ImageView) {
        Picasso.get().load(url).into(imageView)
    }
}

// 사용하는 쪽은 Picasso를 전혀 모른다
class ProfileView(private val imageLoader: ImageLoader) {
    fun showAvatar(url: String, imageView: ImageView) {
        imageLoader.loadImage(url, imageView)  // ✅ 계약에만 의존
    }
}
```
* 래핑하는 것은 이보다 더 복잡할 수 있지만 그만한 가치가 있다.
* 외부 API를 래핑하면 다른 라이브러리로 쉽게 변경할 수 있으며 API 변경 사항이 있더라도 간단하게 대처할 수 있다.
