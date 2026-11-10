package com.lusle.android.soon.schema

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lusle.android.soon.model.schema.Alarm
import com.lusle.android.soon.model.schema.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * #11: Movie 의 non-null 필드가 JSON 에 없거나 null 이면 Gson 이 null 을 넣고,
 * 파싱 시점이 아니라 사용 시점(movie.title.trim() 등)에서 NPE 가 나던 결함.
 */
class MovieGsonTest {

    private val gson = Gson()

    @Test
    fun missing_fields_get_non_null_defaults() {
        val movie = gson.fromJson("""{"id": 42}""", Movie::class.java)
        assertEquals(42, movie.id)
        assertEquals("", movie.title.trim())
        assertEquals(0, movie.genreIds.size)
        assertEquals(0, movie.releaseDate.length)
        assertEquals("", movie.originalTitle)
        assertEquals("", movie.originalLanguage)
    }

    @Test
    fun explicit_nulls_get_non_null_defaults() {
        val json = """{"id": 7, "title": null, "genre_ids": null, "release_date": null,
            "original_title": null, "original_language": null, "poster_path": null}"""
        val movie = gson.fromJson(json, Movie::class.java)
        assertEquals("", movie.title.trim())
        assertEquals(0, movie.genreIds.size)
        assertEquals(0, movie.releaseDate.length)
        assertNull(movie.posterPath)
    }

    @Test
    fun present_fields_are_kept() {
        val json = """{"id": 1, "title": "Dune", "genre_ids": [878, 12], "release_date": "2021-10-20",
            "vote_average": 7.8, "poster_path": "/d5NXSklXo0qyIYkgV94XAgMIckC.jpg"}"""
        val movie = gson.fromJson(json, Movie::class.java)
        assertEquals("Dune", movie.title)
        assertEquals(listOf(878, 12), movie.genreIds)
        assertEquals("2021-10-20", movie.releaseDate)
        assertEquals(7.8, movie.voteAverage, 0.0001)
        assertEquals("/d5NXSklXo0qyIYkgV94XAgMIckC.jpg", movie.posterPath)
    }

    @Test
    fun stored_alarm_with_partial_movie_round_trips() {
        // ReleaseAlarmDataSource 가 SharedPreferences 에 저장하는 형식. 예전 버전이 저장한 값에 필드가 빠져 있을 수 있다
        val type = object : TypeToken<ArrayList<Alarm>>() {}.type
        val json = """[{"movie": {"id": 3, "title": null}, "milliseconds": 0, "pendingIntentID": 1, "active": true}]"""
        val alarms: ArrayList<Alarm> = gson.fromJson(json, type)
        assertEquals("", alarms[0].movie.title)
        assertEquals("", alarms[0].movie.releaseDate)
        val again: ArrayList<Alarm> = gson.fromJson(gson.toJson(alarms, type), type)
        assertEquals(alarms[0].movie, again[0].movie)
        assertEquals(3, again[0].movie.id)
    }
}
