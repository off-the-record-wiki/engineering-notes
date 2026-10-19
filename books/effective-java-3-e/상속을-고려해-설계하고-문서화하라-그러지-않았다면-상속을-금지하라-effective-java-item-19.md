# (Effective Java Item 19) Java ‐ 상속을 고려해 설계하고 문서화하라. 그러지 않았다면 상속을 금지하라

#### 상속을 고려해 설계하고 문서화하라. 그러지 않았다면 상속을 금지하라

* 상속용 클래스는 재정의할 수 있는 메서드들을 내부적으로 어떻게 동작하는지 문서로 남겨야 한다.
* 호출되는 메서드가 재정의 가능한 메서드라면 그 사실을 호출하는 메서드 API 설명에 명시해야 한다.
* 상속은 캡슐화를 해치기 때문에 이 문서화는 반드시 해야하는 작업이며, 클래스를 안전하게 상속할 수 있도록 하려면 내부 구현 방식을 설명해야 한다.
* `Implementation notes` 하위에 문서로 기술하고 있다.

```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 362498820763181265L;

    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */

}
```

```java
public class Super {
    public Super() {
        overrideMe(); // 생성자가 재정의 가능한 메서드를 호출한다.
    }

    public void overrideMe() {
    }
}
public final class Sub extends Super {
    private final Instacnce instacnce;
    
    Sub() {
        instacnce = Instacnce.now(); // create instance in constructor
    }

    @Override
    public void overrideMe() {
        System.out.println(instacnce);
    }

    public static void main(String[] args) {
        Sub sub = new Sub();
        /*
         * instance 가 null / instance 두 개 출력 된다
         * 상위 클래스의 생성자는 하위 클래스의 생성자가 인스턴스 필드를 초기화하기 전에 overrideMe 호출
         * */
        sub.overrideMe();
    }
}
```
* 상속용 클래스 생성자는 직접적으로든 간접적으로든 재정의 가능한 메서드를 호출해서는 안 된다.
* 위 규칙을 지키지 않으면 프로그램의 오작동을 일으킬 수 있다.
* 상위 클래스 생성자가 명시되지 않았다하더라도 하위 클래스 생성자보다 먼저 실행되기 때문이다.
* 상속용으로 설계하지 않은 클래스는 상속을 금지하는 것이다.
* 클래스에 final 키워드를 붙이거나 모든 생성자를 private, package-private으로 선언하고 public 정적 팩토리를 만드는 것이다.
* 굳이 상속을 허용하겠다면 문서를 만들어라.
