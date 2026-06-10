package com.example.sortorder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var rowWidth = 0
        var rowHeight = 0
        var contentHeight = 0
        var contentWidth = 0

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val params = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + params.leftMargin + params.rightMargin
            val childHeight = child.measuredHeight + params.topMargin + params.bottomMargin

            if (rowWidth > 0 && rowWidth + childWidth > maxWidth) {
                contentWidth = max(contentWidth, rowWidth)
                contentHeight += rowHeight
                rowWidth = childWidth
                rowHeight = childHeight
            } else {
                rowWidth += childWidth
                rowHeight = max(rowHeight, childHeight)
            }
        }

        contentWidth = max(contentWidth, rowWidth)
        contentHeight += rowHeight
        setMeasuredDimension(
            resolveSize(contentWidth + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(contentHeight + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val availableWidth = right - left - paddingLeft - paddingRight
        val rows = mutableListOf<MutableList<View>>()
        var row = mutableListOf<View>()
        var rowWidth = 0

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue

            val params = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + params.leftMargin + params.rightMargin
            if (row.isNotEmpty() && rowWidth + childWidth > availableWidth) {
                rows.add(row)
                row = mutableListOf()
                rowWidth = 0
            }
            row.add(child)
            rowWidth += childWidth
        }
        if (row.isNotEmpty()) rows.add(row)

        var y = paddingTop
        rows.forEach { currentRow ->
            val totalWidth = currentRow.sumOf {
                val params = it.layoutParams as MarginLayoutParams
                it.measuredWidth + params.leftMargin + params.rightMargin
            }
            var x = paddingLeft + (availableWidth - totalWidth).coerceAtLeast(0) / 2
            var rowHeight = 0

            currentRow.forEach { child ->
                val params = child.layoutParams as MarginLayoutParams
                val childLeft = x + params.leftMargin
                val childTop = y + params.topMargin
                child.layout(
                    childLeft,
                    childTop,
                    childLeft + child.measuredWidth,
                    childTop + child.measuredHeight
                )
                x += child.measuredWidth + params.leftMargin + params.rightMargin
                rowHeight = max(
                    rowHeight,
                    child.measuredHeight + params.topMargin + params.bottomMargin
                )
            }
            y += rowHeight
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(params: LayoutParams): LayoutParams =
        MarginLayoutParams(params)

    override fun checkLayoutParams(params: LayoutParams): Boolean =
        params is MarginLayoutParams
}
