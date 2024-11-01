package com.grownapp.noteappgrown.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel

class ListCategoryAdapter(private val context: Context) : RecyclerView.Adapter<ListCategoryAdapter.viewHolder>() {
    inner class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var categoryName: TextView = itemView.findViewById(R.id.categoryName)
        var editBtn: ImageView = itemView.findViewById(R.id.editBtn)
        var deleteBtn: ImageView = itemView.findViewById(R.id.deleteBtn)
    }
    private lateinit var categoryViewModel: CategoryViewModel
    private val differCallback = object: DiffUtil.ItemCallback<Category>(){
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
    var differ = AsyncListDiffer(this, differCallback)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListCategoryAdapter.viewHolder {
        categoryViewModel = (context as MainActivity).categoryViewModel
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return viewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ListCategoryAdapter.viewHolder, position: Int) {
        holder.categoryName.text = differ.currentList[position].categoryName
        holder.editBtn.setOnClickListener {
            showEditCategoryDialog(context, position)
        }
        holder.deleteBtn.setOnClickListener {
            showDeleteCategoryDialog(context, position)
        }
    }

    private fun showDeleteCategoryDialog(context: Context, position: Int) {
        val alertDialog = AlertDialog.Builder(context).setMessage("Delete category '${differ.currentList[position].categoryName}'? Notes from the category won't be deleted")
            .setNegativeButton("Cancel"){dialog,_->dialog.dismiss()}
                .setPositiveButton("OK"){dialog,_->categoryViewModel.deleteCategory(differ.currentList[position])
                notifyDataSetChanged()
                dialog.dismiss()}.create()
        alertDialog.show()
    }

    private fun showEditCategoryDialog(context: Context, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_category,null)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        editText.setText(differ.currentList[position].categoryName)
        val alertDialog = AlertDialog.Builder(context).setView(dialogView).setNegativeButton("Cancel"){dialog,_->dialog.dismiss()}
            .setPositiveButton("OK"){dialog,_->val inputText = editText.text.toString()
            val newCategory = Category(differ.currentList[position].id,inputText)
            categoryViewModel.updateCategory(newCategory)
            notifyDataSetChanged()
            dialog.dismiss()}.create()
        alertDialog.show()
    }
}