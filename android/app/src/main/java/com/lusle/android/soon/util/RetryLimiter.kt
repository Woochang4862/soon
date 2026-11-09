package com.lusle.android.soon.util

/**
 * 사용자가 누르는 "재시도" 횟수를 제한한다.
 * CompanyListActivity 의 대화상자 분기를 동작 변경 없이 옮겨 온 것이다(#8 재현용).
 */
class RetryLimiter(private val maxRetries: Int) {

    var count: Int = 0
        private set

    /** 재시도 버튼이 눌렸을 때 부른다. true 면 재시도한다. */
    fun tryAcquire(): Boolean {
        var allowed = false
        if (count <= maxRetries) {
            count += 1
            allowed = true
        }
        count = 0
        return allowed
    }

    /** 로드가 성공했을 때 부른다. */
    fun reset() {
        count = 0
    }
}
