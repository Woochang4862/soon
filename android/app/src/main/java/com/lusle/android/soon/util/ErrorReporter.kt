package com.lusle.android.soon.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * 잡은 예외를 로그와 Crashlytics(non-fatal) 양쪽에 남긴다.
 * 예전에는 catch 블록이 printStackTrace() 만 해서 개발자에게 아무것도 전달되지 않았다(#3).
 */
object ErrorReporter {

    private const val TAG = "SoonError"

    @JvmStatic
    @JvmOverloads
    fun report(e: Throwable?, message: String? = null) {
        if (e == null) {
            Log.e(TAG, message ?: "null throwable reported")
            return
        }
        Log.e(TAG, message ?: e.javaClass.simpleName, e)
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            message?.let { crashlytics.log(it) }
            crashlytics.recordException(e)
        } catch (unavailable: Exception) {
            // FirebaseApp 이 없는 환경(JVM 단위 테스트 등)에서는 로그만 남긴다
            Log.w(TAG, "Crashlytics unavailable: ${unavailable.message}")
        }
    }
}
