package com.grownapp.noteappgrown.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteViewModel

class ListNoteAdapter(
    private val context: Context,
    private val onItemClickListener: com.grownapp.noteappgrown.listeners.OnItemClickListener) : RecyclerView.Adapter<ListNoteAdapter.viewHolder>() {
    private val notes: MutableList<Note> = mutableListOf()
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var noteViewModel: NoteViewModel
    var isCreated = false

    inner class viewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var time: TextView = itemView.findViewById(R.id.tvTime)
        var title: TextView = itemView.findViewById(R.id.tvTitle)
        var category: TextView = itemView.findViewById(R.id.tvCategory)
        var noteLayout: LinearLayout = itemView.findViewById(R.id.noteLayout)
    }
    private val selectedItems = mutableSetOf<Note>()
    var isChoosing = false
    var isInTrash = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListNoteAdapter.viewHolder {
        categoryViewModel = (parent.context as MainActivity).categoryViewModel
        noteViewModel = (parent.context as MainActivity).noteViewModel
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return viewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    override fun onBindViewHolder(holder: ListNoteAdapter.viewHolder, position: Int) {
        val note = notes[position]

        holder.title.text = if(note.title.isEmpty()){
            context.getString(R.string.untitled)
        }else{
            note.title
        }

        holder.time.text = if (!isCreated){
            context.getString(R.string.last_edit) + note.time
        }else{
            context.getString(R.string.created) + note.created
        }

        val categoryList = categoryViewModel.getCategoryNameById(note.id)
        if(categoryList.size > 2){
            holder.category.text = "${categoryList[0]}, ${categoryList[1]}, (+${categoryList.size - 2}"
        }else{
            if(categoryList.isNotEmpty()){
                holder.category.text = categoryList.toString().substring(1, categoryList.toString().length - 1)
            }else{
                holder.category.text = null
            }
        }
        val backgroundColor = note.color

        val defaultDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = if (backgroundColor != null) {
                intArrayOf(Color.parseColor("#FAF6D1"), Color.parseColor(backgroundColor))
            } else {
                intArrayOf(Color.parseColor("#FFFEDB"), Color.parseColor("#FFFDAE"))
            }
            cornerRadius = context.resources.getDimension(com.intuit.sdp.R.dimen._5sdp)
            setStroke(
                context.resources.getDimension(com.intuit.sdp.R.dimen._1sdp).toInt(),
                ContextCompat.getColor(context, R.color.brownEarth)
            )
        }

        val selectedDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = if (backgroundColor != null) {
                intArrayOf(Color.parseColor(backgroundColor), Color.parseColor("#C0A28C"))
            } else {
                intArrayOf(Color.parseColor("#FAF6D1"), Color.parseColor("#C0A28C"))
            }
            cornerRadius = context.resources.getDimension(com.intuit.sdp.R.dimen._5sdp)
            setStroke(
                context.resources.getDimension(com.intuit.sdp.R.dimen._2sdp).toInt(),
                ContextCompat.getColor(context, R.color.brownEarth),
                context.resources.getDimension(com.intuit.sdp.R.dimen._20sdp),
                context.resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
            )
        }

        holder.noteLayout.background = if(selectedItems.contains(note)){
            selectedDrawable
        }else{
            defaultDrawable
        }

        holder.itemView.setOnLongClickListener {
            isChoosing = true
            toggleSelection(note)
            onItemClickListener.onNoteLongClick(note)
            notifyItemChanged(position)
            true
        }

        holder.itemView.isSelected = selectedItems.contains(note)
        holder.itemView.setOnClickListener {
            if(holder.itemView.isSelected){
                selectedItems.remove(note)
                notifyItemChanged(position)
            }else{
                if(isChoosing){
                    selectedItems.add(note)
                    notifyDataSetChanged()
                }else{
                    if(isInTrash){
                        showActionDialog(context, position)
                    }
                }
            }
            onItemClickListener.onNoteClick(note, holder.itemView.isSelected)
        }
    }

    fun updateNotes(newNotes: List<Note>){
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    fun removeSelectedItems(){
        notes.removeAll(selectedItems)
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItemsCount(): Int{
        return selectedItems.size
    }

    fun getSelectedItems(): Set<Note>{
        return selectedItems
    }

    fun getNotes(): List<Note>{
        return notes
    }

    fun clearSelection(){
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun selectAllItems(){
        if(selectedItems.isEmpty()){
            selectedItems.addAll(notes)
        }else{
            if(selectedItems.size < notes.size){
                selectedItems.clear()
                selectedItems.addAll(notes)
            }else{
                selectedItems.clear()
            }
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(note: Note) {
        if(selectedItems.contains(note)){
            selectedItems.remove(note)
        }else{
            selectedItems.add(note)
        }
        notifyDataSetChanged()
    }

    private fun showActionDialog(context: Context, position: Int) {
        val note = notes[position]
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_action_in_trash, null)
        val title = dialogView.findViewById<TextView>(R.id.trashTitle)
        val content = dialogView.findViewById<TextView>(R.id.trashContent)
        val actionRadioGroup = dialogView.findViewById<RadioGroup>(R.id.actionRadioGroup)
        title.text = note.title
        content.text = note.content
        val alertDialog = AlertDialog.Builder(context).setView(dialogView)
            .setNegativeButton("CANCEL"){dialog, _ -> dialog.dismiss()}
            .setPositiveButton("OK"){dialog, _ ->
                val selectedAction = when (actionRadioGroup.checkedRadioButtonId){
                    R.id.rbUndelete -> "Undelete"
                    R.id.rbDelete -> "Delete"
                    else -> null
                }
                handleNoteAction(note, selectedAction)
                dialog.dismiss()
            }.create()
        alertDialog.show()
    }

    private fun handleNoteAction(note: Note, selectedAction: String?) {
        when(selectedAction){
            "Undelete" -> {
                var noteIds: List<Int> = listOf(note.id)
                noteViewModel.restoreFromTrash(noteIds)
            }
            "Delete" -> {
                val builder = AlertDialog.Builder(context).setMessage("The note will be deleted permanently! Are you sure that you want to delete the '${note.title}' note?")
                    .setNegativeButton("No"){dialog, _-> dialog.dismiss()}
                        .setNegativeButton("Yes"){dialog, _-> noteViewModel.deleteNote(note)}
                builder.create().show()
            }
        }
    }
}