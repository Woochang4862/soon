package com.lusle.android.soon.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #8: 재시도 카운터가 매번 0 으로 리셋돼 MAX_RETRY_NUM 이 무력화되던 결함 */
class RetryLimiterTest {

    @Test
    fun allows_exactly_max_retries_then_refuses() {
        val limiter = RetryLimiter(5)
        repeat(5) { i -> assertTrue("retry #${i + 1}", limiter.tryAcquire()) }
        assertFalse("retry #6 must be refused", limiter.tryAcquire())
    }

    @Test
    fun count_accumulates_between_retries() {
        val limiter = RetryLimiter(5)
        limiter.tryAcquire()
        limiter.tryAcquire()
        assertEquals(2, limiter.count)
    }

    @Test
    fun reset_after_success_allows_retries_again() {
        val limiter = RetryLimiter(2)
        limiter.tryAcquire()
        limiter.tryAcquire()
        assertFalse(limiter.tryAcquire())
        limiter.reset()
        assertTrue(limiter.tryAcquire())
    }
}
