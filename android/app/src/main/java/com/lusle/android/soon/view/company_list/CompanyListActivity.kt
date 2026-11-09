package com.lusle.android.soon.view.company_list

import com.lusle.android.soon.util.ErrorReporter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.paging.LoadState
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lusle.android.soon.R
import com.lusle.android.soon.adapter.CompanyPagedListAdapter
import com.lusle.android.soon.adapter.decoration.CompanyItemDecoration
import com.lusle.android.soon.model.source.RegionCodeRepository
import com.lusle.android.soon.view.movie_list.MovieListActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.lusle.android.soon.util.refreshError
import com.lusle.android.soon.util.RetryLimiter

class CompanyListActivity : AppCompatActivity() {

    private lateinit var companyListRecyclerView: RecyclerView
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: CompanyPagedListAdapter
    private var query: String? = null
    private var shownError: Throwable? = null

    private val retryLimiter = RetryLimiter(MAX_RETRY_NUM)

    private val viewModel by viewModels<CompanyListViewModel> {
        CompanyListViewModelFactory(RegionCodeRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_list)

        companyListRecyclerView = findViewById(R.id.company_list)
        layoutManager = GridLayoutManager(this, 3)
        if (companyListRecyclerView.itemDecorationCount == 0)
            companyListRecyclerView.addItemDecoration(CompanyItemDecoration(this))
        companyListRecyclerView.layoutManager = layoutManager
        adapter = CompanyPagedListAdapter { view, position ->
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("keyword", adapter.getCompanyItem(position))
            startActivity(intent)
        }
        companyListRecyclerView.adapter = adapter
        observeLoadError()

        query = intent.getStringExtra("query")

        query?.let {
            load(it)
        } ?: run {
            AlertDialog.Builder(this)
                .setMessage("제작사 정보를 불러오는 데 문제가 생겼습니다.")
                .setNegativeButton("닫기") {dialogInterface, i->
                    finish()
                }
                .show()
        }
    }

    private fun load(query: String) {
        // 로드 실패는 예외로 던져지지 않고 LoadState.Error 로 온다(#5). 오류 처리는 observeLoadError 에서 한다
        lifecycleScope.launch {
            viewModel.getFlow(query).collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun observeLoadError() {
        adapter.addLoadStateListener { loadStates ->
            if (loadStates.refresh is LoadState.NotLoading) {
                retryLimiter.reset()  // 성공했을 때만 재시도 횟수를 되돌린다(#8)
            }
            val error = loadStates.refreshError() ?: return@addLoadStateListener
            if (error === shownError) return@addLoadStateListener  // 같은 실패로 대화상자를 여러 번 띄우지 않는다
            shownError = error
            // 원인 예외는 PagingSource 가 이미 ErrorReporter 로 보냈다. 여기서는 화면 맥락만 남긴다
            Log.w(TAG, "company list load failed: query=$query", error)
            showRetryDialog()
        }
    }

    private fun showRetryDialog() {
        AlertDialog.Builder(this)
            .setMessage("제작사 정보를 불러오는 데 문제가 생겼습니다.\n다시 시도하시겠습니까?")
            .setPositiveButton("재시도") { dialogInterface, i ->
                if (retryLimiter.tryAcquire()) {
                    adapter.retry()
                } else {
                    Toast.makeText(this, "잠시 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .setNegativeButton("아니요") { dialogInterface, i ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        val TAG = CompanyListActivity::class.java.simpleName
        private const val MAX_RETRY_NUM = 5
    }
}