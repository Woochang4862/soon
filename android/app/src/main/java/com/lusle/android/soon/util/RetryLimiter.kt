package com.lusle.android.soon.util

/**
 * 사용자가 누르는 "재시도" 횟수를 제한한다.
 * 예전 코드는 조건 밖에서 카운터를 매번 0 으로 되돌려 제한이 걸리지 않았다(#8).
 * 카운터는 로드가 성공했을 때만 reset() 으로 되돌린다.
 */
class RetryLimiter(private val maxRetries: Int) {

    var count: Int = 0
        private set

    /** 재시도 버튼이 눌렸을 때 부른다. true 면 재시도한다. */
    fun tryAcquire(): Boolean {
        if (count >= maxRetries) return false
        count += 1
        return true
    }

    /** 로드가 성공했을 때 부른다. */
    fun reset() {
        count = 0
    }
}
