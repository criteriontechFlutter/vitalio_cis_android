package com.critetiontech.ctvitalio.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppGsonTest {

    data class Item(val name: String, val note: String?, val count: Int = 0)
    data class Reply(val status: Int, val message: String, val title: String?, val items: List<Item>)

    @Test
    fun `null text in a non-null String becomes empty`() {
        val r = AppGson.fromJson("""{"status":1,"message":null,"title":null,"items":[{"name":null,"note":null}]}""", Reply::class.java)
        assertEquals("", r.message)
        assertEquals("", r.items[0].name)
    }

    @Test
    fun `nullable fields stay null so fallbacks still work`() {
        val r = AppGson.fromJson("""{"status":1,"message":"ok","title":null,"items":[{"name":"A","note":null}]}""", Reply::class.java)
        assertNull(r.title)
        assertNull(r.items[0].note)
    }

    @Test
    fun `missing or null list becomes empty`() {
        val r = AppGson.fromJson("""{"status":1,"message":"ok","items":null}""", Reply::class.java)
        assertEquals(emptyList<Item>(), r.items)
    }

    @Test
    fun `values that are present are untouched`() {
        val r = AppGson.fromJson("""{"status":1,"message":"ok","title":"T","items":[{"name":"A","note":"n","count":3}]}""", Reply::class.java)
        assertEquals(Reply(1, "ok", "T", listOf(Item("A", "n", 3))), r)
    }

    @Test
    fun `arrays of models are filled too`() {
        val list = AppGson.fromJson("""[{"name":null,"note":null}]""", Array<Item>::class.java)
        assertEquals("", list[0].name)
    }
}
