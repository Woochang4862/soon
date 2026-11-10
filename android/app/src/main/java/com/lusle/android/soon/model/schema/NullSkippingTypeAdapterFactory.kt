package com.lusle.android.soon.model.schema

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * JSON 객체에서 값이 null 인 항목을 버린 뒤 기본 어댑터로 넘긴다.
 *
 * Gson 은 Kotlin non-null 타입을 모른다. 명시적 null 은 필드에 그대로 대입되고
 * 사용 시점에서야 NPE 가 난다(#11). 모든 생성자 인자에 기본값이 있는 클래스에
 * `@JsonAdapter(NullSkippingTypeAdapterFactory::class)` 로 붙이면, null 항목은 "누락"으로
 * 취급돼 Kotlin 기본값이 남는다. 어노테이션 방식이라 Retrofit·SharedPreferences 등
 * 어느 Gson 인스턴스로 읽어도 적용된다.
 */
class NullSkippingTypeAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegate = gson.getDelegateAdapter(this, type)
        val elementAdapter = gson.getAdapter(JsonElement::class.java)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                delegate.write(out, value)
            }

            override fun read(reader: JsonReader): T? {
                val tree = elementAdapter.read(reader)
                if (tree.isJsonObject) {
                    val obj = tree.asJsonObject
                    obj.entrySet().filter { it.value.isJsonNull }.map { it.key }.forEach { obj.remove(it) }
                }
                return delegate.fromJsonTree(tree)
            }
        }
    }
}
