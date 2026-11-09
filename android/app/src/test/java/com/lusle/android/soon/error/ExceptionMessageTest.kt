package com.lusle.android.soon.error

import com.lusle.android.soon.view.detail.CreditsNotFoundException
import com.lusle.android.soon.view.detail.MovieDetailNotFoundException
import com.lusle.android.soon.view.detail.SimilarMovieNotFoundException
import com.lusle.android.soon.view.detail.WatchProviderNotFoundException
import com.lusle.android.soon.view.main.genre.GenreNotFoundException
import com.lusle.android.soon.view.main.setting.company_alarm.SubscribeCompanyException
import com.lusle.android.soon.view.main.setting.company_alarm.UnsubscribeCompanyException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * #3: 커스텀 예외의 message 에 `${e.stackTrace}`(배열 toString)가 들어가고,
 * 일부는 cause 를 넘기지 않아 원인이 사라지던 결함.
 */
class ExceptionMessageTest {

    private val cause = IOException("connect timed out")

    private fun wrapped(): List<Exception> = listOf(
        MovieDetailNotFoundException(cause),
        CreditsNotFoundException(cause),
        WatchProviderNotFoundException(cause),
        SimilarMovieNotFoundException(cause),
        GenreNotFoundException(cause),
        SubscribeCompanyException(cause),
        UnsubscribeCompanyException(cause),
    )

    @Test
    fun message_does_not_contain_stack_trace_array_toString() {
        for (e in wrapped()) {
            val message = e.message ?: ""
            assertFalse("${e.javaClass.simpleName}: $message", message.contains("StackTraceElement;@"))
        }
    }

    @Test
    fun message_contains_cause_message() {
        for (e in wrapped()) {
            assertTrue("${e.javaClass.simpleName}: ${e.message}", e.message!!.contains("connect timed out"))
        }
    }

    @Test
    fun cause_is_preserved() {
        for (e in wrapped()) {
            assertSame(e.javaClass.simpleName, cause, e.cause)
        }
    }
}
