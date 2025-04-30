package cn.com.edtechhub.workmassivelikes.cache;

/**
 * 描述 TopK 数据结构中的元素
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public record Item(String key, int count) {
}
/*
这个 record 类型的类会自动生成以下内容：
(1)Person(String name, int age)
(2)key(), count()
(3)toString()
(4)equals() 和 hashCode()
*/
