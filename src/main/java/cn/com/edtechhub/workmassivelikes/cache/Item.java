package cn.com.edtechhub.workmassivelikes.cache;

public record Item(String key, int count) {
}
/*
这个 record 类型的类会自动生成以下内容：
(1)Person(String name, int age)
(2)key(), count()
(3)toString()
(4)equals() 和 hashCode()
*/
