package com.lusle.android.soon.adapter.decoration

import android.app.Activity
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.lusle.android.soon.R

class MovieItemDecoration @JvmOverloads constructor(
    activity: Activity,
    private val spanCount: Int = 2,
    spacing: Float = 12f,
    outerMargin: Float = 25f,
    numberOfNotMovieItem: Int = 1
) : ItemDecoration() {
    private val spacing: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        spacing, activity.resources.displayMetrics
    ).toInt()
    private val outerMargin: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        outerMargin, activity.resources.displayMetrics
    ).toInt()
    private val _numberOfNotMovieItem = numberOfNotMovieItem

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.adapter!!.getItemViewType(parent.getChildAdapterPosition(view)) != R.layout.item_movie_recyclerview) {
            return
        }
        val maxCount = parent.adapter!!.itemCount
        val position = parent.getChildAdapterPosition(view) - _numberOfNotMovieItem
        val column = position % spanCount
        val row = position / spanCount
        val firstRow = 0
        val lastRow = (maxCount - 1) / spanCount
        outRect.left = column * spacing / spanCount
        outRect.right = spacing - (column + 1) * spacing / spanCount
        outRect.top = spacing
        if (row == firstRow) {
            outRect.top = 0
        }
        if (row == lastRow) {
            outRect.bottom = outerMargin
        }
    }
}
