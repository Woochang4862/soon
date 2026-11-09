package com.lusle.android.soon.paging

import androidx.paging.PagingSource
import com.lusle.android.soon.model.api.CompanyApi
import com.lusle.android.soon.model.schema.CompanyResult
import com.lusle.android.soon.model.schema.SubscribeCheckResult
import com.lusle.android.soon.model.schema.SubscribedTopicsResult
import com.lusle.android.soon.model.source.SearchCompanyPageKeyDataSource
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * #5: 네트워크 오류는 PagingSource 가 LoadResult.Error 로 바꿔 돌려준다.
 * 예외가 collectLatest 까지 던져지지 않으므로, launch 바깥의 try/catch 는 물론
 * launch 안의 try/catch 로도 잡을 수 없다. 오류는 LoadState 로만 관측된다.
 */
class SearchCompanyPageKeyDataSourceTest {

    private class FailingCompanyApi : CompanyApi {
        override fun searchCompany(query: String, region: String, page: Int): Single<CompanyResult> =
            Single.error(IOException("failed to connect"))
        override fun checkSubscribedTopics(token: String): Single<SubscribedTopicsResult> = throw UnsupportedOperationException()
        override fun checkSubscribeTo(token: String, topic: String): Single<SubscribeCheckResult> = throw UnsupportedOperationException()
        override fun addCompanyAlarm(body: HashMap<String, String>): Single<ResponseBody> = throw UnsupportedOperationException()
        override fun removeCompanyAlarm(body: HashMap<String, String>): Single<ResponseBody> = throw UnsupportedOperationException()
    }

    @Test
    fun network_failure_is_returned_as_LoadResult_Error_not_thrown() = runBlocking {
        val source = SearchCompanyPageKeyDataSource(FailingCompanyApi(), "disney", "KR")
        val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false))
        assertTrue("expected LoadResult.Error but was $result", result is PagingSource.LoadResult.Error)
    }
}
