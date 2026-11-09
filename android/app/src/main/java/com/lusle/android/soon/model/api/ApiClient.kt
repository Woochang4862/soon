package com.lusle.android.soon.model.api

import retrofit2.Retrofit
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.lusle.android.soon.BuildConfig
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 20L
    private const val CALL_TIMEOUT_SECONDS = 30L

    @JvmStatic
    val retrofit: Retrofit
        get() {
            return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(
                    OkHttpClient.Builder()
                        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .addNetworkInterceptor(StethoInterceptor())
                        .addInterceptor(HttpLoggingInterceptor().apply {
                            // 디버그 빌드에서만 요청·응답 본문까지 남긴다. 릴리스는 로그 없음
                            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                            else HttpLoggingInterceptor.Level.NONE
                        }).build()
                )
                .baseUrl("http://34.229.27.130:3000")
                .build()
        }
}
