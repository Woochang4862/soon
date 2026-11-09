package com.lusle.android.soon.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import com.lusle.android.soon.util.refreshError
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class RefreshErrorTest {

    private fun states(refresh: LoadState, append: LoadState = LoadState.NotLoading(false)): CombinedLoadStates {
        val source = LoadStates(refresh, LoadState.NotLoading(false), append)
        return CombinedLoadStates(refresh, LoadState.NotLoading(false), append, source, null)
    }

    @Test
    fun refresh_error_returns_cause() {
        val cause = IOException("failed to connect")
        assertSame(cause, states(LoadState.Error(cause)).refreshError())
    }

    @Test
    fun loading_or_success_returns_null() {
        assertNull(states(LoadState.Loading).refreshError())
        assertNull(states(LoadState.NotLoading(true)).refreshError())
    }

    @Test
    fun append_error_is_not_refresh_error() {
        assertNull(states(LoadState.NotLoading(false), LoadState.Error(IOException())).refreshError())
    }
}
