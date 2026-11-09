package com.lusle.android.soon.util

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

/**
 * 새로고침(첫 페이지) 로드가 실패했으면 그 원인을, 아니면 null 을 돌려준다.
 * PagingSource 는 네트워크 예외를 LoadResult.Error 로 바꿔 돌려주므로
 * collect 를 감싼 try/catch 로는 잡을 수 없고 LoadState 로만 관측된다(#5).
 */
fun CombinedLoadStates.refreshError(): Throwable? = (refresh as? LoadState.Error)?.error
