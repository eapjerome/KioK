package com.eap.kiok

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeToDeleteCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = getDrawable(context, R.drawable.ic_delete_white_24dp)!!
    private val intrinsicWidth = deleteIcon.intrinsicWidth
    private val intrinsicHeight = deleteIcon.intrinsicHeight
    private val background = ColorDrawable()
    private val backgroundColor = Color.RED

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        val itemView = viewHolder.itemView
        val deleteIconMargin = (itemView.height - intrinsicHeight) / 2
        val deleteIconTop = itemView.top + deleteIconMargin
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        background.color = backgroundColor

        when {
            dX < 0 -> {
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                deleteIcon.setBounds(
                    itemView.right - deleteIconMargin - intrinsicWidth,
                    deleteIconTop,
                    itemView.right - deleteIconMargin,
                    deleteIconBottom
                )
            }
            dX > 0 -> {
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                deleteIcon.setBounds(
                    itemView.left + deleteIconMargin,
                    deleteIconTop,
                    itemView.left + deleteIconMargin + intrinsicWidth,
                    deleteIconBottom
                )
            }
            else -> background.setBounds(0, 0, 0, 0)
        }
        background.draw(c)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}