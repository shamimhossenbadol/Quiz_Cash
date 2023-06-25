package com.enigma.quiz_cash.adapters

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enigma.quiz_cash.CategoryClicked
import com.enigma.quiz_cash.databinding.CategoryItemBinding
import com.enigma.quiz_cash.models.CategoryModel

class CategoryAdapter(
    private val context: Activity,
    private val categoryList: List<CategoryModel>,
    private val categoryClicked: CategoryClicked
) : RecyclerView.Adapter<CategoryAdapter.ViewModel>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewModel {
        val binding =
            CategoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewModel(binding)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onBindViewHolder(holder: ViewModel, position: Int) {
        val categoryItem = categoryList[position]
        holder.binding.title.text = categoryItem.title
        val colors = arrayOf(
            "#34c8d7",
            "#35a3d1",
            "#2bc386",
            "#4283f6",
            "#41bca2",
            "#42b9f5",
            "#31d5c2",
            "#34c8d7",
            "#35a3d1",
            "#2bc386",
            "#4283f6",
            "#41bca2",
            "#42b9f5",
            "#31d5c2",
            "#34c8d7",
            "#35a3d1",
            "#2bc386",
            "#4283f6",
            "#41bca2",
            "#42b9f5",
            "#31d5c2",
            "#34c8d7",
            "#35a3d1",
            "#2bc386",
            "#4283f6",
            "#41bca2",
            "#42b9f5",
            "#31d5c2",
            "#34c8d7",
            "#35a3d1",
            "#2bc386",
            "#4283f6",
            "#41bca2",
            "#42b9f5",
            "#31d5c2",
            "#34c8d7"
        )
        val colorStateList = ColorStateList.valueOf(Color.parseColor(colors[position]))
        holder.binding.items.setCardBackgroundColor(colorStateList)
        val imagePath = "file:///android_asset/icons/" + categoryItem.icon + ".png"
        Glide.with(context.applicationContext).load(imagePath).into(holder.binding.icon)
        holder.binding.items.setOnClickListener {
            categoryClicked.onClicked(categoryItem, colors[position])
        }
    }

    inner class ViewModel(val binding: CategoryItemBinding) : RecyclerView.ViewHolder(binding.root)

}
